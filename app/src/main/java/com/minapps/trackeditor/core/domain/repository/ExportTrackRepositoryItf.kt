package com.minapps.trackeditor.core.domain.repository

import android.content.Context
import android.net.Uri
import androidx.room.Insert
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.TrackEntity
import kotlinx.coroutines.flow.Flow
import java.io.File


/**
 * Interface for editing tracks and waypoints.
 *
 * Defines the contract that the data layer must implement.
 * Keeps the domain layer independent from specific data sources
 * (Room, network, etc.).
 */
interface ExportTrackRepositoryItf {

    suspend fun exportTrack(trackId: Int, fileName: String): Uri?

    suspend fun saveTrackAsFile(fileName: String, content: String, context: Context): File

}