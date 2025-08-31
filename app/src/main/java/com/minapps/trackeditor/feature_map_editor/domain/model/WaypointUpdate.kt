package com.minapps.trackeditor.feature_map_editor.domain.model

/**
 * Types of waypoint updates
 *
 */
sealed class WaypointUpdate {
    data class Added(val trackId: Int, val point: SimpleWaypoint) : WaypointUpdate()
    data class ViewChanged(val trackId: Int, val points: List<SimpleWaypoint>) : WaypointUpdate()
    data class AddedList(val trackId: Int, val points: List<SimpleWaypoint>, val center: Boolean) :
        WaypointUpdate()

    data class Removed(val trackId: Int, val index: Int) : WaypointUpdate()
    data class RemovedById(val trackId: Int, val id: Double) : WaypointUpdate()
    data class RemovedSegment(val trackId: Int, val startId: Double, val endId: Double) :
        WaypointUpdate()

    data class RemovedTracks(val trackIds: List<Int>) : WaypointUpdate()
    data class Moved(val trackId: Int, val points: List<Pair<Double, Double>>) : WaypointUpdate()
    data class MovedDone(val trackId: Int, val pointId: Double, val point: Pair<Double, Double>) :
        WaypointUpdate()

    data class Cleared(val trackId: Int) : WaypointUpdate()

    data class JoinedTracks(val trackIdStayed: Int, val trackIdRemoved: Int) : WaypointUpdate()
}