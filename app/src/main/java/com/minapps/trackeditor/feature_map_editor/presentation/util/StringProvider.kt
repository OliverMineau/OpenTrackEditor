package com.minapps.trackeditor.feature_map_editor.presentation.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

class StringProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getString(resId: Int) = context.getString(resId)
}