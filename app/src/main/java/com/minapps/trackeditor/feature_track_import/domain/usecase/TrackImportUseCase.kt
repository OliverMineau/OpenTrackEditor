package com.minapps.trackeditor.feature_track_import.domain.usecase

import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.feature_track_import.domain.repository.TrackImportRepository
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
     * Invokes the use case to import a track asynchronously.
     *
     * @param fileUri The Uri pointing to the track file to import.
     */
    /*suspend operator fun invoke(fileUri: Uri): ImportProgress{
        val importedTrack = trackImportRepository.importTrack(fileUri)
        Log.d("debug", "Track imported ${if (importedTrack != null) "successfully" else "unsuccessfully"} id:${importedTrack?.id}, (${importedTrack?.waypoints?.size} waypoints) e.g. ${importedTrack?.waypoints?.get(0)}}")

        if(importedTrack == null){
            Log.d("debug", "Track import error")
        }

        return importedTrack
    }*/

    operator fun invoke(file: Uri): Flow<DataStreamProgress> = flow {

        repository.importTrack(file).collect { importProgress ->

            when (importProgress) {
                is DataStreamProgress.Completed -> {
                    Log.d("debug", "Done")
                    emit(DataStreamProgress.Completed(importProgress.trackId))
                }

                is DataStreamProgress.Error -> {
                    Log.d("debug", "Parse error")
                    emit(DataStreamProgress.Error(importProgress.message))
                    return@collect
                }

                is DataStreamProgress.Progress -> emit(DataStreamProgress.Progress(importProgress.percent))
            }

        }

    }.catch { e ->
        emit(DataStreamProgress.Error(e.message ?: "Unknown import error"))
    }
}
