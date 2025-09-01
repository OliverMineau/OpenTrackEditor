package com.minapps.trackeditor.feature_map_editor.tools.delete

import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.domain.usecase.DeleteSelectedUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import jakarta.inject.Inject

class DeleteTool @Inject constructor(
    val deleteSelectedUseCase: DeleteSelectedUseCase,
) : EditorTool{
    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {
        val editState = uiContext.getEditState()

        val update = deleteSelectedUseCase(
            editState.currentSelectedTracks,
            editState.currentSelectedPoints
        )

        listener.onToolResult(ActionType.DELETE, update)
    }
}