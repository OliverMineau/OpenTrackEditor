package com.minapps.trackeditor.feature_map_editor.tools.export

import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import jakarta.inject.Inject

class ExportTool @Inject constructor(): EditorTool {

    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {
        listener.onToolResult(ActionType.EXPORT, null)
    }
}