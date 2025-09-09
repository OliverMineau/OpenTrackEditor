package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.type.InsertPosition
import jakarta.inject.Inject

/**
 * Use case responsible for adding a new waypoint to a specific track.
 *
 * This encapsulates the logic required to create a [Waypoint] and delegate
 * the persistence to the [EditTrackRepository].
 *
 * @property repository Repository used to add the waypoint to the data source.
 */
class AddWaypointUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {


    /**
     * Creates and adds a new waypoint to the given track.
     *
     * @param lat The latitude coordinate of the waypoint.
     * @param lng The longitude coordinate of the waypoint.
     * @param idx The index of the waypoint within the track.
     * @param trackId The ID of the track to which this waypoint will be added.
     * @param updateUi /!\ using updateUi will reload entire track, it may/will reload track using taking into account the viewport (DouglasPucker)
     */
    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        id: Double?,
        trackId: Int,
        position: InsertPosition?,
        updateUi: Boolean = false
    ) {

        val first = repository.getTrackFirstWaypointId(trackId)
        val last = repository.getTrackLastWaypointId(trackId)
        var newId = id

        // If new point (no known id) (place to front or back)
        if (newId == null) {
            newId = getNextId(trackId, null)
        }

        Log.d("new point", "newId : $newId : direction:$position, firsdt:$first, last:$last")

        val waypoint = Waypoint(
            id = newId,
            lat = lat,
            lng = lng,
            elv = null,
            time = "",
            trackId = trackId,
        )

        // TODO if updateUI and part of track is loaded, track will load as outline
        repository.addWaypoint(waypoint, updateUi)
    }


    /**
     * Get the index of the next waypoint
     * /!\ Only for track edges (no middle points) /!\
     *
     * @param trackId
     * @return If no point selected give back of track id
     */
    suspend fun getNextId(trackId: Int, position: InsertPosition?): Double {

        Log.d("bug", "trackId: $trackId")
        val first = repository.getTrackFirstWaypointId(trackId)
        val last = repository.getTrackLastWaypointId(trackId)
        var newId = 0.0

        // If first point of track
        if (first == null || last == null) {
            return newId
        }

        // Place to front of track
        if (position == InsertPosition.FRONT) {
            newId = first - 1.0
        }
        // Place to back of track
        else if (position == InsertPosition.BACK) {
            newId = last + 1.0
        }

        // Else is completely new point (id 0.0)
        Log.d("Pos", "pos: $position")

        return newId
    }

}