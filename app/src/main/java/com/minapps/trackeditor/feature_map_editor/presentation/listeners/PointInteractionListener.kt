package com.minapps.trackeditor.feature_map_editor.presentation.listeners

import com.minapps.trackeditor.feature_map_editor.presentation.MovingPointBundle
import org.osmdroid.api.IGeoPoint

interface PointInteractionListener {
    fun onPointSelected(index: Int)
    fun onPointMoved(selectedBundle: MovingPointBundle)
}