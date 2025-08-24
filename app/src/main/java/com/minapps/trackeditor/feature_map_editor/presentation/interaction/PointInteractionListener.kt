package com.minapps.trackeditor.feature_map_editor.presentation.interaction

import com.minapps.trackeditor.feature_map_editor.presentation.MovingPointBundle

interface PointInteractionListener {
    fun onPointMoved(selectedBundle: MovingPointBundle)
    fun onPointClicked(selectedBundle: MovingPointBundle)

}