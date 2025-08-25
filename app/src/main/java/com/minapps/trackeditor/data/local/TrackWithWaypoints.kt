package com.minapps.trackeditor.data.local

import androidx.room.Relation
import androidx.room.Embedded

data class TrackWithWaypoints(
    @Embedded val track: TrackEntity,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "trackOwnerId"
    )
    val waypoints: List<WaypointEntity>
)
