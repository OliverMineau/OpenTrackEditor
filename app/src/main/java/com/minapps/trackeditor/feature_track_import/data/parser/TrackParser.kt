package com.minapps.trackeditor.feature_track_import.data.parser

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Interface for parsers that handle importing tracks from files.
 * Different implementations should handle different file formats (e.g., GPX, KML).
 */
interface TrackParser {

    /**
     * Parse the given file Uri and extract an ImportedTrack.
     *
     * @param context Application context for accessing content resolver or resources.
     * @param fileUri Uri of the file to parse.
     * @return ImportedTrack if parsing is successful, or null if parsing fails.
     */
    suspend fun parse(context: Context, fileUri: Uri, fileSize: Long, chunkSize: Int): Flow<ParsedData>

    /**
     * Check whether this parser can handle the given file Uri.
     * This is useful to select the correct parser dynamically.
     *
     * @param fileUri Uri of the file to check.
     * @return true if this parser can handle the file, false otherwise.
     */
    fun canHandle(fileUri: Uri): Boolean
}