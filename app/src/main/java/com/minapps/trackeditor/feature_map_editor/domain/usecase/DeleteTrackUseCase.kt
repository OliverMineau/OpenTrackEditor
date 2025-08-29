package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

/**
 * Use case responsible for creating a track
 *
 * @property trackRepository
 */
class DeleteTrackUseCase @Inject constructor(
    private val trackRepository: EditTrackRepository
) {

    /**
     * Remove track from database
     *
     * @param trackId
     * @return
     */
    suspend operator fun invoke(trackId: Int){
        trackRepository.removeTrack(trackId)
    }

    suspend operator fun invoke(trackIds: List<Int>){
        trackIds.forEach { trackId ->
            trackRepository.removeTrack(trackId)
        }
    }
}
