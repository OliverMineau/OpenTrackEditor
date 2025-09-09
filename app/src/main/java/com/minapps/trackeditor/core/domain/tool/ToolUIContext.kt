package com.minapps.trackeditor.core.domain.tool

import com.minapps.trackeditor.feature_map_editor.domain.model.EditState

/**
 * Class to give Tools access to UI
 *
 */
interface ToolUiContext {
    suspend fun <T : Any> showDialog(dialog: ToolDialog<T>): T?
    fun showToast(message: String)
    fun getEditState(): EditState
}
