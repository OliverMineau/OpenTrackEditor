package com.minapps.trackeditor.feature_map_editor.tools.layer

import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase.ApplyFilterUseCase
import com.minapps.trackeditor.feature_map_editor.tools.layer.presentation.LayerDialog
import jakarta.inject.Inject


class LayerTool @Inject constructor(
) : EditorTool {

    /**
     * Execute tool
     * Displays popup then applies chosen filter on track
     *
     * @param uiContext
     */
    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {

        val editState = uiContext.getEditState()

        // Ask the user for parameters
        val params = uiContext.showDialog(LayerDialog(editState.layerType))
        if (params != null) {

            // Send back results
            listener.onToolResult(ActionType.LAYERS, params.type)
            return
        }
    }
}