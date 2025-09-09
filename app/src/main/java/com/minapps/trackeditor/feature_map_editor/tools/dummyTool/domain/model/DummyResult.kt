package com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.model

import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate

/**
 * Use case returned data
 *
 * @property succeeded
 * @property update
 */
data class DummyResult(
    var succeeded: Boolean = false,
    var update: WaypointUpdate,
)
