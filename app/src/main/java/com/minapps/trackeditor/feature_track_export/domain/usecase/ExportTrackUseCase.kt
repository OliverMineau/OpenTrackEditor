package com.minapps.trackeditor.feature_track_export.domain.usecase

import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactory
import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_export.domain.repository.ExportTrackRepository
import com.minapps.trackeditor.feature_track_import.domain.usecase.DataStreamProgress
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow


@ViewModelScoped
class ExportTrackUseCase @Inject constructor(
    private val repository: ExportTrackRepository,
) {

    operator fun invoke(
        trackId: Int,
        fileName: String,
        exportFormat: ExportFormat,
    ): Flow<DataStreamProgress> = flow {

        repository.saveExportedTrack(trackId, fileName, exportFormat).collect { exportProgress ->
            when(exportProgress){
                is DataStreamProgress.Completed -> emit(DataStreamProgress.Completed(exportProgress.trackId))
                is DataStreamProgress.Error -> emit(DataStreamProgress.Error(exportProgress.message))
                is DataStreamProgress.Progress -> emit(DataStreamProgress.Progress(exportProgress.percent))
            }
        }
    }.catch { e ->
        emit(DataStreamProgress.Error(e.message ?: "Unknown export error"))
    }


}
