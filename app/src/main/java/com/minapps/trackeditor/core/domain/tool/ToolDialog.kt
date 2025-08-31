package com.minapps.trackeditor.core.domain.tool

interface ToolDialog<T : Any> {
    val title: String
    suspend fun show(context: Any): T?
}
