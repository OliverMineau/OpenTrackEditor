package com.minapps.trackeditor.core.domain.util

import android.util.Log
import javax.inject.Inject

data class MapDecision(
    val showOutline: Boolean,
    val showFull: Boolean,
    val snappedZoom: Int
)

class MapUpdateViewHelper @Inject constructor() {

    private val zoomStep: Int = 3 // TODO to be determined
    private val pointThreshold: Int = 30000 // TODO to be determined

    /**
     * Decide to load precise track (full) or only partial (outline)
     *
     * @param zoom Zoom intensity
     * @param waypointCount Number of potential visible waypoints
     * @param lastZoom Last zoom intensity
     * @param hasDisplayedFull If last update was a full update
     * @param hasDisplayedOutline If last update was a outline update
     * @return MapDecision( If outline is shown, If full track is shown, new zoom intensity)
     */
    fun decide(zoom: Double, waypointCount: Double, lastZoom: Int?, hasDisplayedFull: Boolean, hasDisplayedOutline: Boolean): MapDecision {

        // Create zoom intervals to only update every other step
        val snappedZoom = (zoom.toInt() / zoomStep) * zoomStep
        val hasChangedZoom = lastZoom == null || lastZoom != snappedZoom

        val tooManyPoints = waypointCount >= pointThreshold

        // Check to see if we want to show outline
        val showOutline = (tooManyPoints && hasChangedZoom) ||
                (tooManyPoints && hasDisplayedFull) ||
                (tooManyPoints && !hasDisplayedFull && !hasDisplayedOutline)

        // Check to see if we want to show full detail
        val showFull = !tooManyPoints && !showOutline

        Log.d("debugOpti", "hasChangedZoom: $hasChangedZoom")
        Log.d("debugOpti", "tooManyPoints: $tooManyPoints")
        Log.d("debugOpti", "pts count: $waypointCount")
        Log.d("debugOpti", "showOutline: $showOutline")
        Log.d("debugOpti", "showFull: $showFull")

        return MapDecision(showOutline, showFull, snappedZoom)
    }
}
