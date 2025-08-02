package com.minapps.trackeditor.data.repository

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import com.minapps.trackeditor.data.local.WaypointDao
import com.minapps.trackeditor.data.mapper.toDomain
import com.minapps.trackeditor.data.mapper.toEntity
import jakarta.inject.Inject

class EditTrackRepositoryImpl @Inject constructor(
    private val dao: WaypointDao
) : EditTrackRepositoryItf {

    override suspend fun addWaypoint(waypoint: Waypoint) {
        val all = dao.getAllWaypoints()
        val order = all.size // append at end
        dao.insertWaypoint(waypoint.toEntity(order))

        Log.d("database", "Waypoints : ${dao.getAllWaypoints()}")
    }

    override suspend fun getWaypoints(): List<Waypoint> {
        return dao.getAllWaypoints().map { it.toDomain() }
    }

    override suspend fun clearAll() {
        dao.clearAll()
        Log.d("database", "Cleared all waypoints")
    }
}