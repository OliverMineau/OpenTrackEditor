package com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.model.DummyParams
import com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.model.DummyResult
import jakarta.inject.Inject

/**
 * Use case for the tool logic
 *
 * @property repository
 */
class DummyUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    operator fun invoke(dummySelection: DummyParams) : DummyResult {

        val succeeded = true
        val trackId = 0
        val updateUIMethod = WaypointUpdate.Cleared(trackId)

        // Return if tool succeeded and how what the ui has to update
        return DummyResult(succeeded, updateUIMethod)
    }
}