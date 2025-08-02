package com.minapps.trackeditor.data.mapper

import com.minapps.trackeditor.data.local.WaypointEntity
import com.minapps.trackeditor.core.domain.model.Waypoint

fun Waypoint.toEntity(orderIndex: Int): WaypointEntity {
    return WaypointEntity(
        id = id,
        lat = lat,
        lng = lng,
        orderIndex = orderIndex
    )
}

fun WaypointEntity.toDomain(): Waypoint {
    return Waypoint(
        id = id,
        lat = lat,
        lng = lng
    )
}
