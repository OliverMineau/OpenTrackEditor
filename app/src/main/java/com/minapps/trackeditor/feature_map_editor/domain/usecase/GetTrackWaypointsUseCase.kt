package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import jakarta.inject.Inject

/**
 * Use case responsible for getting all waypoints from a given track
 *
 * @property repository
 */
class GetTrackWaypointsUseCase @Inject constructor(
    private val repository: EditTrackRepositoryItf
) {

    /**
     * Retrieves all waypoints of track from database
     *
     * @param trackId
     * @return List of waypoints associated to given trackId
     */
    suspend operator fun invoke(trackId: Int): List<Waypoint> {
        return repository.getTrackWaypoints(trackId)
    }
}
