package com.minapps.trackeditor.data.mapper

import com.minapps.trackeditor.data.local.WaypointEntity
import com.minapps.trackeditor.core.domain.model.Waypoint

/**
 * Converts a domain Waypoint object into a database WaypointEntity.
 *
 * This is used when saving a Waypoint to the database.
 *
 * @receiver Waypoint (domain model)
 * @return WaypointEntity (database entity)
 */
fun Waypoint.toEntity(): WaypointEntity {
    return WaypointEntity(
        waypointId = id,
        latitude = lat,
        longitude = lng,
        elevation = elv,
        time = time,
        trackOwnerId = trackId,
    )
}

/**
 * Converts a database WaypointEntity into a domain Waypoint object.
 *
 * This is used when reading waypoints from the database and exposing them
 * to the rest of the app in domain form.
 *
 * @receiver WaypointEntity (database entity)
 * @return Waypoint (domain model)
 */
fun WaypointEntity.toDomain(): Waypoint {
    return Waypoint(
        id = waypointId,
        lat = latitude,
        lng = longitude,
        elv = elevation,
        time = time,
        trackId = trackOwnerId,
    )
}
