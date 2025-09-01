package com.minapps.trackeditor.feature_map_editor.tools.join

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
        val result = joinTracksUseCase(uiContext.getEditState().currentSelectedPoints)
        listener.onToolResult(ActionType.JOIN,result)
    }

}