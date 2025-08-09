package com.minapps.trackeditor.feature_track_export.data.factory

import com.minapps.trackeditor.feature_track_export.data.formatter.TrackExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat

interface ExporterFactory {
    fun getExporter(format: ExportFormat): TrackExporter
}
