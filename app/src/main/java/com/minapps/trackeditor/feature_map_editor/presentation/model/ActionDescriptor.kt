package com.minapps.trackeditor.feature_map_editor.presentation.model

import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.util.SelectionCount
import com.minapps.trackeditor.core.domain.util.ToolGroup

data class ActionDescriptor(
    val icon: Int?,
    val label: String?,
    val action: UIAction?,
    val executor: EditorTool?,
    val selectionCount: SelectionCount?,
    val type: ActionType,
    val group: ToolGroup?
)