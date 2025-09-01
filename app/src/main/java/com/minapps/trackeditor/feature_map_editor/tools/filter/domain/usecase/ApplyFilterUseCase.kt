package com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterResult
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterSelection
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import jakarta.inject.Inject

class ApplyFilterUseCase @Inject constructor(
    private val ramerDouglasPeuckerUseCase: RamerDouglasPeuckerUseCase,
    private val evenIntervalDecimationUseCase: EvenIntervalDecimationUseCase,
) {

    // TODO Add remaining filters
    suspend operator fun invoke(selection : FilterSelection, params: FilterParams): FilterResult? {
        return when (params.filterType) {
            is FilterType.DISTANCE_BASED -> return null
            is FilterType.EVEN_INTERVAL_DECIMATION -> evenIntervalDecimationUseCase(selection, params)
            is FilterType.KALMAN -> return null
            is FilterType.MOVING_AVERAGE -> return null
            is FilterType.RAMER_DOUGLAS_PEUCKER -> return null
            null -> return null
        }
    }
}