package com.minapps.trackeditor.core.domain.model

import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.type.InsertPosition

/**
 * Edition state
 *
 * @property currentSelectedTool
 * @property currentSelectedTracks
 * @property currentSelectedPoints
 * @property direction
 * @property version
 */
data class EditState(
    val currentSelectedTool: ActionType = ActionType.NONE,
    val currentSelectedTracks: MutableList<Int> = mutableListOf(),
    val currentSelectedPoints: MutableList<Pair<Int, Double>> = mutableListOf(),
    val direction: InsertPosition = InsertPosition.BACK,
    val version: Long = System.nanoTime(),
)