package com.minapps.trackeditor.core.domain.usecase

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

class GetFullTrackUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke(trackId: Int): Track? {
        return repository.getFullTrack(trackId)
    }
}