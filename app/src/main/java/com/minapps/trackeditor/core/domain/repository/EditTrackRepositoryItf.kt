package com.minapps.trackeditor.core.domain.repository

import androidx.room.Insert
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.TrackEntity


/**
 * Interface for editing tracks and waypoints.
 *
 * Defines the contract that the data layer must implement.
 * Keeps the domain layer independent from specific data sources
 * (Room, network, etc.).
 */
interface EditTrackRepositoryItf {

    /**
     * Add a single waypoint to a track.
     *
     * @param waypoint Waypoint domain model to insert.
     */
    suspend fun addWaypoint(waypoint: Waypoint)

    /**
     * Get all waypoints across all tracks.
     *
     * @return List of Waypoint domain models.
     */
    suspend fun getWaypoints(): List<Waypoint>

    /**
     * Get all waypoints for a specific track.
     *
     * @param trackId ID of the track.
     * @return List of Waypoint domain models.
     */
    suspend fun getTrackWaypoints(trackId: Int): List<Waypoint>

    /**
     * Insert a new track into the database.
     *
     * @param track TrackEntity object to insert.
     * @return The ID (primary key) of the newly inserted track.
     */
    suspend fun insertTrack(track: TrackEntity): Long

    /**
     * Delete all tracks and waypoints from the database.
     */
    suspend fun clearAll()
}