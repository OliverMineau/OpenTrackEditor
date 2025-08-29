package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Use case responsible for creating a track
 *
 * @property trackRepository
 */
class DeleteWaypointUseCase @Inject constructor(
    private val trackRepository: EditTrackRepository
) {

    /**
     * Remove waypoint from database
     *
     * @param trackId
     * @return
     */
    suspend operator fun invoke(trackId: Int, waypoint: Waypoint){
        //trackRepository.removeTrack(trackId)
    }

    suspend operator fun invoke(trackId: Int, waypoints: List<Waypoint>){
        //trackRepository.removeTrack(trackId)
    }

    suspend operator fun invoke(trackId: Int, startId: Double, endId: Double){
        val min = min(startId, endId)
        val max = max(startId, endId)
        trackRepository.deleteSegment(trackId, min, max)
    }

    suspend operator fun invoke(trackId: Int, id: Double){
        trackRepository.deleteWaypoint(trackId, id)
    }
}
