package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUI

class MovingAverageUI : FilterUI() {

    override fun createView(context: Context, waypointCount: Int, onValueChange: (FilterType) -> Unit): View {

        val default = FilterType.MOVING_AVERAGE().window
        val sliderDescription = createTextView(context)
        sliderDescription.text = "Window size: ${default} points (larger = smoother, smaller = more detailed)"

        val slider = Slider(context).apply {
            valueFrom = 2f
            valueTo = 100f
            stepSize = 1f
            value = default.toFloat()
            addOnChangeListener { _, value, _ ->
                onValueChange(FilterType.MOVING_AVERAGE(value.toInt()))
                sliderDescription.text = "Window size: ${value.toInt()} points (larger = smoother, smaller = more detailed)"
            }
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(slider)
            addView(sliderDescription)
        }

        return layout
    }
}