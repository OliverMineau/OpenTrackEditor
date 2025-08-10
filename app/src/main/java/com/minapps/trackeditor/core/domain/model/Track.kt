package com.minapps.trackeditor.core.domain.model

/**
 * Domain model representing a track (a collection of waypoints forming a path).
 *
 * @property id Unique identifier for the track (matches DB primary key).
 * @property name Human-readable name of the track.
 * @property description Optional description providing more details about the track.
 * @property createdAt Timestamp (in millis) when the track was created.
 * @property waypoints List of waypoints that belong to this track.
 */
data class Track(
    val id: Int,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val waypoints: List<Waypoint>?
)