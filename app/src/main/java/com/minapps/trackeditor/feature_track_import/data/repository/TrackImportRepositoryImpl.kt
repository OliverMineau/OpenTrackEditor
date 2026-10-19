package com.minapps.trackeditor.feature_track_import.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.local.TrackEntity
import com.minapps.trackeditor.data.mapper.toEntity
import com.minapps.trackeditor.feature_track_import.data.factory.ImporterFactory
import com.minapps.trackeditor.feature_track_import.data.parser.ParsedData
import com.minapps.trackeditor.feature_track_import.domain.model.ImportFormat
import com.minapps.trackeditor.core.domain.repository.TrackImportRepository
import com.minapps.trackeditor.feature_track_import.domain.model.DataStreamProgress
import com.minapps.trackeditor.feature_track_import.domain.model.ImportDiagnostic
import com.minapps.trackeditor.feature_track_import.domain.model.ImportDiagnosticKind
import com.minapps.trackeditor.feature_track_import.domain.model.ImportDiagnosticScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import javax.inject.Inject

/**
 * Implementation of TrackImportRepository that handles
 * importing tracks from different file formats.
 *
 * @param context Application context, used for content resolver access.
 */

class TrackImportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TrackDao,
    private val importerFactory: ImporterFactory,
) : TrackImportRepository {

    private val chunkSize = 50000
    private val maxDiagnostics = 100

    private class DownstreamProgressException(cause: Throwable) : RuntimeException(cause)

    override suspend fun keepRecoveredTracks(affectedTrackIds: List<Int>) {
        dao.keepRecoveredTracks(affectedTrackIds)
    }

    override suspend fun discardImportedTracks(trackIds: List<Int>) {
        dao.discardImportedTracks(trackIds)
    }

    /**
     * Import a track from the given file Uri.
     * Detects the file format and uses appropriate parser.
     *
     * @param fileUri The Uri of the file to import.
     * @return Flow DataStreamProgress
     */
    override suspend fun importTrack(fileUri: Uri): Flow<DataStreamProgress> = flow {
        val trackIds = mutableListOf<Int>()
        val fileName = getFileName(context, fileUri)
        val fileSize = getFileSize(context, fileUri)
        var progress = 0

        val format = detectFileFormat(context, fileUri)

        // Format error
        if (format == null) {
            emit(DataStreamProgress.Error("Couldn't read / determine $fileName's format."))
            return@flow
        }


        val parser = importerFactory.getImporter(format)

        var trackId: Int? = null

        val diagnostics = mutableListOf<ImportDiagnostic>()
        val affectedTrackIds = linkedSetOf<Int>()
        var diagnosticCount = 0
        var structuralErrorReported = false
        var finalResult: DataStreamProgress? = null

        try {
                    parser.parse(context, fileUri, fileSize, chunkSize)
                        .catch { e ->
                            if (e !is XmlPullParserException) throw e

                            // Active-track XML errors were already diagnosed by the parser.
                            // Add a repository diagnostic only when parsing failed outside a track.
                            if (!structuralErrorReported) diagnosticCount++

                            if (!structuralErrorReported && diagnostics.size < maxDiagnostics) {
                                diagnostics.add(
                                    ImportDiagnostic(
                                        trackId = trackId,
                                        trackName = null,
                                        message = "Malformed XML: ${e.message ?: "Unknown XML parsing error"}",
                                        line = e.lineNumber.takeIf { it > 0 },
                                        column = e.columnNumber.takeIf { it > 0 },
                                        scope = ImportDiagnosticScope.DOCUMENT,
                                        kind = ImportDiagnosticKind.STRUCTURAL
                                    )
                                )
                            }

                            Log.w("TrackImportRepository", "XML parsing interrupted import: $fileName", e)
                        }
                        .collect { parsedData ->
            when (parsedData) {

                // Send track metadata
                is ParsedData.TrackMetadata -> {
                    val id = dao.insertTrack((parsedData.metadata).toEntity()).toInt()
                    trackId = id
                    trackIds.add(id)
                }

                // Send waypoints
                is ParsedData.Waypoints -> {

                    // If not track
                    if (trackId == null) {
                        trackId = dao.insertTrack(
                            TrackEntity(
                                name = "New track",
                                description = "",
                                createdAt = 0
                            )
                        ).toInt()
                        trackIds.add(trackId)
                    }

                    dao.insertWaypoints((parsedData.waypoints).map {
                        it.trackId = trackId
                        it.toEntity()
                    })


                }

                is ParsedData.Progress -> {
                    if (parsedData.progress != progress) {
                        progress = parsedData.progress

                        try {
                            emit(DataStreamProgress.Progress(parsedData.progress))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            throw DownstreamProgressException(e)
                        }
                    }
                }

                is ParsedData.Diagnostic -> {
                    if (parsedData.diagnostic.kind == ImportDiagnosticKind.STRUCTURAL) {
                        structuralErrorReported = true
                    }
                    diagnosticCount++

                    // Document errors must not mark the last completed track incomplete.
                    val trackLevel = parsedData.diagnostic.scope == ImportDiagnosticScope.TRACK
                    val diagnostic = parsedData.diagnostic.copy(trackId = if (trackLevel) trackId else null)

                    if (trackLevel && trackId != null) {
                        affectedTrackIds.add(trackId)
                    }

                    if (diagnostics.size < maxDiagnostics) {
                        diagnostics.add(diagnostic)
                    }
                }
            }
        }

                Log.d("debug", "Finished")

                finalResult = if (diagnosticCount > 0 && trackIds.isEmpty()) {
                    val message = diagnostics.firstOrNull()?.message
                        ?: "Import failed before any tracks could be recovered"

                    DataStreamProgress.Error(message)
                } else if (diagnosticCount > 0) {
                    val waypointCount = dao.countWaypointsForTracks(trackIds)

                    DataStreamProgress.PartialSuccess(
                        trackIds = trackIds.toList(),
                        affectedTrackIds = affectedTrackIds.toList(),
                        waypointCount = waypointCount,
                        diagnostics = diagnostics.toList(),
                        omittedDiagnosticCount = diagnosticCount - diagnostics.size
                    )
                } else {
                    DataStreamProgress.Completed(trackIds.toMutableList())
                }

            } catch (e: CancellationException) {
                // Cancellation must not leave tracks from an unfinished import behind.
                withContext(NonCancellable) {
                    if (trackIds.isNotEmpty()) {
                        try {
                            dao.discardImportedTracks(trackIds)
                        } catch (cleanupError: Exception) {
                            Log.e("TrackImportRepository", "Cleanup failed after import cancellation", cleanupError)
                            e.addSuppressed(cleanupError)
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                // Unexpected failures abort the import; remove only tracks created here.
                withContext(NonCancellable) {
                    if (trackIds.isNotEmpty()) {
                        try {
                            dao.discardImportedTracks(trackIds)
                        } catch (cleanupError: Exception) {
                            Log.e("TrackImportRepository", "Cleanup failed after import error", cleanupError)
                            e.addSuppressed(cleanupError)
                        }
                    }
                }
            if (e is DownstreamProgressException) {
                // The collector failed. Cleanup has already run; do not emit again.
                throw (e.cause ?: e)
            }

            Log.e("TrackImportRepository", "Import failed: $fileName", e)
            emit(DataStreamProgress.Error(e.message ?: "Unknown Error"))
            return@flow
            }

        finalResult?.let { emit(it) }
    }


    /**
     * Helper method to retrieve the display name of a file
     * given its Uri by querying the content resolver.
     *
     * @param context Application context.
     * @param uri The Uri of the file.
     * @return The display name or null if not available.
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    /**
     * Helper method to retrieve the size of a file
     *
     * @param context
     * @param uri
     * @return
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                return cursor.getLong(sizeIndex)
            }
        }
        return -1L // Unknown size
    }


    /**
     * Detect the file format by reading the first line of the file.
     * Currently supports detecting GPX and KML formats.
     *
     * @param context Application context.
     * @param uri The Uri of the file.
     * @return The detected format as string or null if unknown.
     */
    fun detectFileFormat(context: Context, uri: Uri): ImportFormat? {
        val fileName = getFileName(context, uri)

        if (fileName?.endsWith(".gpx", ignoreCase = true) == true) return ImportFormat.GPX
        if (fileName?.endsWith(".kml", ignoreCase = true) == true) return ImportFormat.KML

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = inputStream.bufferedReader()
            val firstLine = reader.readLine()
            if (firstLine != null) {
                return when {
                    firstLine.contains("<gpx", ignoreCase = true) -> ImportFormat.GPX
                    firstLine.contains("<kml", ignoreCase = true) -> ImportFormat.KML
                    else -> null
                }
            }
        }
        return null
    }


}
