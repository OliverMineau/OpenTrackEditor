package com.minapps.trackeditor.core.domain.tool

import com.minapps.trackeditor.feature_map_editor.domain.model.EditState

/**
 * TODO
 *
 */
interface ToolUiContext {
    suspend fun <T : Any> showDialog(dialog: ToolDialog<T>): T?
    fun showToast(message: String)
    fun getEditState(): EditState
}
