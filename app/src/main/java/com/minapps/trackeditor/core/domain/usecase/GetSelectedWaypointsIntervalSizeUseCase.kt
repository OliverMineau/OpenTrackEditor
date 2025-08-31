package com.minapps.trackeditor.core.domain.usecase
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

class GetSelectedWaypointsIntervalSizeUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke(trackId: Int, p1: Double, p2: Double): Int? {
        if(!repository.getTrackIds().contains(trackId)) return null
        return repository.getIntervalSize(trackId, p1, p2)
    }

    suspend operator fun invoke(trackId: Int): Int? {
        if(!repository.getTrackIds().contains(trackId)) return null
        return repository.getIntervalSize(trackId)
    }
}
