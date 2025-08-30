package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.graphics.Path
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
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
        trackId: Int, marker: Double?
    ): Pair<Waypoint?, AddWaypointUseCase.InsertPosition> {
        Log.d("new point", "Marker : $marker")

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var direction = AddWaypointUseCase.InsertPosition.BACK

        var newPoint: Waypoint? = null

        if (marker != null) {

            if (marker == first) {
                direction = AddWaypointUseCase.InsertPosition.FRONT
            } else if (marker == last) {
                direction = AddWaypointUseCase.InsertPosition.BACK
            } else {
                val index = repository.getWaypointIndex(trackId, marker)
                if (index != null) {
                    val startPoint = repository.getWaypoint(trackId, index)
                    val endPoint = repository.getWaypoint(trackId, index + 1)

                    if (startPoint != null && endPoint != null) {
                        direction = AddWaypointUseCase.InsertPosition.MIDDLE
                        val latlong = getWaypointCoords(startPoint, endPoint)
                        val newInnerId = (startPoint.id + endPoint.id) / 2.0
                        Log.d("ADD", "nenewInnerId :$newInnerId")

                        newPoint = Waypoint(newInnerId, latlong.first, latlong.second, null, null, trackId)
                    }
                }
            }
        }
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