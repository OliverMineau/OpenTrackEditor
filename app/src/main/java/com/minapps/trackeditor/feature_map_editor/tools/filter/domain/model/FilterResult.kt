package com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model

import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate

data class FilterResult(
    var succeeded: Boolean = false,
    var update: List<WaypointUpdate>,
)
