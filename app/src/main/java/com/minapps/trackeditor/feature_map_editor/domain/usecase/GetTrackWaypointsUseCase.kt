package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.util.TrackSimplifier
import jakarta.inject.Inject

/**
 * Use case responsible for getting all waypoints from a given track
 *
 * @property repository
 */
class GetTrackWaypointsUseCase @Inject constructor(
    private val repository: EditTrackRepository,
    private val trackSimplifier: TrackSimplifier
) {

    val DISPLAY_POINT_COUNT_MAX = 11000


    /**
     * Retrieves all waypoints of track from database
     *
     * @param trackId
     * @return List of waypoints associated to given trackId
     */
    suspend operator fun invoke(trackId: Int, latNorth: Double?, latSouth: Double?, lonWest: Double?, lonEast: Double?): List<Waypoint> {
        var waypointCount = 0.0
        if(latNorth == null || latSouth == null || lonWest == null || lonEast == null){
            waypointCount = repository.getTrackLastWaypointIndex(trackId)

        }else{
            waypointCount = repository.getVisibleTrackWaypointsCount(trackId, latNorth,latSouth, lonWest, lonEast)
        }

        // Small track → just return all points
        if (waypointCount < DISPLAY_POINT_COUNT_MAX) {
            return repository.getTrackWaypoints(trackId)
        }

        val chunkSize = 10_000
        val tolerance = 2.0 //Good for huge tracks
        val simplifiedPoints = mutableListOf<Waypoint>()

        var offset = 0
        while (offset < waypointCount) {
            // 1. Load a chunk
            //val chunk = repository.getVisibleTrackWaypointsChunk(trackId, latNorth,latSouth, lonWest, lonEast, chunkSize + 1, offset)

            var chunk = listOf<Waypoint>()
            if(latNorth == null || latSouth == null || lonWest == null || lonEast == null){
                chunk = repository.getTrackWaypointsChunk(trackId,chunkSize + 1, offset)

            }else{
                chunk = repository.getVisibleTrackWaypointsChunk(trackId, latNorth,latSouth, lonWest, lonEast, chunkSize + 1, offset)
            }


            if (chunk.isNotEmpty()) {
                // 2. Simplify this chunk
                val simplifiedChunk = trackSimplifier.simplify(chunk, tolerance)

                // 3. Merge with previous results (avoid duplicates at boundaries)
                if (simplifiedPoints.isNotEmpty() && simplifiedChunk.isNotEmpty()) {
                    if (simplifiedPoints.last().id == simplifiedChunk.first().id) {
                        simplifiedChunk.removeAt(0)
                    }
                }

                simplifiedPoints.addAll(simplifiedChunk)
            }

            offset += chunkSize
        }

        // 4. Optional: final pass to smooth across chunks
        return trackSimplifier.simplify(simplifiedPoints, tolerance)
    }

}
