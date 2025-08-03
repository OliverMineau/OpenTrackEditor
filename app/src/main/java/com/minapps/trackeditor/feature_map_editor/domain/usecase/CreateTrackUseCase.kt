package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import com.minapps.trackeditor.data.local.TrackEntity
import jakarta.inject.Inject

/**
 * Use case responsible for creating a track
 *
 * @property trackRepository
 */
class CreateTrackUseCase @Inject constructor(
    private val trackRepository: EditTrackRepositoryItf
) {

    /**
     * Create and insert track into database
     *
     * @param name Name of the track
     * @return Int that represents track ID
     */
    suspend operator fun invoke(name: String): Int {
        return trackRepository.insertTrack(
            TrackEntity(
                name = name,
                description = null,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()
    }
}
