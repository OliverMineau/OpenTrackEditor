package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUI

class EvenIntervalDecimation : FilterUI() {

    override fun createView(
        context: Context,
        waypointCount: Int,
        onValueChange: (FilterType) -> Unit
    ): View {

        val sliderDescription = createTextView(context)
        sliderDescription.text = "Keep ${waypointCount / 2} of $waypointCount waypoints (Remove ${waypointCount / 2})"

        val slider = Slider(context).apply {
            valueFrom = 0f
            valueTo = waypointCount.toFloat()
            stepSize = 1f
            value = (waypointCount / 2).toFloat()
            addOnChangeListener { _, value, _ ->
                onValueChange(FilterType.EVEN_INTERVAL_DECIMATION(value.toInt()))
                sliderDescription.text = "Keep ${value.toInt()} of $waypointCount waypoints (Remove ${waypointCount-value.toInt()})"
            }
        }

        // Send default value when selected
        onValueChange(FilterType.EVEN_INTERVAL_DECIMATION((waypointCount / 2)))

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(slider)
            addView(sliderDescription)
        }

        return layout
    }
}