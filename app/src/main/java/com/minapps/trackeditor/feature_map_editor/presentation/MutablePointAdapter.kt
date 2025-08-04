package com.minapps.trackeditor.feature_map_editor.presentation

import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay

class MutablePointAdapter(
    private val points: MutableList<IGeoPoint>
) : SimpleFastPointOverlay.PointAdapter {

    override fun get(index: Int): IGeoPoint = points[index]

    override fun size(): Int = points.size

    override fun iterator(): MutableIterator<IGeoPoint> = points.iterator()

    override fun isLabelled(): Boolean = points.firstOrNull() is LabelledGeoPoint

    override fun isStyled(): Boolean = false

    fun set(index: Int, point: IGeoPoint) {
        points[index] = point
    }

    fun getAll(): List<IGeoPoint> = points
}
