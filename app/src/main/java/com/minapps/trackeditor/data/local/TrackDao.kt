package com.minapps.trackeditor.data.local

import androidx.room.*

/**
 * DAO (Data Access Object) for Tracks and Waypoints.
 * Handles all database operations for the track editor.
 */
@Dao
interface TrackDao {

    // ==========================
    // TRACK OPERATIONS
    // ==========================

    /** Insert a new Track into the database. Returns generated ID. */
    @Insert
    suspend fun insertTrack(track: TrackEntity): Long

    /** Remove a track by its ID. */
    @Query("DELETE FROM tracks WHERE trackId = :trackId ")
    suspend fun removeTrack(trackId: Int)

    /** Get a track by its ID. */
    @Query("SELECT * FROM tracks WHERE trackId = :id LIMIT 1")
    suspend fun getTrackById(id: Int): TrackEntity?

    /** Get all track IDs. */
    @Query("SELECT trackId FROM tracks")
    suspend fun getTrackIds(): List<Int>

    /** Delete all tracks. */
    @Query("DELETE FROM tracks")
    suspend fun clearTracks()



    // ==========================
    // WAYPOINT BASIC OPERATIONS
    // ==========================

    /** Insert or replace a waypoint. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity)

    /** Insert multiple waypoints at once. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<WaypointEntity>)

    /** Insert waypoint with track check; creates new track if necessary. */
    @Transaction
    suspend fun insertWaypointWithTrackCheck(waypoint: WaypointEntity) {
        val existing = getTrackById(waypoint.trackOwnerId)
        val trackId = existing?.trackId ?: insertTrack(
            TrackEntity(
                name = "Untitled Track", description = null, createdAt = System.currentTimeMillis()
            )
        ).toInt()
        insertWaypoint(waypoint.copy(trackOwnerId = trackId))
    }

    /** Get all waypoints. */
    @Query("SELECT * FROM waypoints")
    suspend fun getAllWaypoints(): List<WaypointEntity>

    /** Get waypoints for a specific track. */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC")
    suspend fun getTrackWaypoints(trackId: Int): List<WaypointEntity>

    /** Get sampled waypoints for a track. */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId AND waypointId % :sampleRate = 0 ORDER BY waypointId ASC")
    suspend fun getTrackWaypointsSample(trackId: Int, sampleRate: Int): List<WaypointEntity>

    /** Get first waypoint ID of a track. */
    @Query("""SELECT MIN(waypointId) FROM waypoints WHERE trackOwnerId = :trackId""")
    suspend fun getTrackFirstWaypointId(trackId: Int): Double?

    /** Get last waypoint ID of a track. */
    @Query("""SELECT MAX(waypointId) FROM waypoints WHERE trackOwnerId = :trackId""")
    suspend fun getTrackLastWaypointId(trackId: Int): Double?

    /** Get the index of a waypoint in a track. */
    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId = :trackId AND waypointId < :id")
    suspend fun getWaypointIndex(trackId: Int, id: Double): Int?

    /** Get waypoint by index for a track. */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC LIMIT 1 OFFSET :index")
    suspend fun getWaypoint(trackId: Int, index: Int): WaypointEntity?



    // ==========================
    // WAYPOINT VISIBILITY / BOUNDING
    // ==========================

    /** Count visible waypoints in bounding box. */
    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId = :trackId AND latitude BETWEEN :latSouth AND :latNorth AND longitude BETWEEN :lonWest AND :lonEast")
    suspend fun getVisibleTrackWaypointsCount(
        trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double
    ): Double

    /** Get visible waypoints in bounding box. */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId AND latitude BETWEEN :latSouth AND :latNorth AND longitude BETWEEN :lonWest AND :lonEast ORDER BY waypointId ASC")
    suspend fun getVisibleTrackWaypoints(
        trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double
    ): List<WaypointEntity>

    /** Get visible waypoint chunks in bounding box. */
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

    /** Get all tracks with visible waypoints. */
    @Query("""
        SELECT * FROM waypoints
        WHERE latitude BETWEEN :latSouth AND :latNorth
          AND longitude BETWEEN :lonWest AND :lonEast
        ORDER BY trackOwnerId ASC, waypointId ASC
    """)
    suspend fun getTracksWithVisibleWaypoints(
        latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double
    ): List<WaypointEntity>

    /** Count all tracks with visible waypoints. */
    @Query("""
        SELECT COUNT(*) FROM waypoints
        WHERE latitude BETWEEN :latSouth AND :latNorth
          AND longitude BETWEEN :lonWest AND :lonEast
        ORDER BY trackOwnerId ASC, waypointId ASC
    """)
    suspend fun getTracksWithVisibleWaypointsCount(
        latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double
    ): Double

    /** Get IDs of tracks with visible waypoints. */
    @Query("""
        SELECT DISTINCT trackOwnerId FROM waypoints
        WHERE latitude BETWEEN :latSouth AND :latNorth
          AND longitude BETWEEN :lonWest AND :lonEast
        ORDER BY trackOwnerId ASC
    """)
    suspend fun getTrackIdsWithVisibleWaypoints(
        latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double
    ): List<Int>

    /** Get waypoints in a bounding box with optional step. */
    @Query("""
        SELECT * FROM waypoints
        WHERE trackOwnerId = :trackId
          AND latitude BETWEEN :south AND :north
          AND longitude BETWEEN :west AND :east
          AND (:step = 1 OR (CAST(waypointId AS INTEGER) % :step) = 0)
        ORDER BY waypointId
    """)
    suspend fun getWaypointsInBoundingBox(
        trackId: Int, south: Double, north: Double, west: Double, east: Double, step: Int = 1
    ): List<WaypointEntity>



    // ==========================
    // WAYPOINT BATCH OPERATIONS
    // ==========================

    /** Get waypoints by chunk for a track. */
    @Query("SELECT * FROM waypoints WHERE trackOwnerId = :trackId ORDER BY waypointId ASC LIMIT :chunkSize OFFSET :offset")
    suspend fun getWaypointsByChunk(trackId: Int, chunkSize: Int, offset: Int): List<WaypointEntity>

    /** Get waypoints batch by ID range. */
    @Query("""
        SELECT * FROM waypoints 
        WHERE trackOwnerId = :trackId 
          AND waypointId BETWEEN :p1 AND :p2
        ORDER BY waypointId ASC
        LIMIT :batchSize OFFSET :offset
    """)
    suspend fun getWaypointsBatch(trackId: Int, p1: Double, p2: Double, batchSize: Int, offset: Int): List<WaypointEntity>

    /** Get waypoints batch by ID range. */
    @Query("""
        SELECT * FROM waypoints 
        WHERE trackOwnerId = :trackId 
          AND waypointId BETWEEN :p1 AND :p2
        ORDER BY waypointId ASC
        LIMIT :batchSize
    """)
    suspend fun getWaypointsBatchFromId(trackId: Int, p1: Double, p2: Double, batchSize: Int): List<WaypointEntity>

    /** Get batch of waypoints ascending. */
    @Query("""
        SELECT * FROM waypoints
        WHERE trackOwnerId = :trackId
        ORDER BY waypointId ASC
        LIMIT :batchSize OFFSET :offset
    """)
    suspend fun getWaypointsBatch(trackId: Int, batchSize: Int, offset: Int): List<WaypointEntity>

    /** Get batch of waypoints descending. */
    @Query("""
        SELECT * FROM waypoints
        WHERE trackOwnerId = :trackId
        ORDER BY waypointId DESC
        LIMIT :batchSize OFFSET :offset
    """)
    suspend fun getWaypointsBatchDescending(trackId: Int, batchSize: Int, offset: Int): List<WaypointEntity>

    /** Get batch by Room rowid ascending. */
    @Query("""
        SELECT * FROM waypoints
        WHERE trackOwnerId = :trackId
        ORDER BY rowid ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getWaypointsBatchByRowId(trackId: Int, limit: Int, offset: Int): List<WaypointEntity>

    /** Get batch by Room rowid descending. */
    @Query("""
        SELECT * FROM waypoints
        WHERE trackOwnerId = :trackId
        ORDER BY rowid DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getWaypointsBatchByRowIdDesc(trackId: Int, limit: Int, offset: Int): List<WaypointEntity>

    /** Delete a batch of waypoints by offset. */
    @Query("""
        DELETE FROM waypoints
        WHERE trackOwnerId = :trackId
        AND waypointId IN (
            SELECT waypointId FROM waypoints
            WHERE trackOwnerId = :trackId
            ORDER BY waypointId ASC
            LIMIT :batchSize OFFSET :offset
        )
    """)
    suspend fun deleteWaypointsBatch(trackId: Int, batchSize: Int, offset: Int): Int

    /** Delete multiple waypoints. */
    @Delete
    suspend fun deleteWaypoints(waypoints: List<WaypointEntity>)



    // ==========================
    // WAYPOINT UPDATES
    // ==========================

    /** Update a list of waypoints. */
    @Update
    suspend fun updateWaypoints(waypoints: List<WaypointEntity>)

    /** Update waypoint ID and track owner. */
    @Query("""
        UPDATE waypoints 
        SET waypointId = :newId, trackOwnerId = :newTrackId
        WHERE trackOwnerId = :oldTrackId AND waypointId = :oldId
    """)
    suspend fun updateWaypointIdAndTrack(oldTrackId: Int, oldId: Double, newId: Double, newTrackId: Int)

    /** Change all waypoints from one track to another. */
    @Query("""
        UPDATE waypoints
        SET trackOwnerId = :toTrackId
        WHERE trackOwnerId = :fromTrackId
    """)
    suspend fun changeTrackId(fromTrackId: Int, toTrackId: Int)



    // ==========================
    // WAYPOINT DELETES
    // ==========================

    /** Delete a single waypoint by entity. */
    @Delete
    suspend fun deleteWaypoint(waypoint: WaypointEntity)

    /** Delete a single waypoint by ID. */
    @Query("DELETE FROM waypoints WHERE trackOwnerId = :trackId AND waypointId = :id")
    suspend fun deleteWaypoint(trackId: Int, id: Double)

    /** Delete a segment of waypoints by range. */
    @Query("DELETE FROM waypoints WHERE trackOwnerId = :trackId AND waypointId > :startId AND waypointId < :endId")
    suspend fun deleteSegment(trackId: Int, startId: Double, endId: Double)

    /** Delete all waypoints. */
    @Query("DELETE FROM waypoints")
    suspend fun clearWaypoints()



    // ==========================
    // TRACK MANIPULATION / ID SHIFTING / REVERSING
    // ==========================

    /** Shift waypoint IDs temporarily by 1,000,000. */
    @Query("""
        UPDATE waypoints
        SET waypointId = waypointId + 1000000
        WHERE trackOwnerId = :trackId
    """)
    suspend fun shiftIdsTemporarily(trackId: Int)

    /** Shift IDs in range. */
    @Query("""
        UPDATE waypoints
        SET waypointId = waypointId + 1000000
        WHERE trackOwnerId = :trackId
          AND waypointId BETWEEN :p1 AND :p2
    """)
    suspend fun shiftIds(trackId: Int, p1: Double, p2: Double)

    /** Shift all IDs for a track. */
    @Query("""
        UPDATE waypoints
        SET waypointId = waypointId + 1000000
        WHERE trackOwnerId = :trackId
    """)
    suspend fun shiftIds(trackId: Int)

    /** Reassign reversed IDs for entire track. */
    @Query("""
        UPDATE waypoints
        SET waypointId = (
            (SELECT (MIN(waypointId) - 1000000) + (MAX(waypointId) - 1000000)
             FROM waypoints WHERE trackOwnerId = :trackId
            ) - (waypointId - 1000000)
        )
        WHERE trackOwnerId = :trackId
    """)
    suspend fun reassignReversedIds(trackId: Int)

    /** Reassign reversed IDs for a range. */
    @Query("""
        UPDATE waypoints
        SET waypointId = (
            (SELECT (MIN(waypointId) - 1000000) + (MAX(waypointId) - 1000000)
             FROM waypoints
             WHERE trackOwnerId = :trackId
               AND waypointId BETWEEN (:p1 + 1000000) AND (:p2 + 1000000)
            ) - (waypointId - 1000000)
        )
        WHERE trackOwnerId = :trackId
          AND waypointId BETWEEN (:p1 + 1000000) AND (:p2 + 1000000)
    """)
    suspend fun reassignReversedIds(trackId: Int, p1: Double, p2: Double)

    /** Reverse track IDs for a range. */
    @Transaction
    suspend fun reverseTrack(trackId: Int, p1: Double, p2: Double) {
        shiftIds(trackId, p1, p2)
        reassignReversedIds(trackId, p1, p2)
    }

    /** Reverse all waypoint IDs for a track. */
    @Transaction
    suspend fun reverseTrack(trackId: Int) {
        shiftIds(trackId)
        reassignReversedIds(trackId)
    }



    // ==========================
    // COUNT OPERATIONS
    // ==========================

    /** Count waypoints for a track. */
    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId = :trackId")
    suspend fun countWaypointsForTrack(trackId: Int): Int

    /** Count waypoints for multiple tracks. */
    @Query("SELECT COUNT(*) FROM waypoints WHERE trackOwnerId in (:trackIds)")
    suspend fun countWaypointsForTracks(trackIds: List<Int>): Int

    /** Get number of waypoints for a track. */
    @Query("""
        SELECT COUNT(*) FROM waypoints
        WHERE trackOwnerId = :trackId
    """)
    suspend fun getTrackWaypointCount(trackId: Int): Int

    /** Get interval size in waypoint IDs. */
    @Query("""
        SELECT COUNT(waypointId)
        FROM waypoints
        WHERE trackOwnerId = :trackId AND
        waypointId BETWEEN :p1 AND :p2
    """)
    suspend fun getIntervalSize(trackId: Int, p1: Double, p2: Double): Int



    // ==========================
    // TRANSACTIONS
    // ==========================

    /** Clear all tracks and waypoints in one transaction. */
    @Transaction
    suspend fun clearAll() {
        clearWaypoints()
        clearTracks()
    }

}
