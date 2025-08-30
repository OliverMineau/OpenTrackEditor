package com.minapps.trackeditor.core.domain.model

/**
 * Edit state meant for UI
 *
 * @property selectedTrackIds
 * @property selectedPoints
 */
data class UiMapState(
    val selectedTrackIds: MutableList<Int>,
    val selectedPoints: MutableList<Pair<Int, Double>>,
)