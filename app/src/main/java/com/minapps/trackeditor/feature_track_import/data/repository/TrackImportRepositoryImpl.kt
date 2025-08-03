package com.minapps.trackeditor.feature_track_import.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.minapps.trackeditor.feature_track_import.data.parser.GpxParser
import com.minapps.trackeditor.feature_track_import.data.parser.TrackParser
import com.minapps.trackeditor.feature_track_import.domain.model.ImportedTrack
import com.minapps.trackeditor.feature_track_import.domain.repository.TrackImportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Implementation of TrackImportRepository that handles
 * importing tracks from different file formats.
 *
 * @param context Application context, used for content resolver access.
 */
class TrackImportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TrackImportRepository {

    /**
     * Import a track from the given file Uri.
     * Detects the file format and uses appropriate parser.
     *
     * @param fileUri The Uri of the file to import.
     * @return ImportedTrack instance or null if import fails.
     * @throws IllegalArgumentException if the file format is unsupported.
     */
    override suspend fun importTrack(fileUri: Uri): ImportedTrack? {
        val fileName = getFileName(context, fileUri)
        val format = when {
            fileName?.endsWith(".gpx", ignoreCase = true) == true -> "gpx"
            else -> detectFileFormat(context, fileUri) ?: throw IllegalArgumentException("Unsupported file type: $fileUri")
        }

        val parser = when(format) {
            "gpx" -> GpxParser()
            // Add other parsers in future
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }

        return parser.parse(context, fileUri)
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
     * Detect the file format by reading the first line of the file.
     * Currently supports detecting GPX and KML formats.
     *
     * @param context Application context.
     * @param uri The Uri of the file.
     * @return The detected format as string or null if unknown.
     */
    fun detectFileFormat(context: Context, uri: Uri): String? {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = inputStream.bufferedReader()
            val firstLine = reader.readLine()
            if (firstLine != null) {
                return when {
                    firstLine.contains("<gpx", ignoreCase = true) -> "gpx"
                    firstLine.contains("<kml", ignoreCase = true) -> "kml"
                    else -> null
                }
            }
        }
        return null
    }

}
