package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.SimpleWaypoint
import jakarta.inject.Inject

class AddWaypointToSelectedTrackUseCase @Inject constructor(
    private val addWaypointUseCase: AddWaypointUseCase,
    private val createTrackUseCase: CreateTrackUseCase
) {
    suspend operator fun invoke(
        selectedTrackIds: List<Int>,
        lat: Double,
        lng: Double,
        position: AddWaypointUseCase.InsertPosition,
        trackNameProvider: () -> String
    ): Pair<Int, SimpleWaypoint> {

        // Get trackId
        var trackId = selectedTrackIds.firstOrNull()

        // If tracks first waypoint create new track
        if (trackId == null) {
            trackId = createTrackUseCase(trackNameProvider())
        }

        val newId = addWaypointUseCase.getNextId(trackId, position)
        addWaypointUseCase(lat, lng, newId, trackId, position)
        return trackId to SimpleWaypoint(newId, lat, lng)
    }
}
