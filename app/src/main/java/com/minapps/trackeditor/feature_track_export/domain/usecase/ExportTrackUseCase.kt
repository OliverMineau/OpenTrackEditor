package com.minapps.trackeditor.feature_track_export.domain.usecase


import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.core.domain.repository.ExportTrackRepository
import com.minapps.trackeditor.feature_track_import.domain.usecase.DataStreamProgress
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow


/**
 * Export track use case
 * exports async by chunks
 * returns DataStreamProgress
 *
 * @property repository
 */
@ViewModelScoped
class ExportTrackUseCase @Inject constructor(
    private val repository: ExportTrackRepository,
) {

    operator fun invoke(
        trackIds: List<Int>,
        fileName: String,
        exportFormat: ExportFormat,
    ): Flow<DataStreamProgress> = flow {

        // Call repo in charge of setting up the export
        repository.saveExportedTrack(trackIds, fileName, exportFormat).collect { exportProgress ->
            when(exportProgress){
                is DataStreamProgress.Completed -> emit(exportProgress)
                is DataStreamProgress.Error -> emit(exportProgress)
                is DataStreamProgress.Progress -> emit(exportProgress)
            }
        }
    }.catch { e ->
        emit(DataStreamProgress.Error(e.message ?: "Unknown export error"))
    }


}
