package com.minapps.trackeditor.feature_map_editor.tools.filter

import android.util.Log
import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.usecase.GetSelectedWaypointsIntervalSizeUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.MapViewModel
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase.ApplyFilterUseCase
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterDialog
import jakarta.inject.Inject

class FilterTool @Inject constructor (
    private val applyFilterUseCase: ApplyFilterUseCase,
    private val getSelectedWaypointsIntervalSizeUseCase: GetSelectedWaypointsIntervalSizeUseCase,
) : EditorTool {

    /**
     * Execute tool
     * Displays popup then applies chosen filter on track
     *
     * @param uiContext
     */
    override suspend fun launch(listener: ToolResultListener, uiContext: ToolUiContext, isSelected: Boolean) {

        val editState =  uiContext.getEditState()

        val hasSelectedTracks = editState.currentSelectedTracks.isEmpty()
        val hasSelectedPoints = editState.currentSelectedPoints.size == 2

        // If No track selected
        if(hasSelectedTracks){
            uiContext.showToast("Select track or segment to filter")
            listener.onFilterApplied(FilterParams(null, false))
            return
        }

        // If points selected check if on same track
        var point1: Pair<Int, Double>
        var point2: Pair<Int, Double>
        val waypointCount: Int?

        if(hasSelectedPoints){
            point1 = editState.currentSelectedPoints[0]
            point2 = editState.currentSelectedPoints[1]
            if(point1.first != point2.first){
                uiContext.showToast("Points must belong to same track")
                listener.onFilterApplied(FilterParams(null, false))
                return
            }
            waypointCount = getSelectedWaypointsIntervalSizeUseCase(point1.first, point1.second, point2.second)
        } else{
            waypointCount = getSelectedWaypointsIntervalSizeUseCase(editState.currentSelectedTracks.first())
        }

        if(waypointCount == null){
            uiContext.showToast("No waypoints selected")
            listener.onFilterApplied(FilterParams(null, false))
            return
        }

        // Ask the user for parameters
        val params = uiContext.showDialog(FilterDialog(waypointCount))
        if (params != null) {

            // Apply filtering logic
            val filtered = applyFilterUseCase(params)
            // Provide feedback to the user
            uiContext.showToast("${params.filterType?.label} filtering ${if(filtered) "applied" else "failed"}")

            listener.onFilterApplied(params)
            return
        }

        listener.onFilterApplied(FilterParams(null, false))
    }
}