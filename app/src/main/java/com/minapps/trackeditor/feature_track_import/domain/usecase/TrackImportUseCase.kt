package com.minapps.trackeditor.feature_track_import.domain.usecase

import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.core.domain.repository.TrackImportRepository
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
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
            }

        }

    }.catch { e ->
        emit(DataStreamProgress.Error(e.message ?: "Unknown import error"))
    }
}
