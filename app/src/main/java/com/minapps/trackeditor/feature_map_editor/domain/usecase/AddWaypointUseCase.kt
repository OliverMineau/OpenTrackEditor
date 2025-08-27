package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
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

    var direction: Double? = 0.0

    /**
     * Creates and adds a new waypoint to the given track.
     *
     * @param lat The latitude coordinate of the waypoint.
     * @param lng The longitude coordinate of the waypoint.
     * @param idx The index of the waypoint within the track.
     * @param trackId The ID of the track to which this waypoint will be added.
     */
    suspend operator fun invoke(lat: Double, lng: Double, id: Double, trackId: Int) {

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var newId = id

        // Add to Front
        if(direction == -1.0){
            newId = first + direction!!
        }
        // Add to Back
        else if(direction == 1.0){
            newId = last + direction!!
        }
        // No selection, add to Back
        else if(direction == 0.0){
            newId = last + 1.0
        }

        Log.d("new point", "newId : $newId : direction:$direction, firsdt:$first, last:$last")

        val waypoint = Waypoint(
            id = newId,
            lat = lat,
            lng = lng,
            elv = null,
            time = "",
            trackId = trackId,
        )
        //repository.addWaypoint(waypoint)

        repository.addWaypoint(waypoint, )
    }

    /**
     * TODO
     *
     * @param trackId
     * @param marker
     * @return index of point
     */
    suspend fun updateMarker(trackId: Int, marker: Double?) {
        Log.d("new point", "Marker : $marker")

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var dir = 0.0

        if(marker != null){

            if(marker == first){
                dir = -1.0
                Log.d("same","same :$marker, f$first")
            }

            if(marker == last){
                dir = 1.0
                Log.d("same","same :$marker, l$last")
            }
        }
        this.direction = dir
    }

    suspend fun getNextId(trackId: Int): Double{
        Log.d("bug","trackId: $trackId")
        val first = repository.getTrackFirstWaypointIndex(trackId)
        var newId = repository.getTrackLastWaypointIndex(trackId)

        if(direction == -1.0){
            newId = first + direction!!
        }
        else if(direction == 1.0){
            newId = newId + direction!!
        }

        return newId
    }


}