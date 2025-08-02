package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import jakarta.inject.Inject

class ClearAllUseCase @Inject constructor(
    private val repository: EditTrackRepositoryItf
) {
    suspend operator fun invoke() {
        repository.clearAll()
    }
}