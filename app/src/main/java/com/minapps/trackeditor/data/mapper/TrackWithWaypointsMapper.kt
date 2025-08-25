package com.minapps.trackeditor.data.mapper

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.TrackWithWaypoints

fun TrackWithWaypoints.toDomain(): Pair<Int, List<Waypoint>> {
    return track.trackId to waypoints.map { it.toDomain() }
}