package com.minapps.trackeditor.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing a Waypoint.
 *
 * This is the persistence-layer model stored in the local DB.
 * It maps directly to the "waypoints" table and has a foreign key
 * relationship to the TrackEntity.
 *
 * @property waypointId Unique ID for the waypoint.
 *        - Using Double here is unusual; typically Int or Long is used.
 *        - If this represents a sequence/index, consider making it a composite key with trackOwnerId.
 *
 * @property latitude Latitude of the waypoint.
 * @property longitude Longitude of the waypoint.
 * @property elevation Optional elevation in meters.
 * @property trackOwnerId Foreign key pointing to the parent TrackEntity.
 */
@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["trackId"],
            childColumns = ["trackOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["trackOwnerId", "waypointId"],
    indices = [Index("trackOwnerId")]
)
data class WaypointEntity(
    val waypointId: Double = 0.0,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val time: String?,
    val trackOwnerId: Int // FK to TrackEntity
)