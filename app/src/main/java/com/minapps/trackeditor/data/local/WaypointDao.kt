package com.minapps.trackeditor.data.local

import androidx.room.*

@Dao
interface WaypointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity)

    @Query("SELECT * FROM waypoints ORDER BY orderIndex ASC")
    suspend fun getAllWaypoints(): List<WaypointEntity>

    @Delete
    suspend fun deleteWaypoint(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints")
    suspend fun clearAll()
}
