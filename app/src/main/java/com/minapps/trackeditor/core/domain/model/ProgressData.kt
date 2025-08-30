package com.minapps.trackeditor.core.domain.model

/**
 * Progress bundle
 *
 * @property progress
 * @property isDisplayed
 * @property message
 */
data class ProgressData(
    val progress: Int = 0,
    val isDisplayed: Boolean = false,
    val message: String? = null,
)