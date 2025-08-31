package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUI

class DistanceBasedUI : FilterUI() {

    override fun createView(context: Context, waypointCount: Int, onValueChange: (FilterType) -> Unit): View {

        val defaultDistanceVal = FilterType.DISTANCE_BASED().distance

        val sliderDescription = createTextView(context)
        sliderDescription.text = "Min. distance: ${defaultDistanceVal} meters (points closer than this will be removed)"

        val slider = Slider(context).apply {
            valueFrom = 0f
            valueTo = 1000f
            stepSize = 1f
            value = defaultDistanceVal.toFloat()
            addOnChangeListener { _, value, _ ->
                onValueChange(FilterType.DISTANCE_BASED(value.toInt()))
                sliderDescription.text = "Min. distance: ${value.toInt()} meters (points closer than this will be removed)"
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