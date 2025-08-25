package com.minapps.trackeditor.core.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.domain.usecase.GetTrackWaypointsUseCase
import jakarta.inject.Inject

class UpdateMapViewUseCase @Inject constructor(
    private val repository: EditTrackRepository,
    private val getTrackWaypointsUseCase: GetTrackWaypointsUseCase,
) {

    suspend operator fun invoke(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double,
        showOutline: Boolean,
        showFull: Boolean,
    ): List<Pair<Int, List<Waypoint>>>? {

        val count =
            repository.getTracksWithVisibleWaypointsCount(latNorth, latSouth, lonWest, lonEast)

        val trackIds =
            repository.getTrackIdsWithVisibleWaypoints(latNorth, latSouth, lonWest, lonEast)
        //Log.d("optimise", "Total DB visible points : $count points, ids:$trackIds")

        if(trackIds.isEmpty()){
            return null
        }

        // Update only if possible, maybe only update once we can load all points on screen (zoomed in)
        // and add that to the track outline ?
        if (showFull){//count < 10000 && !showOutline) {
            val tracks =
                repository.getTracksWithVisibleWaypoints(latNorth, latSouth, lonWest, lonEast)

            for (track in tracks) {
                Log.d(
                    "optimise",
                    "Display non filtered points: ${track.first} : ${track.second.size}points"
                )
            }

            return tracks
        } else if (showOutline) {

            var retList = mutableListOf<Pair<Int, List<Waypoint>>>()

            var waypoints =
                getTrackWaypointsUseCase(trackIds[0], latNorth, latSouth, lonWest, lonEast)
            retList.add(Pair(trackIds[0], waypoints))

            Log.d(
                "optimise",
                "Display DouglasPeucker points: ${trackIds} : ${waypoints.size}points"
            )

            return retList
        }

        return null
    }

    suspend fun getVisiblePointCount(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double,
    ): Double {
        return repository.getTracksWithVisibleWaypointsCount(latNorth, latSouth, lonWest, lonEast)
    }
}