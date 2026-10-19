package com.minapps.trackeditor.core.domain.repository

import android.net.Uri
import com.minapps.trackeditor.feature_track_import.domain.model.DataStreamProgress
import kotlinx.coroutines.flow.Flow

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
    suspend fun importTrack(fileUri: Uri): Flow<DataStreamProgress>

    /** Mark affected tracks as incomplete after the user chooses Keep. */
    suspend fun keepRecoveredTracks(affectedTrackIds: List<Int>)

    /** Delete all tracks created by an import after the user chooses Discard. */
    suspend fun discardImportedTracks(trackIds: List<Int>)
}