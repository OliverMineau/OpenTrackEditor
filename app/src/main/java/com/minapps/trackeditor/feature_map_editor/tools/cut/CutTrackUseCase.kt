package com.minapps.trackeditor.feature_map_editor.tools.cut

import android.util.Log
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.type.EndType
import com.minapps.trackeditor.feature_map_editor.domain.model.SimpleWaypoint
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import jakarta.inject.Inject
import kotlin.collections.get
import kotlin.math.max
import kotlin.math.min


class CutTrackUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke(
        selectedPoints: List<Pair<Int, Double>>,
    ): WaypointUpdate? {

        var ids: List<Int>? = null

        // If one point selected
        if(selectedPoints.size == 1){
            ids = repository.splitTrack(selectedPoints.first().first, selectedPoints.first().second)
        }

        // If 2 points selected
        /*else if(selectedPoints.size == 2){
            var a = min(selectedPoints.first().second, selectedPoints.last().second)
            var b = max(selectedPoints.first().second, selectedPoints.last().second)
            ids = repository.splitTrackRange(selectedPoints.first().first, a, b)
        }*/

        if(ids == null || ids.isEmpty()) return null

        return WaypointUpdate.SplitTrack(ids)
    }

}
