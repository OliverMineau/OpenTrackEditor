package com.minapps.trackeditor.core.domain.usecase

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import jakarta.inject.Inject

class GetFullTrackUseCase @Inject constructor(
    private val repository: EditTrackRepositoryItf
) {

    suspend operator fun invoke(trackId: Int): Track? {
        return repository.getFullTrack(trackId)
    }
}