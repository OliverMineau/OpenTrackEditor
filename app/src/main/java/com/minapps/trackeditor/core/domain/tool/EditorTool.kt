package com.minapps.trackeditor.core.domain.tool

import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener

/**
 * Interface that every tool should implement
 *
 */
interface EditorTool {
    suspend fun launch(listener: ToolResultListener, uiContext: ToolUiContext, isSelected: Boolean)
}
