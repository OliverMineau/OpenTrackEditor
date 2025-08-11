package com.minapps.trackeditor.feature_track_import.data.factory

import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.data.formatter.TrackExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.data.parser.GpxParser
import com.minapps.trackeditor.feature_track_import.data.parser.TrackParser
import com.minapps.trackeditor.feature_track_import.domain.model.ImportFormat
import jakarta.inject.Inject

/**
 * Factory returns gpx, kml ... parsers
 *
 * @property gpxParser
 */
class ImporterFactoryImpl @Inject constructor(
    private val gpxParser: GpxParser,
    //TODO add kml
    //private val kmlExporter: KmlExporter
) : ImporterFactory {
    override fun getImporter(format: ImportFormat): TrackParser = when(format) {
        ImportFormat.GPX -> gpxParser
        //TODO add kml
        //ExportFormat.KML -> kmlExporter
        ImportFormat.KML -> TODO()
    }
}
