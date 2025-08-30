package com.minapps.trackeditor.feature_map_editor.domain.model

/**
 * Domain model representing a single waypoint (GPS point) in a track.
 * Used to represent waypoint for the UI
 *
 * @property id Unique identifier for the waypoint (Double is unusual, usually Int or Long is used).
 * @property lat Latitude coordinate of the waypoint.
 */
data class SimpleWaypoint(
    val id: Double,
    val lat: Double,
    val lng: Double,
)