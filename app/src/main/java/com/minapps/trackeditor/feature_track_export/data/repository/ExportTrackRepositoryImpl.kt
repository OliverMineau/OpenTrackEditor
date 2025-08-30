package com.minapps.trackeditor.feature_track_export.data.repository

import android.os.Environment
import android.util.Log
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.mapper.toDomain
import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactory
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.core.domain.repository.ExportTrackRepository
import com.minapps.trackeditor.feature_track_import.domain.model.DataStreamProgress
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream

/**
 * Export Track called by usecase,
 * communicates with dao, retrieves tracks from db by chunks
 *
 * @property dao
 * @property exporterFactory
 */
@ViewModelScoped
class ExportTrackRepositoryImpl @Inject constructor(
    private val dao: TrackDao,
    private val exporterFactory: ExporterFactory,
) : ExportTrackRepository {

    // Size of data chunk
    private val chunkSize = 50000

    private fun getExportFolder(): File {
        val downloadsFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsFolder.exists()) downloadsFolder.mkdirs()
        return downloadsFolder
    }


    override suspend fun saveExportedTrack(
        trackIds: List<Int>,
        fileName: String,
        exportFormat: ExportFormat
    ): Flow<DataStreamProgress> = flow {

        val folder = getExportFolder()
        val file = File(folder, fileName)

        // Get total number of waypoints to estimate progress
        val totalPoints = dao.countWaypointsForTracks(trackIds)
        if (totalPoints == 0) {
            emit(DataStreamProgress.Error("No points found for tracks $trackIds"))
            return@flow
        }

        // Only get track metadata
        /*val track = dao.getTrackById(trackId)?.toDomain(null)
        if (track == null) {
            emit(DataStreamProgress.Error("No track found for track $trackId"))
            return@flow
        }*/

        // Get exporter (GPX, KML..)
        val exporter = exporterFactory.getExporter(exportFormat)

        // Start export
        FileOutputStream(file).use { outputStream ->

            val writer = outputStream.writer()

            // Write header and track segment header
            exporter.exportHeader(writer)

            for (tid in trackIds) {

                val track = dao.getTrackById(tid)?.toDomain(null)
                // If track not found, skip
                if (track == null) {
                    emit(DataStreamProgress.Error("No track found for track $tid"))
                    continue
                }

                var name = track.name
                if (name.isEmpty()) name = "OpenTrackEditorTrack"
                exporter.exportTrackSegmentHeader(name, writer)

                var offset = 0 // Index to start getting waypoints
                var writtenPoints = 0 // Total exported wpts
                var lastProgress = -1 // Last progress percentage

                while (true) {

                    // Get chunk of wpts
                    val waypoints =
                        dao.getWaypointsByChunk(tid, chunkSize, offset).map { it.toDomain() }
                    if (waypoints.isEmpty()) break

                    // Parse and export directly in exporter
                    exporter.exportWaypoints(waypoints, writer)

                    writtenPoints += waypoints.size
                    offset += chunkSize

                    // Calculate progress notification
                    val currentProgress = (writtenPoints.toDouble() / totalPoints * 100).toInt()
                    if (currentProgress != lastProgress) {
                        lastProgress = currentProgress
                        emit(DataStreamProgress.Progress(currentProgress))
                    }
                }

                exporter.exportTrackSegmentFooter(writer)

            }

            // Write footer

            exporter.exportFooter(writer)
            writer.close()
        }

        // Send completed notif
        var tIds = trackIds.toMutableList()
        emit(DataStreamProgress.Completed(tIds))
    }


}