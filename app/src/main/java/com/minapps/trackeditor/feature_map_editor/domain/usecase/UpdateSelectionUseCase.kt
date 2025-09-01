package com.minapps.trackeditor.feature_map_editor.domain.usecase

import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.domain.model.SimpleWaypoint
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.type.InsertPosition
import com.minapps.trackeditor.feature_map_editor.domain.model.SelectionResult
import jakarta.inject.Inject



/**
 * Update user selections (Points and Tracks)
 *
 * @property getNewPointDirectionUseCase
 * @property addWaypointUseCase
 * @property deleteWaypointUseCase
 */
class UpdateSelectionUseCase @Inject constructor(
    private val getNewPointDirectionUseCase: GetNewPointDirectionUseCase,
    private val addWaypointUseCase: AddWaypointUseCase,
    private val deleteWaypointUseCase: DeleteWaypointUseCase,
) {
    suspend operator fun invoke(
        trackId: Int?,
        pointId: Double?,
        select: Boolean,
        currentState: EditState,
    ): List<SelectionResult> {
        val results = mutableListOf<SelectionResult>()

        var selectedTracks = manageTrackSelection(trackId, select, currentState)
        val selectedPoints = managePointSelection(trackId, pointId, currentState)

        if(selectedPoints.isNotEmpty() && pointId != null){
            selectedTracks = mutableListOf()
            selectedPoints.forEach { pointId ->

                if(!selectedTracks.contains(pointId.first)){
                    selectedTracks.add(pointId.first)
                }

            }
        }else{
            selectedPoints.clear()
        }

        results.add(
            SelectionResult.UpdatedState(
                selectedTracks = selectedTracks,
                selectedPoints = selectedPoints,
                direction = currentState.direction
            )
        )

        results += clickSelectedPoints(
            currentState.copy(
                currentSelectedTracks = selectedTracks,
                currentSelectedPoints = selectedPoints
            )
        )

        return results
    }

    /**
     * Decide which tracks to select or not
     *
     * @param trackId
     * @param select
     * @param state
     * @return
     */
    private fun manageTrackSelection(
        trackId: Int?,
        select: Boolean,
        state: EditState
    ): MutableList<Int> {
        val selectedTracks = state.currentSelectedTracks.toMutableList()

        // If selected a track
        if (trackId != null) {

            // If state is SELECT or TOOLBOX
            if (state.currentSelectedTool == ActionType.TOOLBOX || state.currentSelectedTool == ActionType.SELECT) {

                // Add selected track if in selection mode
                if (!selectedTracks.contains(trackId) && select) selectedTracks.add(trackId)
                // Remove track if in deselection mode
                else if (selectedTracks.contains(trackId) && !select) selectedTracks.remove(trackId)
            }
            // If any state
            else {

                selectedTracks.clear()
                if (select) selectedTracks.add(trackId)
            }
        }
        // If tool isn't SELECT or TOOLBOX
        else if (state.currentSelectedTool != ActionType.SELECT &&
            state.currentSelectedTool != ActionType.TOOLBOX
        ) {
            selectedTracks.clear()
        }

        return selectedTracks
    }

    /**
     * Decide which points to select or not
     *
     * @param trackId
     * @param pointId
     * @param state
     * @return
     */
    private fun managePointSelection(
        trackId: Int?,
        pointId: Double?,
        state: EditState
    ): MutableList<Pair<Int, Double>> {
        var selectedPoints = mutableListOf<Pair<Int, Double>>()

        // If point from track selected
        if (trackId != null && pointId != null) {
            val pairPoint = trackId to pointId

            // If in selection mode (Multiple selection)
            if (state.currentSelectedTool == ActionType.SELECT) {
                selectedPoints = state.currentSelectedPoints.toMutableList()

                // If selected point isn't already selected and
                // LESS than 2 points are already selected
                if (!selectedPoints.contains(pairPoint) && selectedPoints.size < 2) {
                    // Add point
                    selectedPoints.add(pairPoint)
                }
                // If selected point isn't already selected and
                // MORE than 2 points are already selected
                else if (!selectedPoints.contains(pairPoint) && selectedPoints.size >= 2) {
                    // Set as only point selected
                    selectedPoints = mutableListOf(pairPoint)
                }
            }
            // If tool are ADD or REMOVE (only one point can be selected)
            else if (state.currentSelectedTool == ActionType.ADD ||
                state.currentSelectedTool == ActionType.REMOVE
            ) {
                selectedPoints = mutableListOf(pairPoint)
            }
        }
        // If no points selected, return same list of points
        else {
            selectedPoints = state.currentSelectedPoints.toMutableList()
        }
        return selectedPoints
    }

    /**
     * Called after clicking a point
     *
     * @param state
     * @return
     */
    private suspend fun clickSelectedPoints(state: EditState): List<SelectionResult> {
        return when (state.currentSelectedTool) {
            ActionType.JOIN -> {
                // Future: call JoinTrackUseCase
                emptyList()
            }

            ActionType.ADD -> {
                if (state.currentSelectedPoints.isNotEmpty()) {
                    val (trackId, pointId) = state.currentSelectedPoints.first()
                    val (newWaypoint, direction) = getNewPointDirectionUseCase(trackId, pointId)

                    if (newWaypoint != null) {
                        addWaypointUseCase(
                            newWaypoint.lat,
                            newWaypoint.lng,
                            newWaypoint.id,
                            newWaypoint.trackId,
                            direction,
                            false
                        )
                        listOf(
                            SelectionResult.WaypointAdded(
                                newWaypoint.trackId,
                                SimpleWaypoint(newWaypoint.id, newWaypoint.lat, newWaypoint.lng)
                            ),
                            SelectionResult.UpdatedState(
                                state.currentSelectedTracks,
                                state.currentSelectedPoints,
                                InsertPosition.BACK
                            )
                        )
                    } else {
                        listOf(
                            SelectionResult.UpdatedState(
                                state.currentSelectedTracks,
                                state.currentSelectedPoints,
                                direction
                            )
                        )
                    }
                } else emptyList()
            }

            ActionType.REMOVE -> {
                if (state.currentSelectedPoints.isNotEmpty()) {
                    val (trackId, pointId) = state.currentSelectedPoints.first()
                    deleteWaypointUseCase(trackId, pointId)
                    listOf(
                        SelectionResult.WaypointRemoved(trackId, pointId),
                        SelectionResult.UpdatedState(mutableListOf(trackId), mutableListOf())
                    )
                } else emptyList()
            }

            else -> emptyList()
        }
    }
}
