package com.minapps.trackeditor.feature_map_editor.tools.join

import com.minapps.trackeditor.core.common.util.SelectionType
import com.minapps.trackeditor.core.common.util.SelectionUtil
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.usecase.GetSelectedWaypointsIntervalSizeUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase.ApplyFilterUseCase
import jakarta.inject.Inject

class JoinTool @Inject constructor (
    private val joinTracksUseCase: JoinTracksUseCase,
) : EditorTool {

    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {

        val trackIds = uiContext.getEditState().currentSelectedTracks
        val points = uiContext.getEditState().currentSelectedPoints

        val selectionType = SelectionUtil.getSelectionType(trackIds, points, 2, 2, false)
        when(selectionType){
            SelectionType.TRACK_ONLY,
            SelectionType.TRACK_ERROR,
            SelectionType.POINTS_ERROR -> {
                uiContext.showToast("Select two different track endpoints to join")
                return
            }
            else -> {}
        }

        val result = joinTracksUseCase(uiContext.getEditState().currentSelectedPoints)

        if(result == null){
            uiContext.showToast("Select two different track endpoints to join")
            return
        }

        uiContext.showToast("Joined tracks successfully")

        listener.onToolResult(ActionType.JOIN,result)
    }

}