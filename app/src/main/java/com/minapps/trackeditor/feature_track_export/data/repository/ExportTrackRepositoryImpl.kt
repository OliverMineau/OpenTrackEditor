package com.minapps.trackeditor.feature_track_export.data.repository

import android.os.Environment
import android.util.Log
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.mapper.toDomain
import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactory
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_export.domain.repository.ExportTrackRepository
import com.minapps.trackeditor.feature_track_import.domain.usecase.DataStreamProgress
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream

/**
 * Export Track called by usecase,
 * communicates with dao, retrieves track object to
 * TODO
 *
 * @property trackDao
 */
@ViewModelScoped
class ExportTrackRepositoryImpl @Inject constructor(
    private val dao: TrackDao,
    private val exporterFactory: ExporterFactory,
) : ExportTrackRepository {

    private val chunkSize = 10000 //50000

    private fun getExportFolder(): File {
        val downloadsFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsFolder.exists()) downloadsFolder.mkdirs()
        return downloadsFolder
    }


    override suspend fun saveExportedTrack(
        trackId: Int,
        fileName: String,
        exportFormat: ExportFormat
    ): Flow<DataStreamProgress> = flow {

        val folder = getExportFolder()
        val file = File(folder, fileName)

        // Estimate total for progress
        Log.d("debug", "expo trackID : $trackId")
        val totalPoints = dao.countWaypointsForTrack(trackId)
        Log.d("debug", "expo toaltpoints : $totalPoints")
        if (totalPoints == 0) {
            emit(DataStreamProgress.Error("No points found for track $trackId"))
            return@flow
        }

        val track = dao.getTrackById(trackId)?.toDomain(null)
        Log.d("debug", "expo track : $track")
        if (track == null) {
            emit(DataStreamProgress.Error("No track found for track $trackId"))
            return@flow
        }

        val exporter = exporterFactory.getExporter(exportFormat)

        FileOutputStream(file).use { outputStream ->

            val writer = outputStream.writer()
            // Write header
            exporter.exportHeader(track, writer)
            exporter.exportTrackSegmentHeader(track.name, writer)

            var offset = 0
            var writtenPoints = 0
            var lastProgress = -1

            while (true) {
                val waypoints =
                    dao.getWaypointsByChunk(trackId, chunkSize, offset).map { it.toDomain() }
                Log.d("debug", "expo waypoints : ${waypoints.size}")
                if (waypoints.isEmpty()) break

                // Stream this chunk directly
                exporter.exportWaypoints(waypoints, writer)

                writtenPoints += waypoints.size
                offset += chunkSize

                // Progress
                val currentProgress = (writtenPoints.toDouble() / totalPoints * 100).toInt()
                if (currentProgress != lastProgress) {
                    lastProgress = currentProgress
                    emit(DataStreamProgress.Progress(currentProgress))
                }
            }

            // Write footer
            exporter.exportTrackSegmentFooter(writer)
            exporter.exportFooter(writer)
            writer.close()
        }

        emit(DataStreamProgress.Completed(trackId))
    }


}