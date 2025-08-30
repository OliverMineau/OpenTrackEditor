package com.minapps.trackeditor.feature_map_editor.domain.usecase

import android.util.Log
import com.minapps.trackeditor.core.domain.model.SimpleWaypoint
import com.minapps.trackeditor.feature_map_editor.presentation.ActionType
import com.minapps.trackeditor.feature_map_editor.presentation.EditState
import com.minapps.trackeditor.feature_map_editor.presentation.UiMapState
import jakarta.inject.Inject

sealed class SelectionResult {
    data class UpdatedState(
        val selectedTracks: List<Int>,
        val selectedPoints: List<Pair<Int, Double>>,
        val direction: AddWaypointUseCase.InsertPosition? = null
    ) : SelectionResult()

    data class WaypointAdded(val trackId: Int, val waypoint: SimpleWaypoint) : SelectionResult()
    data class WaypointRemoved(val trackId: Int, val waypointId: Double) : SelectionResult()
    object None : SelectionResult()
}

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

        Log.d("trackselect", "Selected before  ${currentState.currentSelectedTracks}")

        val selectedTracks = manageTrackSelection(trackId, select, currentState)
        val selectedPoints = managePointSelection(trackId, pointId, currentState)

        Log.d("trackselect", "Selected $selectedTracks")

        results.add(
            SelectionResult.UpdatedState(
                selectedTracks = selectedTracks,
                selectedPoints = selectedPoints,
                direction = currentState.direction
            )
        )

        results += toggleSelectedPoints(currentState.copy(
            currentSelectedTracks = selectedTracks,
            currentSelectedPoints = selectedPoints
        ))

        return results
    }

    private fun manageTrackSelection(
        trackId: Int?,
        select: Boolean,
        state: EditState
    ): MutableList<Int> {
        val selectedTracks = state.currentSelectedTracks.toMutableList()

        if (trackId != null) {
            if (state.currentSelectedTool == ActionType.TOOLBOX || state.currentSelectedTool == ActionType.SELECT) {
                if (!selectedTracks.contains(trackId) && select) selectedTracks.add(trackId)
                else if (selectedTracks.contains(trackId) && !select) selectedTracks.remove(trackId)
            } else {
                selectedTracks.clear()
                if (select) selectedTracks.add(trackId)
            }
        } else if (state.currentSelectedTool != ActionType.SELECT &&
            state.currentSelectedTool != ActionType.TOOLBOX) {
            selectedTracks.clear()
        }
        return selectedTracks
    }

    private fun managePointSelection(
        trackId: Int?,
        pointId: Double?,
        state: EditState
    ): MutableList<Pair<Int, Double>> {
        var selectedPoints = mutableListOf<Pair<Int, Double>>()

        if (trackId != null && pointId != null) {
            val pairPoint = trackId to pointId

            if (state.currentSelectedTool == ActionType.SELECT) {
                selectedPoints = state.currentSelectedPoints.toMutableList()

                if (!selectedPoints.contains(pairPoint) && selectedPoints.size < 2) {
                    selectedPoints.add(pairPoint)
                } else if (!selectedPoints.contains(pairPoint) && selectedPoints.size >= 2) {
                    selectedPoints = mutableListOf(pairPoint)
                }
            } else if (state.currentSelectedTool == ActionType.ADD ||
                state.currentSelectedTool == ActionType.REMOVE) {
                selectedPoints = mutableListOf(pairPoint)
            }
        } else {
            selectedPoints = state.currentSelectedPoints.toMutableList()
        }
        return selectedPoints
    }

    private suspend fun toggleSelectedPoints(state: EditState): List<SelectionResult> {
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
                                AddWaypointUseCase.InsertPosition.BACK
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
