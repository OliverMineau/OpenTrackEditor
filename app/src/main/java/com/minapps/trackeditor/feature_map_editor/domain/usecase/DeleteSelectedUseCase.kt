package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.WaypointUpdate
import jakarta.inject.Inject

class DeleteSelectedUseCase @Inject constructor(
    private val deleteWaypointUseCase: DeleteWaypointUseCase,
    private val deleteTrackUseCase: DeleteTrackUseCase
) {

    suspend operator fun invoke(
        selectedTracks: List<Int>,
        selectedPoints: List<Pair<Int, Double>>
    ): WaypointUpdate? {

        return when {

            // If one point selected
            selectedPoints.size == 1 -> {
                val point = selectedPoints.first()
                // Delete point
                deleteWaypointUseCase(point.first, point.second)
                WaypointUpdate.RemovedById(point.first, point.second)
            }

            // If two points selected delete segment
            selectedPoints.size == 2 -> {
                val p1 = selectedPoints[0]
                val p2 = selectedPoints[1]
                deleteWaypointUseCase(p1.first, p1.second, p2.second)
                WaypointUpdate.RemovedSegment(p1.first, p1.second, p2.second)
            }

            // If only track.s selected
            selectedPoints.isEmpty() && selectedTracks.isNotEmpty() -> {
                deleteTrackUseCase(selectedTracks)
                WaypointUpdate.RemovedTracks(selectedTracks)
            }

            else -> {
                null
            }
        }
    }
}
