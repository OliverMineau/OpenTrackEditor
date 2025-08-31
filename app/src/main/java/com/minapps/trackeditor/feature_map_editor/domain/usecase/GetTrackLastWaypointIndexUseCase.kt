package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

/**
 * Use case responsible for getting all waypoints from a given track
 *
 * @property repository
 */
class GetTrackLastWaypointIndexUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    /**
     * Retrieves all waypoints of track from database
     *
     * @param trackId
     * @return List of waypoints associated to given trackId
     */
    suspend operator fun invoke(trackId: Int): Double {
        return repository.getTrackLastWaypointId(trackId) ?: 0.0
    }
}
