package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import jakarta.inject.Inject

/**
 * Use case responsible for adding/displaying imported track
 *
 * @property repository Repository used to add the waypoint to the data source.
 */
class DisplayTrackUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    /**
     * Adds/displays imported track to map
     *
     * @param track
     * @return
     */
    suspend operator fun invoke(trackId: Int): Boolean{
        return repository.addImportedTrack(trackId)
    }


}