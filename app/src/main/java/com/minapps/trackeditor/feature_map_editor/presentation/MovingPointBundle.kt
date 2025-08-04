package com.minapps.trackeditor.feature_map_editor.presentation

import org.osmdroid.api.IGeoPoint

data class MovingPointBundle(
    var previousPoint: IGeoPoint? = null,
    var selectedPoint: IGeoPoint? = null,
    var nextPoint: IGeoPoint? = null,
    var movingPos: IGeoPoint? = null,
    var trackId: Int,
) {
    fun clear() {
        previousPoint = null
        selectedPoint = null
        nextPoint = null
        movingPos = null
    }

    fun getPoints(): List<Pair<Double, Double>> {
        return listOfNotNull(
            previousPoint?.let { Pair(it.latitude, it.longitude) },
            movingPos?.let { Pair(it.latitude, it.longitude) },
            nextPoint?.let { Pair(it.latitude, it.longitude) }
        )
    }

    val isInit: Boolean
        get() = previousPoint != null || selectedPoint != null || nextPoint != null || movingPos != null
}
