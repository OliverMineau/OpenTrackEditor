package com.minapps.trackeditor.core.domain.model

/**
 * Domain model representing a single waypoint (GPS point) in a track.
 *
 * @property id Unique identifier for the waypoint (Double is unusual, usually Int or Long is used).
 * @property lat Latitude coordinate of the waypoint.
 * @property lng Longitude coordinate of the waypoint.
 * @property elv Elevation of the waypoint, can be null (so we can interpolate later)
 * @property trackId Foreign key reference to the track this waypoint belongs to.
 */
data class Waypoint(
    val id: Double,
    val lat: Double,
    val lng: Double,
    val elv: Double?,
    val time: String?,
    val trackId: Int,
)