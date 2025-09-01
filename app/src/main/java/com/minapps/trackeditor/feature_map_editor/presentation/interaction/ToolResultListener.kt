package com.minapps.trackeditor.feature_map_editor.presentation.interaction

import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams

interface ToolResultListener {
    fun onToolResult(tool: ActionType, result: Any?)
}
