package com.minapps.trackeditor.feature_track_export.data.factory

import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.data.formatter.TrackExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import jakarta.inject.Inject

/**
 * Factory pattern implementation
 * Add here file type exporters and the format they correspond to
 * GPX, KML
 *
 */
class ExporterFactoryImpl @Inject constructor(
    private val gpxExporter: GpxExporter,
) : ExporterFactory {
    override fun getExporter(format: ExportFormat): TrackExporter = when(format) {
        ExportFormat.GPX -> gpxExporter
        ExportFormat.KML -> TODO()
    }
}
