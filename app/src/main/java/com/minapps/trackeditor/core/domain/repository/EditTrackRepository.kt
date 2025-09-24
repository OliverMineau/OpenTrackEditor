package com.minapps.trackeditor.core.domain.repository

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


/**
 * Interface for editing tracks and waypoints.
 *
 * Defines the contract that the data layer must implement.
 * Keeps the domain layer independent from specific data sources
 * (Room, network, etc.).
 */
interface EditTrackRepository {

    // Emits newly added track ID and status
    val addedTracks: Flow<Pair<Int, Boolean>>

    // Add a single waypoint
    suspend fun addWaypoint(waypoint: Waypoint, updateUI: Boolean = false)

    // Get all waypoints
    suspend fun getWaypoints(): List<Waypoint>

    // Get all waypoints for a specific track
    suspend fun getTrackWaypoints(trackId: Int): List<Waypoint>

    // Get chunk of waypoints for a track
    suspend fun getTrackWaypointsChunk(trackId: Int, chunkSize: Int, offset: Int): List<Waypoint>

    // Get sampled waypoints for a track
    suspend fun getTrackWaypointsSample(trackId: Int, sampleRate: Int): List<Waypoint>

    // Insert a new track
    suspend fun insertTrack(track: TrackEntity): Long

    // Remove a track by ID
    suspend fun removeTrack(trackId: Int)

    // Delete a waypoint by ID
    suspend fun deleteWaypoint(trackId: Int, id: Double)

    // Delete a segment of waypoints
    suspend fun deleteSegment(trackId: Int, startId: Double, endId: Double)

    // Get full track with waypoints
    suspend fun getFullTrack(trackId: Int): Track?

    // Delete all tracks and waypoints
    suspend fun clearAll()

    // Add an imported track
    suspend fun addImportedTrack(trackId: Int, center: Boolean): Boolean

    // Add multiple waypoints
    suspend fun addWaypoints(waypoints: List<Waypoint>)

    // Get all track IDs
    suspend fun getTrackIds(): List<Int>

    // Get first waypoint ID for a track
    suspend fun getTrackFirstWaypointId(trackId: Int): Double?

    // Get last waypoint ID for a track
    suspend fun getTrackLastWaypointId(trackId: Int): Double?

    // Get index of a waypoint
    suspend fun getWaypointIndex(trackId: Int, id: Double): Int?

    // Get waypoint by index
    suspend fun getWaypoint(trackId: Int, index: Int): Waypoint?

    // Count visible waypoints in bounding box
    suspend fun getVisibleTrackWaypointsCount(trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): Double

    // Get visible waypoints in bounding box
    suspend fun getVisibleTrackWaypoints(trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): List<Waypoint>

    // Get tracks with visible waypoints in bounding box
    suspend fun getTracksWithVisibleWaypoints(latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): List<Pair<Int, List<Waypoint>>>

    // Count tracks with visible waypoints
    suspend fun getTracksWithVisibleWaypointsCount(latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): Double

    // Get IDs of tracks with visible waypoints
    suspend fun getTrackIdsWithVisibleWaypoints(latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): List<Int>

    // Get chunk of visible waypoints
    suspend fun getVisibleTrackWaypointsChunk(trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double, chunkSize: Int, offset: Int): List<Waypoint>

    // Renumber a track
    suspend fun renumberTrack(trackId: Int, newStart: Double, descending: Boolean, indexDescending: Boolean, toTrackId: Int)

    // Change track ID
    suspend fun changeTrackId(fromTrackId: Int, toTrackId: Int)

    // Get interval size between waypoints
    suspend fun getIntervalSize(trackId: Int, p1: Double, p2: Double): Int

    // Get total interval size of track
    suspend fun getIntervalSize(trackId: Int): Int

    // Remove waypoints by step in range
    suspend fun removeWaypointsByStep(trackId: Int, step: Double, p1: Double, p2: Double)

    // Remove waypoints by step for whole track
    suspend fun removeWaypointsByStep(trackId: Int, step: Double)

    // Reverse a track segment
    suspend fun reverseTrack(trackId: Int, p1: Double, p2: Double)

    // Reverse entire track
    suspend fun reverseTrack(trackId: Int)

    // Delete waypoints in batch
    suspend fun deleteWaypointsBatch(trackId: Int, batchSize: Int, offset: Int): Int

    // Split track at point
    suspend fun splitTrack(trackId: Int, point: Double): List<Int>

    // Split track at segment (delete segment)
    suspend fun splitTrackRange(trackId: Int, point1: Double, point2: Double): List<Int>

}
