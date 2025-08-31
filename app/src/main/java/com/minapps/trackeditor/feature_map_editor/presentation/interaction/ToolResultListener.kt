package com.minapps.trackeditor.feature_map_editor.presentation.interaction

import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams

interface ToolResultListener {
    fun onFilterApplied(params: FilterParams)
}
