package com.minapps.trackeditor.feature_track_import.domain.usecase

import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import com.minapps.trackeditor.feature_track_import.domain.repository.TrackImportRepository
import jakarta.inject.Inject

/**
 * Use case responsible for importing a track from a given file Uri.
 */
class TrackImportUseCase @Inject constructor(
    private val trackImportRepository: TrackImportRepository,
) {

    /**
     * Invokes the use case to import a track asynchronously.
     *
     * @param fileUri The Uri pointing to the track file to import.
     */
    suspend operator fun invoke(fileUri: Uri): Track?{
        val importedTrack = trackImportRepository.importTrack(fileUri)
        Log.d("debug", "Track imported ${if (importedTrack != null) "successfully" else "unsuccessfully"} id:${importedTrack?.id}, (${importedTrack?.waypoints?.size} waypoints) e.g. ${importedTrack?.waypoints?.get(0)}}")

        if(importedTrack == null){
            Log.d("debug", "Track import error")
        }

        return importedTrack
    }
}
