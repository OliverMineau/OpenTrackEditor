package com.minapps.trackeditor.feature_track_export.domain.repository

import com.minapps.trackeditor.core.domain.model.Track
import java.io.OutputStream

interface ExportTrackRepository {
    suspend fun getTrack(trackId: Int): Track?
    suspend fun saveExportedTrack(fileName: String, exportFunc: (OutputStream) -> Unit): Boolean
}



