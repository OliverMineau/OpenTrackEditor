package com.minapps.trackeditor.data.mapper

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.data.local.WaypointEntity
import com.minapps.trackeditor.data.local.TrackEntity

/**
 * Converts a domain Track object into a database TrackEntity.
 *
 * This is used when saving a Track to the database.
 *
 * @receiver Track (domain model)
 * @return TrackEntity (Room entity)
 */
fun Track.toEntity(): TrackEntity{
    return TrackEntity(
        trackId = id,
        name = name,
        description = description,
        createdAt = createdAt,
    )
}

/**
 * Converts a TrackEntity (database entity) into a domain Track object,
 * including its list of Waypoints.
 *
 * This is used when reading a Track and its waypoints from the database.
 *
 * @receiver TrackEntity (Room entity)
 * @param waypoints List of WaypointEntity objects that belong to this track
 * @return Track (domain model)
 */
fun TrackEntity.toDomain(waypoints: List<WaypointEntity>?): Track = Track(
    id = this.trackId,
    name = this.name,
    description = this.description,
    createdAt = this.createdAt,
    waypoints = waypoints?.map { it.toDomain() }
)
