package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

/**
 * Use case responsible for deleting entirety of database
 *
 * @property repository
 */
class ClearAllUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke() {
        repository.clearAll()
    }
}