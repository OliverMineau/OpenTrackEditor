package com.minapps.trackeditor.feature_track_export.domain.repository

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.domain.usecase.DataStreamProgress
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

interface ExportTrackRepository {
    suspend fun saveExportedTrack(trackId: Int, fileName: String, exportFormat: ExportFormat): Flow<DataStreamProgress>
}



