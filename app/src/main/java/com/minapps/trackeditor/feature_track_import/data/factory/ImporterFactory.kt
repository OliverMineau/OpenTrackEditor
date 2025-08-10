package com.minapps.trackeditor.feature_track_import.data.factory

import com.minapps.trackeditor.feature_track_export.data.formatter.TrackExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.data.parser.TrackParser
import com.minapps.trackeditor.feature_track_import.domain.model.ImportFormat

interface ImporterFactory {
    fun getImporter(format: ImportFormat): TrackParser
}
