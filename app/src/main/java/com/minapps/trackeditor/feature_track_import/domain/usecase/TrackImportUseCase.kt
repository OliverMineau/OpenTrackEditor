package com.minapps.trackeditor.feature_track_import.domain.usecase

import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
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

    operator fun invoke(file: Uri): Flow<ImportProgress> = flow {

        repository.importTrack(file).collect { importProgress ->

            when (importProgress) {
                is ImportProgress.Completed -> {
                    Log.d("debug", "Done")
                    emit(ImportProgress.Completed(importProgress.trackId))
                }

                is ImportProgress.Error -> {
                    Log.d("debug", "Parse error")
                    emit(ImportProgress.Error(importProgress.message))
                    return@collect
                }

                is ImportProgress.Progress -> emit(ImportProgress.Progress(importProgress.percent))
            }

        }

    }.catch { e ->
        emit(ImportProgress.Error(e.message ?: "Unknown error"))
    }
}
