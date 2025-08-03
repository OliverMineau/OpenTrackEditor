package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import jakarta.inject.Inject

/**
 * Use case responsible for adding/displaying imported track
 *
 * @property repository Repository used to add the waypoint to the data source.
 */
class AddImportedTrackUseCase @Inject constructor(
    private val repository: EditTrackRepositoryItf
) {

    /**
     * Adds/displays imported track to map
     *
     * @param track
     * @return
     */
    suspend operator fun invoke(track: Track): Track {
        return repository.addImportedTrack(track)
    }


}