package com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import jakarta.inject.Inject

class RamerDouglasPeuckerUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    operator fun invoke() : Boolean {
        // TODO
        return true
    }
}