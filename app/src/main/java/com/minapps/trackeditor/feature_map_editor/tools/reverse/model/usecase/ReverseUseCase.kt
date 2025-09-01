package com.minapps.trackeditor.feature_map_editor.tools.reverse.model.usecase

import com.minapps.trackeditor.core.common.util.SelectionType
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import jakarta.inject.Inject

class ReverseUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke(selectionType: SelectionType, trackId: Int, pointA: Double?, pointB: Double?): WaypointUpdate? {

        when(selectionType){
            SelectionType.TRACK_ONLY -> repository.reverseTrack(trackId)
            SelectionType.POINTS -> {
                if(pointA == null || pointB == null) return null
                repository.reverseTrack(trackId, pointA, pointB)
            }
            else -> return null
        }
        return WaypointUpdate.ReversedTrack(trackId)
    }
}