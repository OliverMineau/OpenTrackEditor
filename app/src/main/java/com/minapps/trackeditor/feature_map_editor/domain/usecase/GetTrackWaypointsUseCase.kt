package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.util.TrackSimplifier
import jakarta.inject.Inject

class GetTrackWaypointsUseCase @Inject constructor(
    private val repository: EditTrackRepository,
    private val trackSimplifier: TrackSimplifier
) {

    private val DISPLAY_POINT_COUNT_MAX = 10000
    private val INITIAL_TOLERANCE = 500.0 // meters
    private val TOLERANCE_MULTIPLIER = 5
    private val CHUNK_SIZE = 10_000

    /**
     * Retrieves all waypoints of a track, simplified if necessary
     */
    suspend operator fun invoke(
        trackId: Int,
        latNorth: Double?,
        latSouth: Double?,
        lonWest: Double?,
        lonEast: Double?
    ): List<Waypoint> {

        // Get number of points in given track or in in view
        val waypointCount = if (latNorth == null || latSouth == null || lonWest == null || lonEast == null) {
            repository.getTrackLastWaypointId(trackId)?: 0.0
        } else {
            repository.getVisibleTrackWaypointsCount(trackId, latNorth, latSouth, lonWest, lonEast)
        }

        // If small track display all points
        if (waypointCount <= DISPLAY_POINT_COUNT_MAX) {
            return repository.getTrackWaypoints(trackId)
        }

        // Load track in chunks and simplify
        val simplifiedPoints = mutableListOf<Waypoint>()
        var offset = 0
        while (offset < waypointCount) {

            // Get Chunk of track
            val chunk = if (latNorth == null || latSouth == null || lonWest == null || lonEast == null) {
                repository.getTrackWaypointsChunk(trackId, CHUNK_SIZE + 1, offset)
            } else {
                repository.getVisibleTrackWaypointsChunk(trackId, latNorth, latSouth, lonWest, lonEast, CHUNK_SIZE + 1, offset)
            }

            if (chunk.isNotEmpty()) {

                // Apply filtering algorithm
                val simplifiedChunk = trackSimplifier.simplify(chunk, INITIAL_TOLERANCE)

                // Merge with previous, avoid duplicates at boundaries
                if (simplifiedPoints.isNotEmpty() && simplifiedChunk.isNotEmpty()) {
                    if (simplifiedPoints.last().id == simplifiedChunk.first().id) {
                        simplifiedChunk.removeAt(0)
                    }
                }

                simplifiedPoints.addAll(simplifiedChunk)
            }

            offset += CHUNK_SIZE
        }

        // Final simplification pass with adaptive tolerance
        var tolerance = INITIAL_TOLERANCE
        var resultTrack = trackSimplifier.simplify(simplifiedPoints, tolerance)

        // TODO Heavy calculations here
        //  Simplify track to get less than MAX points
        while (resultTrack.size > DISPLAY_POINT_COUNT_MAX) {
            tolerance *= TOLERANCE_MULTIPLIER
            resultTrack = trackSimplifier.simplify(simplifiedPoints, tolerance)
        }

        Log.d("opti", "Track simplified to ${resultTrack.size} points (tolerance=$tolerance m)")
        return resultTrack
    }
}
