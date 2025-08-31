package com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import jakarta.inject.Inject

class ApplyFilterUseCase @Inject constructor(
    private val ramerDouglasPeuckerUseCase: RamerDouglasPeuckerUseCase,
) {
    operator fun invoke(params: FilterParams): Boolean {
        return when (params.filterType) {
            is FilterType.DISTANCE_BASED -> TODO()
            is FilterType.EVEN_INTERVAL_DECIMATION -> TODO()
            is FilterType.KALMAN -> TODO()
            is FilterType.MOVING_AVERAGE -> TODO()
            is FilterType.RAMER_DOUGLAS_PEUCKER -> ramerDouglasPeuckerUseCase()
            null -> return false
        }
    }
}