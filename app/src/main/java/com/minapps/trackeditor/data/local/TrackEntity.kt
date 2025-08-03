package com.minapps.trackeditor.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing a Track.
 *
 * This is the persistence-layer model stored in the local DB.
 * It maps directly to the "tracks" table.
 *
 * @property trackId Primary key, auto-generated.
 * @property name Human-readable name of the track.
 * @property description Optional description of the track.
 * @property createdAt Epoch timestamp (in millis) when the track was created.
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val trackId: Int = 0,
    val name: String,
    val description: String?,
    val createdAt: Long,
    // Add other metadata fields here
)
