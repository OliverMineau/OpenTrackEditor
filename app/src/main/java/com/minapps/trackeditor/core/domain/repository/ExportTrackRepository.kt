package com.minapps.trackeditor.core.domain.repository

import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.domain.model.DataStreamProgress
import kotlinx.coroutines.flow.Flow

interface ExportTrackRepository {
    suspend fun saveExportedTrack(trackIds: List<Int>, fileName: String, exportFormat: ExportFormat): Flow<DataStreamProgress>
}