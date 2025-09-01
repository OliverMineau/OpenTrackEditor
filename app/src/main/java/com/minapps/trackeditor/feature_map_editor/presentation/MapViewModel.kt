package com.minapps.trackeditor.feature_map_editor.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.R
import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.domain.model.ProgressData
import com.minapps.trackeditor.feature_map_editor.domain.model.SimpleWaypoint
import com.minapps.trackeditor.feature_map_editor.domain.model.UiMapState
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.type.DataDestination
import com.minapps.trackeditor.core.domain.usecase.HandleMapViewportChangeUseCase
import com.minapps.trackeditor.core.domain.usecase.Viewport
import com.minapps.trackeditor.core.domain.util.ToolGroup
import com.minapps.trackeditor.feature_map_editor.domain.usecase.AddWaypointToSelectedTrackUseCase
import com.minapps.trackeditor.feature_track_export.domain.usecase.ExportTrackUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.AddWaypointUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.ClearAllUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.DeleteSelectedUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.DisplayTrackUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.GetTrackWaypointsUseCase
import com.minapps.trackeditor.feature_map_editor.domain.model.SelectionResult
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.model.UIAction
import com.minapps.trackeditor.feature_map_editor.domain.usecase.UpdateSelectionUseCase
import com.minapps.trackeditor.feature_map_editor.presentation.model.ActionDescriptor
import com.minapps.trackeditor.feature_map_editor.presentation.util.HapticFeedback
import com.minapps.trackeditor.feature_map_editor.presentation.util.StringProvider
import com.minapps.trackeditor.feature_map_editor.tools.delete.DeleteTool
import com.minapps.trackeditor.feature_map_editor.tools.export.ExportTool
import com.minapps.trackeditor.feature_map_editor.tools.filter.FilterTool
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase.ApplyFilterUseCase
import com.minapps.trackeditor.feature_map_editor.tools.join.JoinTool
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.domain.model.DataStreamProgress
import com.minapps.trackeditor.feature_track_import.domain.usecase.TrackImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.lang.reflect.Parameter
import kotlin.Long


@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: EditTrackRepository,
    private val addWaypointUseCase: AddWaypointUseCase,
    private val clearAllUseCase: ClearAllUseCase,
    private val getTrackWaypointsUseCase: GetTrackWaypointsUseCase,
    private val displayTrackUseCase: DisplayTrackUseCase,
    private val exportTrackUseCase: ExportTrackUseCase,
    private val trackImportUseCase: TrackImportUseCase,
    private val addWaypointToSelectedTrackUseCase: AddWaypointToSelectedTrackUseCase,
    private val handleMapViewportChangeUseCase: HandleMapViewportChangeUseCase,
    private val updateSelectionUseCase: UpdateSelectionUseCase,
    private val stringProvider: StringProvider,
    private val hapticFeedback: HapticFeedback,

    private val filterTool: FilterTool,
    private val joinTool: JoinTool,
    private val deleteTool: DeleteTool,
    private val exportTool: ExportTool,

    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Expose data events (Added,Removed,Moved points) to other classes (MapActivity)
    private val _waypointEvents = MutableSharedFlow<WaypointUpdate>()
    val waypointEvents: SharedFlow<WaypointUpdate> = _waypointEvents

    // Expose tool creation events (Add, Select, Remove, Export...) to other classes (MapActivity)
    private val _actions =
        MutableStateFlow<Map<DataDestination, List<ActionDescriptor>>>(emptyMap())
    val actions: StateFlow<Map<DataDestination, List<ActionDescriptor>>> = _actions

    // Expose edition state events (current selected tracks, points, ...) to other classes (MapActivity, OverlayRenderer)
    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState

    // Expose progress (Import, Export)
    private val _progressState = MutableStateFlow(ProgressData())
    val progressState: StateFlow<ProgressData> = _progressState

    // Expose selected export parameters
    private val _showExportDialog = MutableSharedFlow<String>()
    val showExportDialog: SharedFlow<String> = _showExportDialog

    // Expose export result state
    private val _exportResult = MutableSharedFlow<Result<Uri>>()
    val exportResult = _exportResult.asSharedFlow()

    private var lastZoom: Int? = null

    private val actionHandlers: Map<ActionType, UIAction> = mapOf(
        ActionType.EXPORT to { onToolSelected(ActionType.EXPORT) },
        ActionType.SCREENSHOT to { onToolSelected(ActionType.SCREENSHOT) },
        ActionType.DELETE to { onToolSelected(ActionType.DELETE) },
        ActionType.ELEVATION to { onToolSelected(ActionType.ELEVATION) },
        ActionType.LAYERS to { onToolSelected(ActionType.LAYERS) },
        ActionType.REVERSE to { isSelected -> onToolSelected(ActionType.REVERSE, isSelected) },
        ActionType.REMOVE_DUPS to { isSelected ->
            onToolSelected(
                ActionType.REMOVE_DUPS,
                isSelected
            )
        },
        ActionType.REMOVE_BUGS to { isSelected ->
            onToolSelected(
                ActionType.REMOVE_BUGS,
                isSelected
            )
        },
        ActionType.CUT to { isSelected -> onToolSelected(ActionType.CUT, isSelected) },
        ActionType.JOIN to { isSelected -> onToolSelected(ActionType.JOIN, isSelected) },
        ActionType.REDUCE_NOISE to { isSelected ->
            onToolSelected(
                ActionType.REDUCE_NOISE,
                isSelected
            )
        },
        ActionType.FILTER to { isSelected -> onToolSelected(ActionType.FILTER, isSelected) },
        ActionType.MAGIC_FILTER to { isSelected ->
            onToolSelected(
                ActionType.MAGIC_FILTER,
                isSelected
            )
        },
        ActionType.ADD to { onToolSelected(ActionType.ADD) },
        ActionType.REMOVE to { onToolSelected(ActionType.REMOVE) }
    )


    init {
        viewModelScope.launch {

            //Clear database at init
            clearAll()

            //Make MapViewModel listen to changes on "repository.addedTracks"
            repository.addedTracks.collect { (trackId, center) ->
                //If added load waypoints of given track id
                loadTrackWaypoints(trackId, center)
            }

        }

        val tools = listOf(
            ActionType.EXPORT,
            ActionType.SCREENSHOT,
            ActionType.SPACER,
            ActionType.DELETE,
            ActionType.SPACER,
            ActionType.ELEVATION,
            ActionType.LAYERS,
            ActionType.SPACER,
            ActionType.REVERSE,
            ActionType.REMOVE_DUPS,
            ActionType.REMOVE_BUGS,
            ActionType.CUT,
            ActionType.JOIN,
            ActionType.REDUCE_NOISE,
            ActionType.FILTER,
            ActionType.MAGIC_FILTER,
            ActionType.SPACER
        )

        _actions.value = mapOf(
            DataDestination.EDIT_TOOL_POPUP to tools.map { type ->
                val action = actionHandlers[type] // existing callback
                val executor: EditorTool? = when (type) {
                    ActionType.FILTER -> filterTool
                    ActionType.JOIN -> joinTool
                    ActionType.DELETE -> deleteTool
                    ActionType.EXPORT -> exportTool
                    else -> null
                }

                ActionDescriptor(
                    type.icon,
                    type.label,
                    action,
                    executor,
                    type.selectionCount,
                    type,
                    type.group,
                )
            }
        )


    }


    /**
     * Add waypoint to selected track
     *
     * @param lat
     * @param lng
     */
    fun addWaypoint(lat: Double, lng: Double) {
        viewModelScope.launch {
            // Get selected track ids
            val selectedTrackIds = editState.value.currentSelectedTracks
            val (trackId, point) = addWaypointToSelectedTrackUseCase(
                selectedTrackIds,
                lat,
                lng,
                editState.value.direction,
            ) { stringProvider.getString(R.string.track_no_name) }

            // Update map with new waypoint
            _waypointEvents.emit(WaypointUpdate.Added(trackId, point))

            // Select track after adding point
            if (!selectedTrackIds.contains(trackId)) {
                selectedTrackIds.add(trackId)
                _editState.update { it.copy(currentSelectedTracks = selectedTrackIds) }
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            clearAllUseCase()
        }
    }

    /**
     * Manage single tap
     *
     * @param geoPoint
     */
    fun singleTapConfirmed(geoPoint: GeoPoint) {

        if (editState.value.currentSelectedTool == ActionType.ADD) {
            addWaypoint(geoPoint.latitude, geoPoint.longitude)
            Log.d(
                "debug",
                "Added waypoint to track trackId at: ${geoPoint.latitude}, ${geoPoint.longitude}"
            )
        }
    }


    /**
     * Reemit imported track to flow listened to by mapActivity
     *
     * @param trackId
     */
    suspend fun loadTrackWaypoints(trackId: Int, center: Boolean) {
        val waypoints = getTrackWaypointsUseCase(trackId, null, null, null, null)

        if (waypoints.isNotEmpty()) {
            //Reemit points for mapActivity
            val points = waypoints.map { wp -> SimpleWaypoint(wp.id, wp.lat, wp.lng) }
            _waypointEvents.emit(WaypointUpdate.AddedList(trackId, points, center))
        }
    }

    /**
     * Send coord to activity and to database if set
     *
     * @param waypointList
     * @param pointId
     * @param isPointSet
     */
    fun movePoint(
        waypointList: List<Waypoint>,
        pointIndex: Int?,
        pointId: Double?,
        isPointSet: Boolean = false
    ) {

        viewModelScope.launch {
            val trackId = waypointList.first().trackId

            // Point is set, update database
            if (isPointSet && pointId != null && pointIndex != null) {
                addWaypointUseCase(
                    waypointList.first().lat,
                    waypointList.first().lng,
                    pointId,
                    trackId = trackId,
                    editState.value.direction,
                    false
                )

                _waypointEvents.emit(
                    WaypointUpdate.MovedDone(
                        trackId,
                        pointId,
                        waypointList.first().lat to waypointList.first().lng
                    )
                )
                return@launch
            }

            val points = waypointList.map { wp -> Pair(wp.lat, wp.lng) }
            _waypointEvents.emit(WaypointUpdate.Moved(trackId, points))
        }
    }

    fun selectedTrack(trackId: Int?, select: Boolean, pointId: Double?): UiMapState {
        viewModelScope.launch {
            val results = updateSelectionUseCase(
                trackId = trackId,
                pointId = pointId,
                select = select,
                currentState = editState.value
            )

            results.forEach { result ->
                when (result) {
                    is SelectionResult.UpdatedState -> _editState.update {
                        it.copy(
                            currentSelectedTracks = result.selectedTracks.toMutableList(),
                            currentSelectedPoints = result.selectedPoints.toMutableList(),
                            direction = result.direction ?: it.direction,
                            version = System.nanoTime()
                        )
                    }

                    is SelectionResult.WaypointAdded -> _waypointEvents.emit(
                        WaypointUpdate.Added(result.trackId, result.waypoint)
                    )

                    is SelectionResult.WaypointRemoved -> _waypointEvents.emit(
                        WaypointUpdate.RemovedById(result.trackId, result.waypointId)
                    )

                    SelectionResult.None -> {}
                }
            }

            hapticFeedback.vibrate(30)
        }
        return UiMapState(
            editState.value.currentSelectedTracks,
            editState.value.currentSelectedPoints
        )
    }


    fun toolExport(fileName: String) {

        val format = ExportFormat.GPX

        viewModelScope.launch {
            val trackIds = editState.value.currentSelectedTracks
            if (trackIds.isEmpty()) {
                _exportResult.emit(Result.failure(Exception("No tracks selected")))
                return@launch
            }

            Log.d("debug", "Progress show")
            _progressState.update { it.copy(isDisplayed = true) }

            exportTrackUseCase(trackIds, fileName, format).collect { exportResult ->
                when (exportResult) {
                    is DataStreamProgress.Completed -> {
                        Log.d("debug", "export success, mapvm")
                        Log.d("debug", "Progress close")
                        _progressState.update {
                            it.copy(
                                isDisplayed = false,
                                message = "Track exported successfully !"
                            )
                        }
                    }

                    is DataStreamProgress.Error -> {
                        //_exportResult.emit(Result.failure(exportResult.message))
                        Log.d("debug", "export fail : ${exportResult.message}")
                        _progressState.update {
                            it.copy(
                                isDisplayed = false,
                                message = "Exporting track failed."
                            )
                        }
                    }

                    is DataStreamProgress.Progress -> {
                        Log.d("debug", "export progress ${exportResult.percent}, mapvm")
                        _progressState.update {
                            it.copy(
                                progress = exportResult.percent,
                                message = null
                            )
                        }
                    }
                }
            }
        }
    }

    fun importTrack(uri: Uri) {

        onToolSelected(ActionType.VIEW)

        viewModelScope.launch {
            trackImportUseCase(uri).collect { importProgress ->
                _progressState.update { it.copy(isDisplayed = true) }

                when (importProgress) {
                    is DataStreamProgress.Completed -> {

                        var isFirst = true
                        for (id in importProgress.trackIds) {
                            val displayed = displayTrackUseCase(id, isFirst)
                            isFirst = false
                            _progressState.update {
                                it.copy(
                                    isDisplayed = false,
                                    message = "Imported track successfully"
                                )
                            }
                            Log.d(
                                "debug",
                                "VM received finished, trackID:${id}, displayed:$displayed"
                            )
                        }
                    }

                    is DataStreamProgress.Error -> {
                        Log.d("debug", "VM received ERROR! ${importProgress.message}")
                        _progressState.update {
                            it.copy(
                                isDisplayed = false,
                                message = "Importing failed"
                            )
                        }
                    }

                    is DataStreamProgress.Progress -> {
                        Log.d("debug", "VM received progress : ${importProgress.percent}%")
                        _progressState.update {
                            it.copy(
                                progress = importProgress.percent,
                                message = null
                            )
                        }
                    }
                }
            }
        }
    }

    private var hasDisplayedFull = false
    private var hasDisplayedOutline = false
    private var hasStartedCalculationsInThread = false


// TODO : Works but should be redesigned (pls)
    /**
     * When user moves map, recalculate what tracks should be displayed and
     * with which algorithm
     *
     * @param latNorth
     * @param latSouth
     * @param lonWest
     * @param lonEast
     * @param zoom
     */
    fun viewChanged(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double,
        zoom: Double
    ) {
        val viewport = Viewport(latNorth, latSouth, lonWest, lonEast, zoom)

        viewModelScope.launch {
            if (hasStartedCalculationsInThread) return@launch

            hasStartedCalculationsInThread = true

            val result = withContext(Dispatchers.Default) {
                handleMapViewportChangeUseCase(
                    viewport,
                    lastZoom,
                    hasDisplayedFull,
                    hasDisplayedOutline
                )
            }

            result?.let { update ->
                lastZoom = update.snappedZoom
                hasDisplayedFull = update.showFull
                hasDisplayedOutline = update.showOutline

                update.tracks.forEach { (trackId, waypoints) ->
                    val points = waypoints.map { wp -> SimpleWaypoint(wp.id, wp.lat, wp.lng) }
                    _waypointEvents.emit(WaypointUpdate.ViewChanged(trackId, points))
                }
            }

            hasStartedCalculationsInThread = false
        }
    }


    /**
     * Propagates selected tool to UI
     * /!\ Update version, if same tool selected twice, wont trigger /!\
     *
     * @param action
     */
    fun onToolSelected(action: ActionType, isSelected: Boolean = true) {

        hapticFeedback.vibrate(30)

        var action = action
        if (!isSelected) {
            action = ActionType.EDIT
        }

        viewModelScope.launch {

            val freezeSelection =
                (action == ActionType.SELECT ||
                        action == ActionType.TOOLBOX ||
                        !action.deselect)

            _editState.update {

                if (freezeSelection) {
                    it.copy(
                        currentSelectedTool = action,
                        version = System.nanoTime()
                    )
                } else {
                    it.copy(
                        currentSelectedTool = action,
                        currentSelectedTracks = mutableListOf(),
                        currentSelectedPoints = mutableListOf(),
                        version = System.nanoTime()
                    )
                }

            }
        }
        Log.d("debug", "Selected ${action.label}, " +
                "trakcs:${editState.value.currentSelectedTracks}, " +
                "points: ${editState.value.currentSelectedPoints}")
    }

    /**
     * Called when tool finished processing
     *
     * @param tool
     * @param parameters
     */
    suspend fun onToolResult(tool: ActionType, parameters: Any?) {

        when (tool) {
            ActionType.FILTER -> {
                parameters as FilterParams
                sendFilterResults(parameters)
            }

            ActionType.JOIN -> {
                parameters as List<*>?
                sendJoinResults(parameters)
            }

            ActionType.DELETE -> {
                parameters as WaypointUpdate?
                sendDeleteResults(parameters)
            }

            ActionType.EXPORT -> {
                sendExportResults()
            }

            else -> return
        }

    }

    suspend fun sendJoinResults(parameters: List<*>?) {
        parameters?.forEach { result ->
            when (result) {
                is WaypointUpdate.AddedList -> _waypointEvents.emit(result)
                is WaypointUpdate.RemovedTracks -> _waypointEvents.emit(result)
                else -> {}
            }
        }
    }

    fun sendFilterResults(parameters: FilterParams) {
        if (parameters.succeeded) {
            _editState.update {
                it.copy(
                    currentSelectedTracks = mutableListOf(),
                    currentSelectedPoints = mutableListOf(),
                    version = System.nanoTime()
                )
            }
        }
    }

    suspend fun sendDeleteResults(parameters: WaypointUpdate?) {
        if (parameters != null) {
            _waypointEvents.emit(parameters)
        }
        _editState.update { it.copy(currentSelectedPoints = mutableListOf()) }

    }

    private suspend fun sendExportResults() {
        // Emit event to UI to show dialog with default filename
        val defaultTrackName = stringProvider.getString(R.string.track_no_name)
        _showExportDialog.emit(defaultTrackName)
    }


}