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

    var direction: Double? = 1.0

    /**
     * Creates and adds a new waypoint to the given track.
     *
     * @param lat The latitude coordinate of the waypoint.
     * @param lng The longitude coordinate of the waypoint.
     * @param idx The index of the waypoint within the track.
     * @param trackId The ID of the track to which this waypoint will be added.
     */
    suspend operator fun invoke(lat: Double, lng: Double, id: Double, trackId: Int, forceDir:Double?=null) {

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var newId = id

        //TODO ADDED THIS BUT on large tracks forces Outline render for som reason
        if(forceDir != null){
            direction = forceDir
        }

        // Add to Front
        if(direction == -1.0){
            newId = first + direction!!
        }
        // Add to Back
        else if(direction == 1.0){
            newId = last + direction!!
        }
        // No selection, add to Back
        /*else if(direction == 0.0){
            newId = last + 1.0
        }*/

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

        repository.addWaypoint(waypoint)
    }

    /**
     * TODO
     *
     * @param trackId
     * @param marker
     * @return index of point if point has to be inserted in middle of track
     * if not : null
     */
    suspend fun updateMarker(trackId: Int, marker: Double?): Int? {
        Log.d("new point", "Marker : $marker")

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var dir = 1.0

        var index:Int? = null

        if(marker != null){

            if(marker == first){
                dir = -1.0
            }
            else if(marker == last){
                dir = 1.0
            }else{
                index = repository.getWaypointIndex(trackId, marker)
                if(index != null){
                    val startPoint = repository.getWaypoint(trackId, index)
                    val endPoint = repository.getWaypoint(trackId, index+1)

                    if(startPoint != null && endPoint != null){
                        dir = 0.0
                        this.direction = dir
                        val latlong = getWaypointCoords(startPoint, endPoint)
                        val newInnerId = (startPoint.id + endPoint.id) / 2.0
                        Log.d("ADD","nenewInnerId :$newInnerId")
                        this.invoke(latlong.first, latlong.second, newInnerId , trackId)
                    }
                }
            }
        }
        this.direction = dir

        return index
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

    private fun getWaypointCoords(start: Waypoint, end: Waypoint): Pair<Double,Double>{
        val midLat = (start.lat + end.lat) / 2.0
        val midLng = (start.lng + end.lng) / 2.0

        return Pair(midLat, midLng)
    }


}