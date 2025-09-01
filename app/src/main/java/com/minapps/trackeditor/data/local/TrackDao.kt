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

    @Query("DELETE FROM tracks WHERE trackId = :trackId ")
    suspend fun removeTrack(trackId: Int)

    /**
     * Fetch a single track by its ID.
     * @param id Track ID
     * @return TrackEntity or null if not found
     */
    @Query("SELECT * FROM tracks WHERE trackId = :id LIMIT 1")
    suspend fun getTrackById(id: Int): TrackEntity?


    @Query("""SELECT MIN(waypointId) FROM waypoints WHERE trackOwnerId = :trackId""")
    suspend fun getTrackFirstWaypointId(trackId: Int): Double?

    @Query("""SELECT MAX(waypointId) FROM waypoints WHERE trackOwnerId = :trackId""")
    suspend fun getTrackLastWaypointId(trackId: Int): Double?

    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId = :trackId AND waypointId < :id")
    suspend fun getWaypointIndex(trackId: Int, id: Double): Int?

    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC LIMIT 1 OFFSET :index")
    suspend fun getWaypoint(trackId: Int, index: Int): WaypointEntity?

    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId = :trackId AND latitude BETWEEN :latSouth AND :latNorth AND longitude BETWEEN :lonWest AND :lonEast")
    suspend fun getVisibleTrackWaypointsCount(
        trackId: Int,
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): Double

    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId AND latitude BETWEEN :latSouth AND :latNorth AND longitude BETWEEN :lonWest AND :lonEast ORDER BY waypointId ASC")
    suspend fun getVisibleTrackWaypoints(
        trackId: Int,
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId AND latitude BETWEEN :latSouth AND :latNorth AND longitude BETWEEN :lonWest AND :lonEast ORDER BY waypointId ASC LIMIT :chunkSize OFFSET :offset")
    suspend fun getVisibleTrackWaypointsChunk(
        trackId: Int,
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double,
        chunkSize: Int,
        offset: Int
    ): List<WaypointEntity>

    @Query(
        """
    SELECT * FROM waypoints
    WHERE latitude BETWEEN :latSouth AND :latNorth
      AND longitude BETWEEN :lonWest AND :lonEast
    ORDER BY trackOwnerId ASC, waypointId ASC
"""
    )
    suspend fun getTracksWithVisibleWaypoints(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): List<WaypointEntity>

    @Query(
        """
    SELECT COUNT(*) FROM waypoints
    WHERE latitude BETWEEN :latSouth AND :latNorth
      AND longitude BETWEEN :lonWest AND :lonEast
    ORDER BY trackOwnerId ASC, waypointId ASC
"""
    )
    suspend fun getTracksWithVisibleWaypointsCount(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): Double

    @Query(
        """
    SELECT DISTINCT trackOwnerId FROM waypoints
    WHERE latitude BETWEEN :latSouth AND :latNorth
      AND longitude BETWEEN :lonWest AND :lonEast
    ORDER BY trackOwnerId ASC
"""
    )
    suspend fun getTrackIdsWithVisibleWaypoints(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): List<Int>


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
     * Inserts a list of waypoints
     *
     * @param waypoints
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<WaypointEntity>)

    @Query("SELECT trackId FROM tracks")
    suspend fun getTrackIds(): List<Int>

    /**
     * Get all waypoints for a specific track, ordered by their ID.
     * @param trackId Track ID
     */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC")
    suspend fun getTrackWaypoints(trackId: Int): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId AND waypointId % :sampleRate = 0 ORDER BY waypointId ASC")
    suspend fun getTrackWaypointsSample(trackId: Int, sampleRate: Int): List<WaypointEntity>


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

    @Query("DELETE FROM waypoints WHERE trackOwnerId = :trackId AND waypointId = :id")
    suspend fun deleteWaypoint(trackId: Int, id: Double)

    @Query("DELETE FROM waypoints WHERE trackOwnerId = :trackId AND waypointId > :startId AND waypointId < :endId")
    suspend fun deleteSegment(trackId: Int, startId: Double, endId: Double)

    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId = :trackId")
    suspend fun countWaypointsForTrack(trackId: Int): Int

    /*@Query(
        """
    DELETE FROM waypoints
    WHERE waypointId IN (
        SELECT waypointId FROM (
            SELECT waypointId, ROW_NUMBER() OVER (ORDER BY waypointId ASC) AS rn
            FROM waypoints
            WHERE trackOwnerId = :trackId
              AND waypointId BETWEEN :p1 AND :p2
        )
        WHERE rn % :step = 0
    )
"""
    )
    suspend fun removeWaypointsByStep(trackId: Int, step: Int, p1: Double, p2: Double)

    @Query(
        """
    DELETE FROM waypoints
    WHERE waypointId IN (
        SELECT waypointId FROM (
            SELECT waypointId, ROW_NUMBER() OVER (ORDER BY waypointId ASC) AS rn
            FROM waypoints
            WHERE trackOwnerId = :trackId
        )
        WHERE rn % :step = 0
    )
"""
    )
    suspend fun removeWaypointsByStep(trackId: Int, step: Int)*/


    @Query("""
    DELETE FROM waypoints
    WHERE trackOwnerId = :trackId
      AND waypointId BETWEEN :p1 AND :p2
      AND waypointId NOT IN (
          SELECT waypointId FROM (
              SELECT waypointId, ROW_NUMBER() OVER (ORDER BY waypointId ASC) AS rn
              FROM waypoints
              WHERE trackOwnerId = :trackId
                AND waypointId BETWEEN :p1 AND :p2
          )
          WHERE rn % :step = 0 OR waypointId = :p1 OR waypointId = :p2
          )
    """)
    suspend fun removeWaypointsByStep(
        trackId: Int,
        step: Int,
        p1: Double,
        p2: Double
    )

    @Query("""
    DELETE FROM waypoints
    WHERE trackOwnerId = :trackId
      AND waypointId NOT IN (
          SELECT waypointId FROM (
              SELECT waypointId,
                     ROW_NUMBER() OVER (ORDER BY waypointId ASC) AS rn,
                     COUNT(*) OVER () AS total
              FROM waypoints
              WHERE trackOwnerId = :trackId
          )
          WHERE rn % :step = 0 
             OR rn = 1   
             OR rn = total
      )
""")
    suspend fun removeWaypointsByStep(
        trackId: Int,
        step: Int
    )





    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId in (:trackIds)")
    suspend fun countWaypointsForTracks(trackIds: List<Int>): Int

    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC LIMIT :chunkSize OFFSET :offset")
    suspend fun getWaypointsByChunk(trackId: Int, chunkSize: Int, offset: Int): List<WaypointEntity>

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

    @Query(
        """
        SELECT * FROM waypoints
        WHERE trackOwnerId = :trackId
          AND latitude BETWEEN :south AND :north
          AND longitude BETWEEN :west AND :east
          AND (:step = 1 OR (CAST(waypointId AS INTEGER) % :step) = 0)
        ORDER BY waypointId
    """
    )
    suspend fun getWaypointsInBoundingBox(
        trackId: Int,
        south: Double,
        north: Double,
        west: Double,
        east: Double,
        step: Int = 1
    ): List<WaypointEntity>


    @Query(
        """
        UPDATE waypoints
        SET waypointId = waypointId + 1000000
        WHERE trackOwnerId = :trackId
        """
    )
    suspend fun shiftIdsTemporarily(trackId: Int)

    @Query(
        """
        WITH ordered AS (
            SELECT waypointId,
                   ROW_NUMBER() OVER (ORDER BY waypointId) - 1 AS rn,
                   COUNT(*) OVER () - 1 AS maxRn
            FROM waypoints
            WHERE trackOwnerId = :trackId
        )
        UPDATE waypoints
        SET waypointId = :newStart + (
            SELECT CASE 
                     WHEN :indexDescending = 1 THEN (maxRn - rn) 
                     ELSE rn 
                   END
            FROM ordered
            WHERE ordered.waypointId = waypoints.waypointId
        )
        WHERE trackOwnerId = :trackId
        """
    )
    suspend fun reassignIdsAscending(
        trackId: Int,
        newStart: Double,
        indexDescending: Boolean
    )


    @Query(
        """
        WITH ordered AS (
            SELECT waypointId,
                   ROW_NUMBER() OVER (ORDER BY waypointId) - 1 AS rn,
                   COUNT(*) OVER () - 1 AS maxRn
            FROM waypoints
            WHERE trackOwnerId = :trackId
        )
        UPDATE waypoints
        SET waypointId = :newStart - (
            SELECT CASE 
                     WHEN :indexDescending = 1 THEN (maxRn - rn) 
                     ELSE rn 
                   END
            FROM ordered
            WHERE ordered.waypointId = waypoints.waypointId
        )
        WHERE trackOwnerId = :trackId
        """
    )
    suspend fun reassignIdsDescending(
        trackId: Int,
        newStart: Double,
        indexDescending: Boolean
    )

    @Transaction
    suspend fun renumberTrack(
        trackId: Int,
        newStart: Double,
        descending: Boolean = false,
        indexDescending: Boolean = false
    ) {
        shiftIdsTemporarily(trackId)
        if (descending) {
            reassignIdsDescending(trackId, newStart, indexDescending)
        } else {
            reassignIdsAscending(trackId, newStart, indexDescending)
        }
    }

    @Query(
        """
        UPDATE waypoints
        SET trackOwnerId = :toTrackId
        WHERE trackOwnerId = :fromTrackId
    """
    )
    suspend fun changeTrackId(fromTrackId: Int, toTrackId: Int)

    @Query(
        """
        SELECT COUNT(waypointId)
        FROM waypoints
        WHERE trackOwnerId = :trackId AND
        waypointId BETWEEN :p1 AND :p2
    """
    )
    suspend fun getIntervalSize(trackId: Int, p1: Double, p2: Double): Int
}

