package com.minapps.trackeditor.feature_track_import.data.factory

import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.data.formatter.TrackExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.data.parser.GpxParser
import com.minapps.trackeditor.feature_track_import.data.parser.KmlParser
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
    private val kmlParser: KmlParser
) : ImporterFactory {
    override fun getImporter(format: ImportFormat): TrackParser = when(format) {
        ImportFormat.GPX -> gpxParser
        ImportFormat.KML -> kmlParser
    }
}
