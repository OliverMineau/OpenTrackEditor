package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.type.EndType
import com.minapps.trackeditor.feature_map_editor.domain.model.SimpleWaypoint
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import jakarta.inject.Inject
import kotlin.collections.get



class JoinTracksUseCase @Inject constructor(
    private val repository: EditTrackRepository
) {

    suspend operator fun invoke(
        selectedPoints: List<Pair<Int, Double>>,
    ): List<WaypointUpdate>? {

        if (selectedPoints.size < 2) return null

        // Verify if each point is from different tracks
        if (selectedPoints[0].first == selectedPoints[1].first) return null

        // Verify if each point is start or end of their respective tracks
        val trackAPoint = selectedPoints[0]
        val trackA = checkEnds(trackAPoint) ?: return null
        val trackBPoint = selectedPoints[1]
        val trackB = checkEnds(trackBPoint) ?: return null

        // Get track to edit and to leave depending on situation
        // If end end -> reset ids of any track from end+1 ascending
        // If start end or end start -> reset ids of start track from end+1
        // If start start -> reset ids of any track from start-1 descending
        var trackToEdit = trackAPoint
        var trackToLeave = trackBPoint
        if (trackB == EndType.START) {
            trackToEdit = trackBPoint
            trackToLeave = trackAPoint
        }

        // If track has to be reversed
        val reverseIndex = (trackA == trackB && trackA == EndType.END)
        // If we have to decrement the values
        val decrement = (trackA == trackB && trackA == EndType.START)

        var id: Double
        if(decrement){
            id = repository.getTrackFirstWaypointId(trackToLeave.first) ?: return null
            id--
        }else{
            id = repository.getTrackLastWaypointId(trackToLeave.first) ?: return null
            id++
        }

        // Set right waypoint ids
        repository.renumberTrack(trackToEdit.first, id, decrement, reverseIndex)

        // Change track id
        repository.changeTrackId(trackToEdit.first, trackToLeave.first)

        val waypoints = repository.getTrackWaypoints(trackToLeave.first)
            .map { SimpleWaypoint(it.id, it.lat, it.lng) }
        val result =
            mutableListOf<WaypointUpdate>(WaypointUpdate.RemovedTracks(listOf(trackToEdit.first)))
        result.add(WaypointUpdate.AddedList(trackToLeave.first, waypoints, false))
        return result
    }

    /**
     * Returns end type of selected point
     *
     * @param trackAndPoint
     * @return
     */
    private suspend fun checkEnds(trackAndPoint: Pair<Int, Double>): EndType? {
        val firstId = repository.getTrackFirstWaypointId(trackAndPoint.first)
        val lastId = repository.getTrackLastWaypointId(trackAndPoint.first)

        val pointIsStart = trackAndPoint.second == firstId
        val pointIsEnd = trackAndPoint.second == lastId

        if (!pointIsStart && !pointIsEnd) {
            return null
        }

        if (pointIsStart) {
            return EndType.START
        }

        return EndType.END
    }
}
