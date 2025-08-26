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

        // Get ids of visible tracks (in view)
        val trackIds =
            repository.getTrackIdsWithVisibleWaypoints(latNorth, latSouth, lonWest, lonEast)

        // If not tracks return
        if (trackIds.isEmpty()) {
            return null
        }

        // Show full track precision
        if (showFull) {
            val tracks =
                repository.getTracksWithVisibleWaypoints(latNorth, latSouth, lonWest, lonEast)

            for (track in tracks) {
                Log.d(
                    "optimise",
                    "Display non filtered points: ${track.first} : ${track.second.size}points"
                )
            }
            return tracks

        }
        // Show rough track outline
        else if (showOutline) {

            // Create return list
            var retList = mutableListOf<Pair<Int, List<Waypoint>>>()

            for (id in trackIds) {

                // Get all visible waypoints
                var waypoints =
                    getTrackWaypointsUseCase(id, latNorth, latSouth, lonWest, lonEast)

                retList.add(Pair(id, waypoints))

                Log.d(
                    "optimise",
                    "Display DouglasPeucker points: ${id} : ${waypoints.size}points"
                )
            }

            return retList
        }

        return null
    }

    /**
     * Get number of potentially visible points in view
     *
     * @param latNorth
     * @param latSouth
     * @param lonWest
     * @param lonEast
     * @return
     */
    suspend fun getVisiblePointCount(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double,
    ): Double {
        return repository.getTracksWithVisibleWaypointsCount(latNorth, latSouth, lonWest, lonEast)
    }
}