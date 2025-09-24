package com.minapps.trackeditor.feature_map_editor.tools.cut

import com.minapps.trackeditor.core.common.util.SelectionType
import com.minapps.trackeditor.core.common.util.SelectionUtil
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import jakarta.inject.Inject

class CutTool @Inject constructor (
    private val cutTrackUseCase: CutTrackUseCase,
) : EditorTool {

    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {

        val trackIds = uiContext.getEditState().currentSelectedTracks
        val points = uiContext.getEditState().currentSelectedPoints

        val selectionType = SelectionUtil.getSelectionType(trackIds, points, 1, 1, true)
        when(selectionType){
            SelectionType.TRACK_ONLY -> {
                uiContext.showToast("Select a waypoint to split the track")
            }
            SelectionType.TRACK_ERROR -> {
                uiContext.showToast("Select only one track to split")
            }
            SelectionType.POINTS_ERROR -> {
                uiContext.showToast("Select one waypoint to split the track")
                return
            }
            else -> {}
        }

        uiContext.showProgressBar("Splitting Track")
        val result = cutTrackUseCase(uiContext.getEditState().currentSelectedPoints)
        uiContext.hideProgressBar()

        if(result == null) return
        uiContext.showToast("Split track successfully")

        listener.onToolResult(ActionType.CUT,result)
    }

}