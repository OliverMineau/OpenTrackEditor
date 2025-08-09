package com.minapps.trackeditor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main Room database for the app.
 * Holds both Tracks and Waypoints tables.
 *
 * @Database annotation:
 *  - entities: list of all tables (TrackEntity & WaypointEntity)
 *  - version: database version (used for migrations)
 *  - exportSchema: false to avoid exporting schema files
 */
@Database(
    entities = [TrackEntity::class, WaypointEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Exposes the DAO (Data Access Object) to perform
     * all CRUD operations on tracks and waypoints.
     */
    abstract fun trackDao(): TrackDao
}
