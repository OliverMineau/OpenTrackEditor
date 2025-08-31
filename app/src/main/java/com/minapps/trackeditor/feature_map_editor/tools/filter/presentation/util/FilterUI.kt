package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util

import android.content.Context
import android.view.View
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType

abstract class FilterUI {
    abstract fun createView(context: Context, waypointCount: Int, onValueChange: (FilterType) -> Unit): View
    protected fun createTextView(context: Context) = FilterUIUtils.createTextView(context)
    protected fun createRoundedSpinner(context: Context, items: List<String>) = FilterUIUtils.createRoundedSpinner(context, items)
}
