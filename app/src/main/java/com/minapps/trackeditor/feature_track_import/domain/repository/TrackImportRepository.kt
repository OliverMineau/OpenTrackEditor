package com.minapps.trackeditor.feature_track_import.domain.repository

import android.net.Uri
import com.minapps.trackeditor.core.domain.model.Track

/**
 * Repository interface responsible for importing track data from external sources.
 */
interface TrackImportRepository {

    /**
     * Imports a track from the given file URI.
     *
     * @param fileUri The URI pointing to the track file to import.
     * @return An [ImportedTrack] object if import is successful, or null if it fails.
     */
    suspend fun importTrack(fileUri: Uri): Boolean
}