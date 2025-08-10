package com.minapps.trackeditor.data.repository

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.local.TrackEntity
import com.minapps.trackeditor.data.mapper.toDomain
import com.minapps.trackeditor.data.mapper.toEntity
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Implementation of the EditTrackRepository interface.
 * This repository handles data operations related to tracks and waypoints,
 * using the TrackDao to access the local database.
 *
 * The class is injectable via Dagger/Hilt with @Inject constructor.
 */
class EditTrackRepositoryImpl @Inject constructor(
    private val dao: TrackDao
) : EditTrackRepositoryItf {

    private val _addedTracks = MutableSharedFlow<Track>()
    override val addedTracks: Flow<Track> = _addedTracks

    /**
     * Add a waypoint to the database.
     * Converts the domain model Waypoint to a database entity and inserts it.
     *
     * @param waypoint The domain Waypoint to add
     */
    override suspend fun addWaypoint(waypoint: Waypoint) {
        dao.insertWaypointWithTrackCheck(waypoint.toEntity(), )
        Log.d("debug", "Added to track: ${waypoint.trackId}, ${waypoint.id}")
    }

    /**
     * Add a list of waypoints to the database.
     * Converts the domain model Waypoint to a database entity and inserts it.
     *
     * @param waypoints The domain Waypoint to add
     */
    override suspend fun addWaypoints(waypoints: List<Waypoint>) {
        //TODO
        dao.insertWaypoints(waypoints.map { it.toEntity() })
        Log.d("debug", "Added ${waypoints.size} waypoints")
    }

    /**
     * Retrieve all waypoints from the database.
     * Converts database entities back to domain models.
     *
     * @return List of Waypoint domain objects
     */
    override suspend fun getWaypoints(): List<Waypoint> {
        return dao.getAllWaypoints().map { it.toDomain() }
    }

    /**
     * Retrieve all waypoints belonging to a specific track.
     *
     * @param trackId The ID of the track
     * @return List of Waypoint domain objects associated with the track
     */
    override suspend fun getTrackWaypoints(trackId: Int): List<Waypoint> {
        return dao.getTrackWaypoints(trackId).map { it.toDomain() }
    }


    override suspend fun getTrackLastWaypointIndex(trackId: Int): Double {
        return dao.getTrackLastWaypointIndex(trackId)
    }

    /**
     * Insert a new Track entity into the database.
     *
     * @param track The TrackEntity to insert
     * @return The row ID of the newly inserted track
     */
    override suspend fun insertTrack(track: TrackEntity): Long {
        return dao.insertTrack(track)
    }

    override suspend fun getFullTrack(trackId: Int): Track? {
        return dao.getTrackById(trackId)?.toDomain( dao.getTrackWaypoints(trackId))
    }

    /**
     * Clear all tracks and waypoints from the database.
     */
    override suspend fun clearAll() {
        dao.clearAll()
        Log.d("debug", "Cleared all waypoints")
    }

    /**
     * Add an imported track to the database.
     *
     * @param track The domain track to add
     * @return Track
     */
    override suspend fun addImportedTrack(trackId: Int) : Boolean{
        //TODO DEBUG purposes : No need of track now in the future


        var track = dao.getTrackById(trackId)?.toDomain(dao.getTrackWaypoints(trackId))

        if(track == null){
            return false
        }
        // Emit track for MapViewModel
        _addedTracks.emit(track)
        return true
    }

}