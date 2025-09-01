package com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterResult
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterSelection
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import jakarta.inject.Inject

class EvenIntervalDecimationUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke(selection: FilterSelection, params: FilterParams): FilterResult {
        val parameters = params.filterType as FilterType.EVEN_INTERVAL_DECIMATION

        var pointCount: Int
        if (selection.pointA == null || selection.pointB == null) {
            pointCount = repository.getIntervalSize(selection.trackId)
        } else {
            pointCount =
                repository.getIntervalSize(selection.trackId, selection.pointA, selection.pointB)
        }

        var totalWaypoints = parameters.waypoint
        if(totalWaypoints == 0){
            totalWaypoints = 1
        }

        val step = pointCount / totalWaypoints

        if (selection.pointA == null || selection.pointB == null) {
            repository.removeWaypointsByStep(selection.trackId, step)
        } else {
            repository.removeWaypointsByStep(
                selection.trackId,
                step,
                selection.pointA,
                selection.pointB
            )
        }

        return FilterResult(true, listOf(WaypointUpdate.FilteredTrack(selection.trackId)))
    }
}