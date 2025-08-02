package com.minapps.trackeditor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey val id: String,
    val lat: Double,
    val lng: Double,
    val orderIndex: Int // useful for track order
)
