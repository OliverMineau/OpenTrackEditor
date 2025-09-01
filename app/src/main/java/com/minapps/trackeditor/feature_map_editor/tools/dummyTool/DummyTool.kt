package com.minapps.trackeditor.feature_map_editor.tools.dummyTool

import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.usecase.DummyUseCase
import jakarta.inject.Inject

class DummyTool @Inject constructor(
    val dummyUseCase: DummyUseCase,
) : EditorTool{

    override suspend fun launch(
        listener: ToolResultListener,
        uiContext: ToolUiContext,
        isSelected: Boolean
    ) {

        val result = dummyUseCase
        listener.onToolResult(ActionType.NONE, result)
    }
}