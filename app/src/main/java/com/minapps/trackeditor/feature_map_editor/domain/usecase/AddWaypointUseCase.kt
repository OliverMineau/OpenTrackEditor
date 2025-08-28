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

    enum class InsertPosition { FRONT, BACK, MIDDLE }
    //var direction: Double? = 1.0
    var position: InsertPosition = InsertPosition.BACK

    /**
     * Creates and adds a new waypoint to the given track.
     *
     * @param lat The latitude coordinate of the waypoint.
     * @param lng The longitude coordinate of the waypoint.
     * @param idx The index of the waypoint within the track.
     * @param trackId The ID of the track to which this waypoint will be added.
     * @param updateUi /!\ using updateUi will reload entire track, it may/will reload track using taking into account the viewport (DouglasPucker)
     */
    suspend operator fun invoke(lat: Double, lng: Double, id: Double?, trackId: Int, updateUi: Boolean = false) {

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var newId = id

        // If new point (no known id) (place to front or back)
        if(newId == null){
            newId = getNextId(trackId)
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
    suspend fun updateMarker(trackId: Int, marker: Double?): Waypoint? {
        Log.d("new point", "Marker : $marker")

        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var dir = InsertPosition.BACK

        var newPoint: Waypoint? = null

        if(marker != null){

            if(marker == first){
                dir = InsertPosition.FRONT
            }
            else if(marker == last){
                dir = InsertPosition.BACK
            }else{
                val index = repository.getWaypointIndex(trackId, marker)
                if(index != null){
                    val startPoint = repository.getWaypoint(trackId, index)
                    val endPoint = repository.getWaypoint(trackId, index+1)

                    if(startPoint != null && endPoint != null){
                        dir = InsertPosition.MIDDLE
                        this.position = InsertPosition.MIDDLE
                        val latlong = getWaypointCoords(startPoint, endPoint)
                        val newInnerId = (startPoint.id + endPoint.id) / 2.0
                        Log.d("ADD","nenewInnerId :$newInnerId")

                        newPoint = Waypoint(newInnerId, latlong.first, latlong.second, null, null, trackId)

                        this.invoke(latlong.first, latlong.second, newInnerId , trackId, false)
                    }
                }
            }
        }
        this.position = dir
        return newPoint
    }

    /**
     * Get the index of the next waypoint
     * /!\ Only for track edges (no middle points) /!\
     *
     * @param trackId
     * @return If no point selected give back of track id
     */
    suspend fun getNextId(trackId: Int): Double{

        Log.d("bug","trackId: $trackId")
        val first = repository.getTrackFirstWaypointIndex(trackId)
        val last = repository.getTrackLastWaypointIndex(trackId)
        var newId = 0.0

        // If first point of track
        if(first == null || last == null){
            return newId
        }

        // Place to front of track
        if(position == InsertPosition.FRONT){
            newId = first - 1.0
        }
        // Place to back of track
        else if(position == InsertPosition.BACK){
            newId = last + 1.0
        }

        // Else is completely new point (id 0.0)

        return newId
    }

    /**
     * Calculate approximative new waypoint coordinates
     *
     * @param start
     * @param end
     * @return
     */
    private fun getWaypointCoords(start: Waypoint, end: Waypoint): Pair<Double,Double>{
        val midLat = (start.lat + end.lat) / 2.0
        val midLng = (start.lng + end.lng) / 2.0

        return Pair(midLat, midLng)
    }


}