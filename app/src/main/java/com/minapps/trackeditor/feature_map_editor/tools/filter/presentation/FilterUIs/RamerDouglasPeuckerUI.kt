package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUI

class RamerDouglasPeuckerUI : FilterUI() {

    override fun createView(context: Context, waypointCount: Int, onValueChange: (FilterType) -> Unit): View {

        val defaultTolerance = FilterType.RAMER_DOUGLAS_PEUCKER().tolerance
        val sliderDescription = createTextView(context)
        sliderDescription.text = "Tolerance:  $defaultTolerance meters (smaller keeps more points, larger removes more)"

        val slider = Slider(context).apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 1f
            value = defaultTolerance.toFloat()
            addOnChangeListener { _, value, _ ->
                onValueChange(FilterType.RAMER_DOUGLAS_PEUCKER(value.toInt()))
                sliderDescription.text = "Tolerance:  ${value.toInt()} meters (smaller keeps more points, larger removes more)"
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