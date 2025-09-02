package com.minapps.trackeditor.feature_map_editor.tools.reverse

import com.minapps.trackeditor.core.common.util.SelectionType
import com.minapps.trackeditor.core.common.util.SelectionUtil
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.reverse.model.usecase.ReverseUseCase
import jakarta.inject.Inject

class ReverseTool @Inject constructor(
    private val reverseUseCase: ReverseUseCase,
) : EditorTool {

    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {

        // Get selection
        val trackIds = uiContext.getEditState().currentSelectedTracks
        val points = uiContext.getEditState().currentSelectedPoints

        // Get track or point or error selection
        val selectionType = SelectionUtil.getSelectionType(trackIds, points, 1, null, true)

        when(selectionType){
            SelectionType.TRACK_ONLY , SelectionType.POINTS -> {

                // Order points and call usecase
                val (pointA, pointB) = SelectionUtil.getOrderedPoints(points)
                val result = reverseUseCase(selectionType, trackIds.first(), pointA, pointB)

                // Provide feedback to user
                uiContext.showToast("${if (result != null) "Reversed track successfully" else "Reversing track failed"}")

                // Send results
                listener.onToolResult(ActionType.REVERSE, result)
            }
            SelectionType.POINTS_ERROR -> {
                uiContext.showToast("Points must belong to same track")
            }
            SelectionType.TRACK_ERROR -> {
                uiContext.showToast("Select one track or segment to reverse")
            }
        }
    }

}