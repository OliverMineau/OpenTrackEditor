package com.minapps.trackeditor.feature_map_editor.tools.layer.domain.model

import android.view.Window

sealed class LayerType(val label: String, val description: String) {

    companion object {
        val entries: List<LayerType> = listOf(
            TERRAIN(),
            SATELLITE(),
            OPENTOPO(),
        )
    }

    data class SATELLITE (
        var tolerance: Int = 10
    ): LayerType(
        "Satellite",
        ""
    )

    data class TERRAIN(
        var distance : Int = 10
    ) : LayerType(
        "Terrain",
        ""
    )

    data class OPENTOPO(
        var distance : Int = 10
    ) : LayerType(
        "Open Topo",
        ""
    )
}
