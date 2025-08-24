package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.util.TrackSimplifier
import jakarta.inject.Inject

/**
 * Use case responsible for adding/displaying imported track
 *
 * @property repository Repository used to add the waypoint to the data source.
 */
class DisplayTrackUseCase @Inject constructor(
    private val repository: EditTrackRepository,
    //private val trackSimplifier: TrackSimplifier
) {

    val DISPLAY_POINT_COUNT_MAX = 100

    /**
     * Adds/displays imported track to map
     *
     * @param track
     * @return
     */
    suspend operator fun invoke(trackId: Int): Boolean{

        return repository.addImportedTrack(trackId)

        /*var rtval = false
        val waypointCounts = repository.getTrackLastWaypointIndex(trackId)

        // If track can be fully displayed (small track)
        if(waypointCounts.toInt() < DISPLAY_POINT_COUNT_MAX){
            rtval = repository.addImportedTrack(trackId)

        }else{
            // TODO get track by chunks

            // TODO get track by sampling
            val sampleRate = waypointCounts / DISPLAY_POINT_COUNT_MAX
            val waypoints = repository.getTrackWaypointsSample(trackId, sampleRate.toInt())
            rtval = repository.addImportedTrack(trackId, waypoints)


        }



        return rtval*/
    }


}