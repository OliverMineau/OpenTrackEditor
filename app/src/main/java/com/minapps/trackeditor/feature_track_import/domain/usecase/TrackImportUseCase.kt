package com.minapps.trackeditor.feature_track_import.domain.usecase

import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.feature_track_import.domain.model.ImportedTrack
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
    suspend operator fun invoke(fileUri: Uri): ImportedTrack?{
        val importedTrack = trackImportRepository.importTrack(fileUri)
        Log.d("debug", "Track imported ${if (importedTrack != null) "successfully" else "unsuccessfully"} (${importedTrack?.waypoints?.size} waypoints) e.g. ${importedTrack?.waypoints?.get(0)}}")
        return importedTrack
    }
}
