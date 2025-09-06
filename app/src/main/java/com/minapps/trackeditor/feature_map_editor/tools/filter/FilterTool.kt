package com.minapps.trackeditor.feature_map_editor.tools.filter


import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.usecase.GetSelectedWaypointsIntervalSizeUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterResult
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterSelection
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

        val hasSelectedTracks = editState.currentSelectedTracks.size != 1
        val hasSelectedPoints = editState.currentSelectedPoints.size == 2

        var trackId: Int?
        var pointA: Double? = null
        var pointB: Double? = null

        // If No track selected
        if(hasSelectedTracks){
            uiContext.showToast("Select one track or segment to filter")
            listener.onToolResult(ActionType.FILTER,FilterResult(false, listOf()))
            return
        }
        trackId = editState.currentSelectedTracks.first()

        // If points selected check if on same track
        var point1: Pair<Int, Double>
        var point2: Pair<Int, Double>
        var waypointCount: Int?

        // Check selected points
        if(hasSelectedPoints){
            point1 = editState.currentSelectedPoints[0]
            point2 = editState.currentSelectedPoints[1]
            if(point1.first != point2.first){
                uiContext.showToast("Points must belong to same track")
                listener.onToolResult(ActionType.FILTER,FilterResult(false, listOf()))
                return
            }

            if(point1.second > point2.second){
                pointA = point2.second
                pointB = point1.second
            }else{
                pointA = point1.second
                pointB = point2.second
            }

            waypointCount = getSelectedWaypointsIntervalSizeUseCase(point1.first, pointA, pointB)

        } else{
            waypointCount = getSelectedWaypointsIntervalSizeUseCase(editState.currentSelectedTracks.first())
        }

        if(waypointCount == null){
            uiContext.showToast("No waypoints selected")
            listener.onToolResult(ActionType.FILTER, FilterResult(false, listOf()))
            return
        }

        // Remove end waypoints to count
        waypointCount -= 2

        if(waypointCount<=0){
            uiContext.showToast("Not enough waypoints")
            listener.onToolResult(ActionType.FILTER,FilterResult(false, listOf()))
            return
        }

        // Ask the user for parameters
        val params = uiContext.showDialog(FilterDialog(waypointCount))
        if (params != null) {

            // Apply filtering logic
            val selection = FilterSelection(trackId, pointA, pointB)
            val result = applyFilterUseCase(selection,params)

            // Provide feedback to the user
            uiContext.showToast("${params.filterType?.label} filtering ${if(result?.succeeded == true) "applied" else "failed"}")

            // Send back results
            listener.onToolResult(ActionType.FILTER, result)
            return
        }

        listener.onToolResult(ActionType.FILTER, FilterResult(false, listOf()))
    }
}