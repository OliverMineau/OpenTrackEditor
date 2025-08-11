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
import com.minapps.trackeditor.feature_track_import.domain.repository.TrackImportRepository
import com.minapps.trackeditor.feature_track_import.domain.usecase.DataStreamProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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

    /**
     * Import a track from the given file Uri.
     * Detects the file format and uses appropriate parser.
     *
     * @param fileUri The Uri of the file to import.
     * @return ImportedTrack instance or null if import fails.
     * @throws IllegalArgumentException if the file format is unsupported.
     */
    override suspend fun importTrack(fileUri: Uri): Flow<DataStreamProgress> = flow {
        val fileName = getFileName(context, fileUri)
        val fileSize = getFileSize(context, fileUri)
        var progress = 0

        val format = detectFileFormat(context, fileUri)

        if (format == null) {
            emit(DataStreamProgress.Error("Couldn't read / determine $fileName's format."))
            return@flow
        }


        val parser = importerFactory.getImporter(format)

        var trackId: Int? = null
        parser.parse(context, fileUri, fileSize, chunkSize).collect { parsedData ->
            when (parsedData) {

                // Send track metadata
                is ParsedData.TrackMetadata -> {
                    trackId = dao.insertTrack((parsedData.metadata).toEntity()).toInt()
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
                    }

                    dao.insertWaypoints((parsedData.waypoints).map {
                        it.trackId = trackId
                        it.toEntity()
                    })


                }

                is ParsedData.Progress -> {
                    if (parsedData.progress != progress) {
                        progress = parsedData.progress
                        emit(DataStreamProgress.Progress(parsedData.progress))
                    }
                }
            }
        }

        Log.d("debug", "Finished")
        val tid = trackId ?: -1
        emit(DataStreamProgress.Completed(tid))

    }.catch { e ->
        emit(DataStreamProgress.Error(e.message ?: "Unknown Error"))
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
