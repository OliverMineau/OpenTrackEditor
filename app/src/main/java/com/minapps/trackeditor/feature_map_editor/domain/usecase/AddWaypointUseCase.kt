package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import jakarta.inject.Inject

class AddWaypointUseCase @Inject constructor(
    private val repository: EditTrackRepositoryItf
) {
    suspend operator fun invoke(lat: Double, lng: Double) {
        val waypoint = Waypoint(
            id = System.currentTimeMillis().toString(),
            lat = lat,
            lng = lng
        )
        repository.addWaypoint(waypoint)
    }
}