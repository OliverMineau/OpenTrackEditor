package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

/**
 * Use case responsible for adding a new waypoint to a specific track.
 *
 * This encapsulates the logic required to create a [Waypoint] and delegate
 * the persistence to the [EditTrackRepository].
 *
 * @property repository Repository used to add the waypoint to the data source.
 */
class AddWaypointUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    /**
     * Creates and adds a new waypoint to the given track.
     *
     * @param lat The latitude coordinate of the waypoint.
     * @param lng The longitude coordinate of the waypoint.
     * @param idx The index of the waypoint within the track.
     * @param trackId The ID of the track to which this waypoint will be added.
     */
    suspend operator fun invoke(lat: Double, lng: Double, idx: Double, trackId: Int) {
        val waypoint = Waypoint(
            id = idx,
            lat = lat,
            lng = lng,
            elv = null,
            time = "",
            trackId = trackId,
        )
        repository.addWaypoint(waypoint)
    }

}