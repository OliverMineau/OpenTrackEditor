package com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model

import android.view.Window

sealed class FilterType(val label: String, val description: String) {

    companion object {
        val entries: List<FilterType> = listOf(
            RAMER_DOUGLAS_PEUCKER(),
            DISTANCE_BASED(),
            MOVING_AVERAGE(),
            KALMAN(),
            EVEN_INTERVAL_DECIMATION(),
        )
    }

    // Tolerance / Epsilon: A numeric value indicating the maximum deviation allowed between the
    // original track and the simplified track. Smaller values keep more points; larger values
    // remove more points.
    data class RAMER_DOUGLAS_PEUCKER (
        var tolerance: Int = 10
    ): FilterType(
        "Ramer–Douglas–Peucker",
        "Removes points that don’t significantly change the shape of the track."
    )

    // Minimum Distance: A numeric value (meters) specifying the minimum distance between
    // consecutive points to keep them.
    data class DISTANCE_BASED(
        var distance : Int = 10
    ) : FilterType(
        "Distance-Based",
        "Keep only points that are a minimum distance apart."
    )

    // Window Size: Number of points to include in the averaging window. Larger windows give
    // smoother tracks but may distort sharp turns.
    data class MOVING_AVERAGE (
        var window: Int = 5
    ) : FilterType(
        "Moving Average",
        "Smooths coordinates by averaging neighboring points."
    )

    // Process Noise: Numeric value representing uncertainty in movement
    // (how much the filter expects the position to change). Lower values make the track smoother.
    // Measurement Noise: Numeric value representing the expected GPS error.
    data class KALMAN(
        var processNoise: Float = 1f,
        var measurementNoise: Int = 5
    ) : FilterType(
        "Kalman",
        "Advanced filter that predicts and corrects positions based on motion."
    )

    // Number of points to delete / keep
    data class EVEN_INTERVAL_DECIMATION(
        var waypoint: Int = -1
    ) : FilterType
        ("Even Interval Decimation",
        "Removes a fixed number of points evenly along the track to simplify it."
    )
}
