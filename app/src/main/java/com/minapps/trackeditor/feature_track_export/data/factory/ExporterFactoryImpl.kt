package com.minapps.trackeditor.feature_track_export.data.factory

import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.data.formatter.TrackExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import jakarta.inject.Inject

class ExporterFactoryImpl @Inject constructor(
    private val gpxExporter: GpxExporter,
    //TODO add kml
    //private val kmlExporter: KmlExporter
) : ExporterFactory {
    override fun getExporter(format: ExportFormat): TrackExporter = when(format) {
        ExportFormat.GPX -> gpxExporter
        //TODO add kml
        //ExportFormat.KML -> kmlExporter
        ExportFormat.KML -> TODO()
    }
}
