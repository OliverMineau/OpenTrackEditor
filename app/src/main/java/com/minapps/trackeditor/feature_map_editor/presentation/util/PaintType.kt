package com.minapps.trackeditor.feature_map_editor.presentation.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathDashPathEffect

/**
 * Paint style for polylines
 *
 */
enum class PaintType {
    SOLID,
    DASHED,
    DOTTED;

    fun applyTo(paint: Paint, color: Int = Color.RED) {

        paint.color = color

        when (this) {
            SOLID -> {
                paint.pathEffect = null
            }

            DASHED -> {
                val dashLength = 20f
                val gapLength = 10f
                paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
            }

            DOTTED -> {
                val dot = Path().apply {
                    addCircle(0f, 0f, 5f, Path.Direction.CW)
                }
                paint.pathEffect = PathDashPathEffect(dot, 20f, 0f, PathDashPathEffect.Style.ROTATE)
            }
        }
    }
}
