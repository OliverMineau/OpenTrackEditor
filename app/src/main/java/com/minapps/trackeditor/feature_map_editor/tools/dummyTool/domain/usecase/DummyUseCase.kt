package com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

class DummyUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    operator fun invoke() : Boolean {
        return true
    }
}