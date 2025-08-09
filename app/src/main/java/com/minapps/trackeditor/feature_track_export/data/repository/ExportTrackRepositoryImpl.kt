package com.minapps.trackeditor.feature_track_export.data.repository

import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.usecase.GetFullTrackUseCase
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.mapper.toDomain
import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_export.domain.repository.ExportTrackRepository
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


@ViewModelScoped
class ExportTrackRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
) : ExportTrackRepository {

    override suspend fun getTrack(trackId: Int): Track? {
        return trackDao.getTrackById(trackId)?.toDomain( trackDao.getTrackWaypoints(trackId))
    }

    override suspend fun saveExportedTrack(fileName: String, exportFunc: (OutputStream) -> Unit): Boolean {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsFolder.exists()) downloadsFolder.mkdirs()

        val file = File(downloadsFolder, fileName)
        return try {
            FileOutputStream(file).use { outputStream ->
                exportFunc(outputStream)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

}