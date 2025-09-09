package com.minapps.trackeditor.feature_map_editor.tools.dummyTool

import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.usecase.DummyUseCase
import com.minapps.trackeditor.feature_map_editor.tools.dummyTool.presentation.DummyDialog
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterDialog
import jakarta.inject.Inject

/**
 * Main tool class
 *
 * @property dummyUseCase
 */
class DummyTool @Inject constructor(
    val dummyUseCase: DummyUseCase,
) : EditorTool{

    /**
     * Executed when the tool is clicked
     *
     * @param listener
     * @param uiContext
     * @param isSelected
     */
    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {

        // Get the current selected points/tracks
        val dummyData = uiContext.getEditState()

        // Show the tool ui to get user input
        val selectedParameters = uiContext.showDialog(DummyDialog(dummyData))

        if(selectedParameters != null){

            // Execute tool logic
            val result = dummyUseCase(selectedParameters)

            // Send tool result
            listener.onToolResult(ActionType.NONE, result)
        }

    }
}