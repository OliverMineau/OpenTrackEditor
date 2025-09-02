package com.minapps.trackeditor.feature_map_editor.tools.delete

import com.minapps.trackeditor.core.common.util.SelectionType
import com.minapps.trackeditor.core.common.util.SelectionUtil
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.domain.usecase.DeleteSelectedUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import jakarta.inject.Inject

class DeleteTool @Inject constructor(
    val deleteSelectedUseCase: DeleteSelectedUseCase,
) : EditorTool {
    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {
        val editState = uiContext.getEditState()
        val trackIds = editState.currentSelectedTracks
        val points = editState.currentSelectedPoints

        val selectionType = SelectionUtil.getSelectionType(trackIds, points, null, null, true)
        when (selectionType) {
            SelectionType.POINTS_ERROR -> {
                uiContext.showToast("Points must belong to same track")
                return
            }
            SelectionType.TRACK_ERROR -> {
                uiContext.showToast("Select tracks or segments to delete")
                return
            }
            else -> {}
        }
        if (editState.currentSelectedTracks.isEmpty()) {
            uiContext.showToast("Select tracks or segments to delete")
        }

        val update = deleteSelectedUseCase(trackIds,points)

        if(update == null){
            uiContext.showToast("Delete track failed")
            return
        }

        uiContext.showToast("Successfully deleted track(s)")


        listener.onToolResult(ActionType.DELETE, update)
    }
}