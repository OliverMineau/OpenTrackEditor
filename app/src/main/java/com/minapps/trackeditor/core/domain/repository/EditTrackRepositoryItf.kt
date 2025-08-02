package com.minapps.trackeditor.core.domain.repository

import com.minapps.trackeditor.core.domain.model.Waypoint

interface EditTrackRepositoryItf {
    suspend fun addWaypoint(waypoint: Waypoint)
    suspend fun getWaypoints(): List<Waypoint>
    suspend fun clearAll()
}