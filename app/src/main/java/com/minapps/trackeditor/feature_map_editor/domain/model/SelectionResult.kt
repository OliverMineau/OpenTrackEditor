package com.minapps.trackeditor.feature_map_editor.domain.model

import com.minapps.trackeditor.feature_map_editor.domain.model.SimpleWaypoint
import com.minapps.trackeditor.core.domain.type.InsertPosition

sealed class SelectionResult {
    data class UpdatedState(
        val selectedTracks: List<Int>,
        val selectedPoints: List<Pair<Int, Double>>,
        val direction: InsertPosition? = null
    ) : SelectionResult()

    data class WaypointAdded(val trackId: Int, val waypoint: SimpleWaypoint) : SelectionResult()
    data class WaypointRemoved(val trackId: Int, val waypointId: Double) : SelectionResult()
    object None : SelectionResult()
}