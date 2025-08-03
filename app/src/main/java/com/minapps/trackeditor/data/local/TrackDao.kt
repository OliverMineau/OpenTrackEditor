package com.minapps.trackeditor.data.local

import androidx.room.*

/**
 * DAO (Data Access Object) for Tracks and Waypoints.
 * Handles all database operations for the track editor.
 */
@Dao
interface TrackDao {

    /**
     * Insert a new Track into the database.
     * @param track TrackEntity to insert
     * @return ID of the inserted track (auto-generated primary key)
     */
    @Insert
    suspend fun insertTrack(track: TrackEntity): Long

    /**
     * Fetch a single track by its ID.
     * @param id Track ID
     * @return TrackEntity or null if not found
     */
    @Query("SELECT * FROM tracks WHERE trackId = :id LIMIT 1")
    suspend fun getTrackById(id: Int): TrackEntity?

    /**
     * Insert a waypoint into the database.
     * If the waypoint already exists, it will be replaced.
     * @param waypoint
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity)

    /**
     * Inserts a waypoint, ensuring the parent track exists.
     *
     * If the waypoint's track doesn't exist yet (It should but just in case):
     *  - Creates a new "Untitled Track"
     *  - Uses the new track's ID as the waypoint's trackOwnerId
     *
     * This method is marked @Transaction to ensure both inserts happen atomically.
     *
     * @param waypoint
     */
    @Transaction
    suspend fun insertWaypointWithTrackCheck(waypoint: WaypointEntity) {
        val existing = getTrackById(waypoint.trackOwnerId)
        val trackId = existing?.trackId ?: insertTrack(
            TrackEntity(
                name = "Untitled Track",
                description = null,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()
        insertWaypoint(waypoint.copy(trackOwnerId = trackId))
    }

    /**
     * Get all waypoints for a specific track, ordered by their ID.
     * @param trackId Track ID
     */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC")
    suspend fun getTrackWaypoints(trackId: Int): List<WaypointEntity>





    /**
     * Get all waypoints in the database (for debugging or global operations).
     */
    @Query("SELECT * FROM waypoints")
    suspend fun getAllWaypoints(): List<WaypointEntity>

    /**
     * Delete a single waypoint debug (for debugging or global operations).
     */
    @Delete
    suspend fun deleteWaypoint(waypoint: WaypointEntity)





    /**
     * Delete all waypoints from the database.
     */
    @Query("DELETE FROM waypoints")
    suspend fun clearWaypoints()

    /**
     * Delete all tracks from the database.
     */
    @Query("DELETE FROM tracks")
    suspend fun clearTracks()

    /**
     * Clears the entire database (both tracks and waypoints).
     * Runs inside a single transaction to ensure atomicity.
     */
    @Transaction
    suspend fun clearAll() {
        clearWaypoints()
        clearTracks()
    }
}
