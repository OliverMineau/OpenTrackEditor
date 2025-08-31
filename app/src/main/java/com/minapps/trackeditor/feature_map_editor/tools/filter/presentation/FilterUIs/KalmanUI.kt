package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUI

class KalmanUI : FilterUI() {

    override fun createView(
        context: Context,
        waypointCount: Int,
        onValueChange: (FilterType) -> Unit
    ): View {


        val defaultProcessVal = FilterType.KALMAN().processNoise
        val defaultMeasurementVal = FilterType.KALMAN().measurementNoise

        var process = defaultProcessVal.toFloat()
        var measurement = defaultMeasurementVal

        val processNoiseDescription = createTextView(context)
        processNoiseDescription.text = "Process noise: $defaultProcessVal (lower = smoother, higher = more responsive)"

        val measurementNoiseDescription = createTextView(context)
        measurementNoiseDescription.text = "Measurement noise: $defaultMeasurementVal meters (expected GPS error)"


        val processNoiseSlider = Slider(context).apply {
            valueFrom = 0.01f
            valueTo = 10f
            stepSize = 0.01f
            value = defaultProcessVal.toFloat()
            addOnChangeListener { _, value, _ ->
                onValueChange(FilterType.KALMAN(value, measurement))
                val formatted = String.format("%.2f", value)
                processNoiseDescription.text = "Process noise: $formatted. (lower = smoother, higher = more responsive)"
            }
        }

        val measurementNoiseSlider = Slider(context).apply {
            valueFrom = 1f
            valueTo = 50f
            stepSize = 1f
            value = defaultMeasurementVal.toFloat()
            addOnChangeListener { _, value, _ ->
                onValueChange(FilterType.KALMAN(process,value.toInt()))
                measurementNoiseDescription.text = "Measurement noise: $value meters (expected GPS error)"
            }
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(processNoiseSlider)
            addView(processNoiseDescription)
            addView(measurementNoiseSlider)
            addView(measurementNoiseDescription)
        }

        return layout
    }
}