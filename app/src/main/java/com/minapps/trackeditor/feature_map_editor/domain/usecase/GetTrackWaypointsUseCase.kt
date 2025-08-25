package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.util.TrackSimplifier
import jakarta.inject.Inject
import kotlin.math.max

class GetTrackWaypointsUseCase @Inject constructor(
    private val repository: EditTrackRepository,
    private val trackSimplifier: TrackSimplifier
) {

    private val DISPLAY_POINT_COUNT_MAX = 3000
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

        val waypointCount = if (latNorth == null || latSouth == null || lonWest == null || lonEast == null) {
            repository.getTrackLastWaypointIndex(trackId)
        } else {
            repository.getVisibleTrackWaypointsCount(trackId, latNorth, latSouth, lonWest, lonEast)
        }

        // Small track → just return all points
        if (waypointCount <= DISPLAY_POINT_COUNT_MAX) {
            return repository.getTrackWaypoints(trackId)
        }

        // Load track in chunks and simplify
        val simplifiedPoints = mutableListOf<Waypoint>()
        var offset = 0
        while (offset < waypointCount) {

            val chunk = if (latNorth == null || latSouth == null || lonWest == null || lonEast == null) {
                repository.getTrackWaypointsChunk(trackId, CHUNK_SIZE + 1, offset)
            } else {
                repository.getVisibleTrackWaypointsChunk(trackId, latNorth, latSouth, lonWest, lonEast, CHUNK_SIZE + 1, offset)
            }

            if (chunk.isNotEmpty()) {
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

        while (resultTrack.size > DISPLAY_POINT_COUNT_MAX) {
            tolerance *= TOLERANCE_MULTIPLIER
            resultTrack = trackSimplifier.simplify(simplifiedPoints, tolerance)
        }

        // Final fallback subsample if still too many points
        if (resultTrack.size > DISPLAY_POINT_COUNT_MAX) {
            val step = max(1, resultTrack.size / DISPLAY_POINT_COUNT_MAX)
            resultTrack = resultTrack.filterIndexed { index, _ -> index % step == 0 }.toMutableList()
        }

        Log.d("opti", "Track simplified to ${resultTrack.size} points (tolerance=$tolerance m)")
        return resultTrack
    }
}
