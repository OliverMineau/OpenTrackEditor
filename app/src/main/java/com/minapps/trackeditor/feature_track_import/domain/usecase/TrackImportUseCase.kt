package com.minapps.trackeditor.feature_track_import.domain.usecase

import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.core.domain.repository.TrackImportRepository
import com.minapps.trackeditor.feature_track_import.domain.model.DataStreamProgress
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/**
 * Use case responsible for importing a track from a given file Uri.
 */
@ViewModelScoped
class TrackImportUseCase @Inject constructor(
    private val repository: TrackImportRepository,
) {

    /**
     * Invokes the use case to import a track.
     *
     * @param fileUri The Uri pointing to the track file to import.
     */
    operator fun invoke(file: Uri): Flow<DataStreamProgress> = flow {

        repository.importTrack(file).collect { importProgress ->

            when (importProgress) {
                is DataStreamProgress.Completed -> {
                    Log.d("debug", "Done")
                    emit(importProgress)
                }

                is DataStreamProgress.Error -> {
                    Log.d("debug", "Parse error")
                    emit(importProgress)
                    return@collect
                }

                is DataStreamProgress.Progress -> emit(importProgress)
                is DataStreamProgress.PartialSuccess -> emit(importProgress)
            }

        }

    }.catch { e ->
        if (e is CancellationException) throw e
        emit(DataStreamProgress.Error(e.message ?: "Unknown import error"))
    }

    /** Keep recovered tracks, marking only affected tracks as incomplete. */
    suspend fun keepRecoveredTracks(affectedTrackIds: List<Int>) {
        repository.keepRecoveredTracks(affectedTrackIds)
    }

    /** Discard all tracks created by the import. */
    suspend fun discardImportedTracks(trackIds: List<Int>) {
        repository.discardImportedTracks(trackIds)
    }
}
