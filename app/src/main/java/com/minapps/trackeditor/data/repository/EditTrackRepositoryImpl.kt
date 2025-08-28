package com.minapps.trackeditor.data.repository

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
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
) : EditTrackRepository {

    private val _addedTracks = MutableSharedFlow<Pair<Int, Boolean>>()
    override val addedTracks: Flow<Pair<Int, Boolean>> = _addedTracks

    /**
     * Add a waypoint to the database.
     * Converts the domain model Waypoint to a database entity and inserts it.
     *
     * @param waypoint The domain Waypoint to add
     */
    override suspend fun addWaypoint(waypoint: Waypoint, updateUI: Boolean) {
        dao.insertWaypointWithTrackCheck(waypoint.toEntity(), )
        Log.d("debug", "Added to track: ${waypoint.trackId}, ${waypoint.id}")

        if(updateUI){
            _addedTracks.emit(Pair(waypoint.trackId,false))
        }
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

    override suspend fun getTrackIds(): List<Int>{
        return dao.getTrackIds()
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

    override suspend fun getTrackWaypointsChunk(trackId: Int, chunkSize: Int, offset: Int): List<Waypoint>{
        return dao.getWaypointsByChunk(trackId, chunkSize, offset).map { it.toDomain() }
    }


    override suspend fun getTrackWaypointsSample(trackId: Int, sampleRate: Int): List<Waypoint>{
        return dao.getTrackWaypointsSample(trackId, sampleRate).map { it.toDomain() }
    }



    override suspend fun getTrackFirstWaypointIndex(trackId: Int): Double? {
        return dao.getTrackFirstWaypointIndex(trackId)
    }

    override suspend fun getTrackLastWaypointIndex(trackId: Int): Double? {
        return dao.getTrackLastWaypointIndex(trackId)
    }

    override suspend fun getWaypointIndex(trackId: Int, id: Double): Int?{
        return dao.getWaypointIndex(trackId, id)
    }

    override suspend fun getWaypoint(trackId: Int, index: Int): Waypoint?{
        return dao.getWaypoint(trackId, index)?.toDomain()
    }


    override suspend fun getVisibleTrackWaypointsCount(trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): Double{
        return dao.getVisibleTrackWaypointsCount(trackId, latNorth, latSouth, lonWest, lonEast)
    }

    override suspend fun getVisibleTrackWaypoints(trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double): List<Waypoint>{
        return dao.getVisibleTrackWaypoints(trackId, latNorth, latSouth, lonWest, lonEast).map { it.toDomain() }
    }

    override suspend fun getVisibleTrackWaypointsChunk(trackId: Int, latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double, chunkSize: Int, offset: Int): List<Waypoint>{
        return dao.getVisibleTrackWaypointsChunk(trackId, latNorth, latSouth, lonWest, lonEast, chunkSize, offset).map { it.toDomain() }
    }

    override suspend fun getTracksWithVisibleWaypoints(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): List<Pair<Int, List<Waypoint>>> {
        val waypoints = dao.getTracksWithVisibleWaypoints(latNorth, latSouth, lonWest, lonEast)
        return waypoints
            .map { it.toDomain() }
            .groupBy { it.trackId }  // group domain waypoints by track
            .toList()
    }

    override suspend fun getTracksWithVisibleWaypointsCount(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): Double {
        val count = dao.getTracksWithVisibleWaypointsCount(latNorth, latSouth, lonWest, lonEast)
        return count
    }

    override suspend fun getTrackIdsWithVisibleWaypoints(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double
    ): List<Int> {
        val ids = dao.getTrackIdsWithVisibleWaypoints(latNorth, latSouth, lonWest, lonEast)
        return ids
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
    override suspend fun addImportedTrack(trackId: Int, center: Boolean) : Boolean{
        //TODO DEBUG purposes : No need of track in the future
        // only get number of points proportional to zoom

        //var track = dao.getTrackById(trackId)?.toDomain(dao.getTrackWaypoints(trackId))

        /*if(track == null){
            return false
        }*/

        // Emit track for MapViewModel
        //_addedTracks.emit(track)
        _addedTracks.emit(Pair(trackId, center))
        return true
    }

}