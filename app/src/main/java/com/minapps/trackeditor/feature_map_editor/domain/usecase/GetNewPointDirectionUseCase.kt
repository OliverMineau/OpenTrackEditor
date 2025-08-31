package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.type.InsertPosition
import jakarta.inject.Inject

class GetNewPointDirectionUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    /**
     * Update marker for adding points
     * IF start of track clicked : Position = FRONT (add to front of track on future clicks)
     * IF end of track clicked : Position = BACK (add to back of track on future clicks)
     * IF middle of track clicked : Position = MIDDLE (add waypoint next to clicked point)
     *
     * @param trackId
     * @param marker
     * @return index of point if point has to be inserted in middle of track
     * if not : null
     */
    suspend operator fun invoke(
        trackId: Int,
        marker: Double?
    ): Pair<Waypoint?, InsertPosition> {

        // Get ends of track
        val first = repository.getTrackFirstWaypointId(trackId)
        val last = repository.getTrackLastWaypointId(trackId)
        // Set default direction to BACK
        var direction = InsertPosition.BACK

        var newPoint: Waypoint? = null

        // If user didn't click on a waypoint
        if (marker != null) {

            // If user clicked on start of track
            if (marker == first) {
                direction = InsertPosition.FRONT
            }
            // If user clicked on end of track
            else if (marker == last) {
                direction = InsertPosition.BACK
            }
            // If user clicked on other waypoint on the track
            else {

                // Get index of point
                val index = repository.getWaypointIndex(trackId, marker)
                // If waypoint exists on track
                if (index != null) {
                    val startPoint = repository.getWaypoint(trackId, index)
                    val endPoint = repository.getWaypoint(trackId, index + 1)

                    // If retrieved start and end points
                    if (startPoint != null && endPoint != null) {
                        direction = InsertPosition.MIDDLE
                        val latlong = getWaypointCoords(startPoint, endPoint)
                        val newInnerId = (startPoint.id + endPoint.id) / 2.0

                        newPoint = Waypoint(newInnerId, latlong.first, latlong.second, null, null, trackId)
                    }
                }
            }
        }

        // Return new point if has on and direction
        return newPoint to direction
    }


    /**
     * Calculate approximative new waypoint coordinates
     *
     * @param start
     * @param end
     * @return
     */
    private fun getWaypointCoords(start: Waypoint, end: Waypoint): Pair<Double, Double> {
        val midLat = (start.lat + end.lat) / 2.0
        val midLng = (start.lng + end.lng) / 2.0

        return Pair(midLat, midLng)
    }

}