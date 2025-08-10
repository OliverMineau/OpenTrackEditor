package com.minapps.trackeditor.feature_track_export.domain.usecase

import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactory
import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_export.domain.repository.ExportTrackRepository
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import jakarta.inject.Singleton



@ViewModelScoped
class ExportTrackUseCase @Inject constructor(
    private val repository: ExportTrackRepository,
    private val exporterFactory: ExporterFactory,
) {

    suspend operator fun invoke(trackId: Int, fileName: String, format: ExportFormat): Boolean {

        val track = repository.getTrack(trackId) ?: return false
        if (track.waypoints == null || track.waypoints.isEmpty()) return false

        val exporter = exporterFactory.getExporter(format)

        return repository.saveExportedTrack(fileName) { outputStream ->
            exporter.export(track, outputStream)
        }
    }

}
