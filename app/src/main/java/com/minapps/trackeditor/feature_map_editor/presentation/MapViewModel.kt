package com.minapps.trackeditor.feature_map_editor.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.model.SimpleWaypoint
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.usecase.UpdateMapViewUseCase
import com.minapps.trackeditor.core.domain.util.MapUpdateViewHelper
import com.minapps.trackeditor.core.domain.util.SelectionCount
import com.minapps.trackeditor.core.domain.util.ToolGroup
import com.minapps.trackeditor.feature_track_export.domain.usecase.ExportTrackUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.AddWaypointUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.ClearAllUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.CreateTrackUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.DisplayTrackUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.GetTrackLastWaypointIndexUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.GetTrackWaypointsUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.UIAction
import com.minapps.trackeditor.feature_map_editor.presentation.util.vibrate
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
import com.minapps.trackeditor.feature_track_import.domain.usecase.DataStreamProgress
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

sealed class WaypointUpdate {
    data class Added(val trackId: Int, val point: SimpleWaypoint) : WaypointUpdate()
    data class ViewChanged(val trackId: Int, val points: List<SimpleWaypoint>) : WaypointUpdate()
    data class AddedList(val trackId: Int, val points: List<SimpleWaypoint>, val center: Boolean) :
        WaypointUpdate()

    data class Removed(val trackId: Int, val index: Int) : WaypointUpdate()
    data class Moved(val trackId: Int, val points: List<Pair<Double, Double>>) : WaypointUpdate()
    data class MovedDone(val trackId: Int, val pointId: Double, val point: Pair<Double, Double>) :
        WaypointUpdate()

    data class Cleared(val trackId: Int) : WaypointUpdate()
}

data class ActionDescriptor(
    val icon: Int?,
    val label: String?,
    val action: UIAction?,
    val selectionCount: SelectionCount?,
    val type: ActionType,
    val group: ToolGroup?
)

data class UiMapState(
    val selectedTrackIds: MutableList<Int>,
    val selectedPoints: MutableList<Pair<Int, Double>>,
)


enum class ActionType(
    val icon: Int?,
    val label: String?,
    val selectionCount: SelectionCount?,
    val group: ToolGroup? = ToolGroup.NONE
) {
    // No action
    NONE(null, null, null),
    SPACER(null, null, null),

    // File & system actions
    EXPORT(R.drawable.file_export_24, "Export", SelectionCount.NONE, ToolGroup.FILE_SYSTEM),
    SCREENSHOT(
        R.drawable.mode_landscape_24,
        "Screenshot",
        SelectionCount.NONE,
        ToolGroup.FILE_SYSTEM
    ),
    CLEAR(R.drawable.trash_24, "Clear", SelectionCount.NONE, ToolGroup.FILE_SYSTEM),

    // Visual tools
    ELEVATION(R.drawable.curve_arrow_24, "Elevation", SelectionCount.MULTIPLE),
    LAYERS(R.drawable.land_layers_24, "Layers", SelectionCount.MULTIPLE),

    // Editing tools
    REVERSE(R.drawable.rotate_reverse_24, "Reverse", SelectionCount.ONE, ToolGroup.TRACK_EDITING),
    REMOVE_DUPS(
        R.drawable.circle_overlap_24,
        "Remove dups",
        SelectionCount.ONE,
        ToolGroup.TRACK_EDITING
    ),
    REMOVE_BUGS(
        R.drawable.bug_slash_24,
        "Remove bugs",
        SelectionCount.ONE,
        ToolGroup.TRACK_EDITING
    ),
    CUT(R.drawable.scissors_24, "Cut", SelectionCount.ONE, ToolGroup.TRACK_EDITING),
    JOIN(R.drawable.link_alt_24, "Join", SelectionCount.ONE, ToolGroup.TRACK_EDITING),
    REDUCE_NOISE(
        R.drawable.noise_cancelling_headphones_24,
        "Reduce noise",
        SelectionCount.ONE,
        ToolGroup.TRACK_EDITING
    ),
    FILTER(R.drawable.filter_24, "Filter", SelectionCount.ONE, ToolGroup.TRACK_EDITING),
    MAGIC_FILTER(R.drawable.sweep_24, "Magic filter", SelectionCount.ONE, ToolGroup.TRACK_EDITING),

    // Edit Bottom Navigation
    ADD(R.drawable.map_marker_plus_24, "Add", SelectionCount.ONE, ToolGroup.TRACK_EDITING),
    REMOVE(R.drawable.map_marker_minus_24, "Remove", SelectionCount.ONE, ToolGroup.TRACK_EDITING),

    // Main Bottom Navigation
    VIEW(R.drawable.map_marker_24, "View", SelectionCount.ONE, ToolGroup.TRACK_EDITING),
    EDIT(R.drawable.file_edit_24, "Edit", SelectionCount.MULTIPLE, ToolGroup.TRACK_EDITING),
    TOOLBOX(R.drawable.tools_24, "Toolbox", SelectionCount.MULTIPLE, ToolGroup.TRACK_EDITING)

}

data class EditState(
    val currentSelectedTool: ActionType = ActionType.NONE,
    val currentselectedTracks: MutableList<Int> = mutableListOf(),
    val currentselectedPoints: MutableList<Pair<Int, Double>> = mutableListOf(),
    val version: Long = System.nanoTime(),
)

enum class DataDestination {
    EDIT_BOTTOM_NAV,
    EDIT_TOOL_POPUP,
}

data class ProgressData(
    val progress: Int = 0,
    val isDisplayed: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: EditTrackRepository,
    private val addWaypointUseCase: AddWaypointUseCase,
    private val clearAllUseCase: ClearAllUseCase,
    private val createTrackUseCase: CreateTrackUseCase,
    private val getTrackWaypointsUseCase: GetTrackWaypointsUseCase,
    private val displayTrackUseCase: DisplayTrackUseCase,
    private val exportTrackUseCase: ExportTrackUseCase,
    private val getTrackLastWaypointIndexUseCase: GetTrackLastWaypointIndexUseCase,
    private val updateMapViewUseCase: UpdateMapViewUseCase,
    private val trackImportUseCase: TrackImportUseCase,
    private val mapUpdateViewHelper: MapUpdateViewHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Expose data events (Add,Remove,Move points) to other classes (MapActivity)
    private val _waypointEvents = MutableSharedFlow<WaypointUpdate>()
    val waypointEvents: SharedFlow<WaypointUpdate> = _waypointEvents

    private val _actions =
        MutableStateFlow<Map<DataDestination, List<ActionDescriptor>>>(emptyMap())
    val actions: StateFlow<Map<DataDestination, List<ActionDescriptor>>> = _actions

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState

    private val _progressState = MutableStateFlow(ProgressData())
    val progressState: StateFlow<ProgressData> = _progressState

    private val _showExportDialog = MutableSharedFlow<String>()
    val showExportDialog: SharedFlow<String> = _showExportDialog

    private val _exportResult = MutableSharedFlow<Result<Uri>>()
    val exportResult = _exportResult.asSharedFlow()

    private var lastZoom: Int? = null

    private val trackWaypointIndexes = mutableMapOf<Int, Double>()


    private val actionHandlers: Map<ActionType, UIAction> = mapOf(
        ActionType.EXPORT to { selectedTool(ActionType.EXPORT) },
        ActionType.SCREENSHOT to { selectedTool(ActionType.SCREENSHOT) },
        ActionType.CLEAR to { selectedTool(ActionType.CLEAR) },
        ActionType.ELEVATION to { selectedTool(ActionType.ELEVATION) },
        ActionType.LAYERS to { selectedTool(ActionType.LAYERS) },
        ActionType.REVERSE to { selectedTool(ActionType.REVERSE) },
        ActionType.REMOVE_DUPS to { selectedTool(ActionType.REMOVE_DUPS) },
        ActionType.REMOVE_BUGS to { selectedTool(ActionType.REMOVE_BUGS) },
        ActionType.CUT to { selectedTool(ActionType.CUT) },
        ActionType.JOIN to { selectedTool(ActionType.JOIN) },
        ActionType.REDUCE_NOISE to { selectedTool(ActionType.REDUCE_NOISE) },
        ActionType.FILTER to { selectedTool(ActionType.FILTER) },
        ActionType.MAGIC_FILTER to { selectedTool(ActionType.MAGIC_FILTER) },
        ActionType.ADD to { selectedTool(ActionType.ADD) },
        ActionType.REMOVE to { selectedTool(ActionType.REMOVE) }
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
            ActionType.CLEAR,
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
                val action = actionHandlers[type]
                ActionDescriptor(
                    type.icon,
                    type.label,
                    action,
                    type.selectionCount,
                    type,
                    type.group
                )
            }
        )


    }

    /**
     * Propagates selected tool to UI
     * /!\ Update version, if same tool selected twice, wont trigger /!\
     *
     * @param action
     */
    fun selectedTool(action: ActionType) {

        context.vibrate(30)


        if (action == ActionType.EXPORT) {
            _editState.update {
                it.copy(
                    currentSelectedTool = action,
                    version = System.nanoTime()
                )
            }
        } else {
            _editState.update {
                it.copy(
                    currentSelectedTool = action,
                    currentselectedTracks = mutableListOf(),
                    currentselectedPoints = mutableListOf(),
                    version = System.nanoTime()
                )
            }
        }

        Log.d("debug", "Selected ${action.label}")

        viewModelScope.launch {
            when (action) {
                ActionType.EXPORT -> toolExportTrack()
                ActionType.SCREENSHOT -> toolGetScreenshot()
                else -> {}
            }
        }
    }

    private suspend fun toolExportTrack() {
        Log.d("debug", "Exporting Track Fun")

        // Emit event to UI to show dialog with default filename
        _showExportDialog.emit("track.gpx")
    }


    private suspend fun toolGetScreenshot() {
        Log.d("debug", "Screenshot")
    }

    /**
     * Add waypoint to selected track
     *
     * @param lat
     * @param lng
     */
    fun addWaypoint(lat: Double, lng: Double) {

        val selectedTrackIds = editState.value.currentselectedTracks
        val currentTool = editState.value.currentSelectedTool

        // If no track is selected, do nothing
        if (selectedTrackIds.isEmpty()) return
        var selectedTrackId = selectedTrackIds.first()

        // If not Add as selected tool
        if (currentTool != ActionType.ADD) return


        viewModelScope.launch {

            /*val currentId =
                trackWaypointIndexes[selectedTrackId] ?: (getTrackLastWaypointIndexUseCase(
                    selectedTrackId
                ) + 1.0)*/

            val currentId = addWaypointUseCase.getNextId(selectedTrackId)

            addWaypointUseCase(lat, lng, currentId, selectedTrackId)
            // Notify observers that a waypoint was added
            _waypointEvents.emit(
                WaypointUpdate.Added(
                    selectedTrackId,
                    SimpleWaypoint(currentId, lat, lng)
                )
            )

            // Update index only after successful addition TODO DELETE
            trackWaypointIndexes[selectedTrackId] = currentId + 1
        }
    }


    /**
     * Create new track entity if no track is selected
     *
     * @return Track id (Never Null)
     */
    suspend fun createNewTrackIfNeeded(): Int {

        val selectedTrackIds = editState.value.currentselectedTracks
        var selectedTrackId: Int? = null

        // If no track selected
        if (selectedTrackIds.isEmpty()) {
            //Set default track name
            val newId = createTrackUseCase(context.getString(R.string.track_no_name))

            selectedTrackId = newId
            trackWaypointIndexes[selectedTrackId] = 0.0
            _editState.update {
                val updatedList = it.currentselectedTracks.apply {
                    if (!contains(selectedTrackId)) {
                        add(selectedTrackId)
                    }
                }
                it.copy(
                    currentselectedTracks = updatedList,
                    version = System.nanoTime()
                )
            }

            Log.d("debug", "Created new track with ID: $newId")
        } else {
            selectedTrackId = selectedTrackIds.first()
        }
        return selectedTrackId
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
    suspend fun singleTapConfirmed(geoPoint: GeoPoint) {

        if (editState.value.currentSelectedTool == ActionType.ADD) {
            val trackId = createNewTrackIfNeeded()
            addWaypoint(geoPoint.latitude, geoPoint.longitude)
            Log.d(
                "debug",
                "Added waypoint to track $trackId at: ${geoPoint.latitude}, ${geoPoint.longitude}"
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

        //val currentTool = editState.value.currentSelectedTool
        //if(currentTool == ActionType.NONE)

        viewModelScope.launch {
            val trackId = waypointList.first().trackId

            // Point is set, update database
            if (isPointSet && pointId != null && pointIndex != null) {
                addWaypointUseCase(
                    waypointList.first().lat,
                    waypointList.first().lng,
                    pointId,
                    trackId = trackId,
                    0.0
                )

                // TODO WRONG INDEX GIVEN
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

    /**
     * Updates selected track
     *
     * @param trackId
     * @param vibrate
     */
    fun selectedTrack(trackId: Int?, select: Boolean = false, pointId: Double?): UiMapState {

        val selectedTracks = manageTrackSelection(trackId, select)
        var selectedPoints = mutableListOf<Pair<Int, Double>>()

        if (trackId != null) {
            selectedPoints = managePointSelection(trackId, pointId)
        }

        val vibrate = true
        if (vibrate) {
            context.vibrate(30)
        }

        _editState.update {
            it.copy(
                currentselectedTracks = selectedTracks,
                currentselectedPoints = selectedPoints,
                version = System.nanoTime()
            )
        }

        viewModelScope.launch {
            toggleSelectedPoints()
        }

        Log.d(
            "debug",
            "Selected track ${editState.value.currentselectedTracks}\nSelected points: ${editState.value.currentselectedPoints}"
        )
        return UiMapState(selectedTracks, selectedPoints)
    }


    fun manageTrackSelection(trackId: Int?, select: Boolean): MutableList<Int> {

        val selectedTracks = editState.value.currentselectedTracks

        if (trackId != null) {

            // TOOLBOX SELECTED
            if (editState.value.currentSelectedTool == ActionType.TOOLBOX) {

                // If not selected and wants to be selected
                if (!selectedTracks.contains(trackId) && select) {
                    selectedTracks.add(trackId)
                }
                // If selected and wants to be unselected
                else if (selectedTracks.contains(trackId) && !select) {
                    selectedTracks.remove(trackId)
                }
            }
            // If other tool
            else {

                // Deselect all
                selectedTracks.clear()

                // Add newly selected
                if (select) {
                    selectedTracks.add(trackId)
                }
            }

        }
        // If null unselect all
        else {
            selectedTracks.clear()
        }

        return selectedTracks
    }

    fun managePointSelection(trackId: Int, pointId: Double?): MutableList<Pair<Int, Double>> {

        // If no selected point, empty
        var selectedPoints = mutableListOf<Pair<Int, Double>>()

        if (pointId != null) {

            val pairPoint = Pair(trackId, pointId)

            // If multiple points selection
            if (editState.value.currentSelectedTool == ActionType.JOIN) {
                selectedPoints = editState.value.currentselectedPoints

                // If point not in list and list is < 2 : add
                if (!selectedPoints.contains(pairPoint) && selectedPoints.size < 2) {
                    selectedPoints.add(pairPoint)
                }
                // If point not in list and list full : clear list and add point
                else if (!selectedPoints.contains(pairPoint) && selectedPoints.size >= 2) {
                    selectedPoints = mutableListOf(pairPoint)
                }
            }

            // If single point selection
            else if (editState.value.currentSelectedTool == ActionType.ADD) {
                selectedPoints = mutableListOf(Pair(trackId, pointId))
            }
        }

        return selectedPoints
    }

    /**
     * Called when point selection changed
     *
     */
    private suspend fun toggleSelectedPoints(){
        when(editState.value.currentSelectedTool){
            ActionType.JOIN -> {
                Log.d("Join", "Call JoinTrackUseCase points :${editState.value.currentselectedPoints}")
            }
            ActionType.ADD -> {
                if(editState.value.currentselectedPoints.isNotEmpty()){
                    val newPoint = addWaypointUseCase.updateMarker(editState.value.currentselectedPoints.first().first, editState.value.currentselectedPoints.first().second)

                    /*if(newPoint !=null){
                        Log.d("add innne", "Do we add inner : $newPoint")
                        addWaypointUseCase.addInnerWaypoint()
                    }*/
                }
            }
            else -> {}
        }
    }

    fun toolExport(fileName: String) {

        val format = ExportFormat.GPX

        viewModelScope.launch {
            val trackIds = editState.value.currentselectedTracks
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

        selectedTool(ActionType.VIEW)

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
    fun viewChanged(
        latNorth: Double,
        latSouth: Double,
        lonWest: Double,
        lonEast: Double,
        zoom: Double
    ) {
        Log.d("viewchanged", "View changed: $latNorth, $latSouth, $lonWest, $lonEast")

        // Add padding to load points off screen
        val padding = 0.05
        val latNorthPadded = latNorth + padding
        val latSouthPadded = latSouth - padding
        val lonWestPadded = lonWest - padding
        val lonEastPadded = lonEast + padding

        viewModelScope.launch {

            if (hasStartedCalculationsInThread) return@launch

            // Do calculations in new thread
            val tracksData = withContext(Dispatchers.Default) {
                Log.d("debugOpti", "Started in new thread")
                hasStartedCalculationsInThread = true

                // Count waypoints in visible area
                val count = updateMapViewUseCase.getVisiblePointCount(
                    latNorthPadded, latSouthPadded, lonWestPadded, lonEastPadded
                )

                // Decide how to load points
                val decision = mapUpdateViewHelper.decide(
                    zoom,
                    count,
                    lastZoom,
                    hasDisplayedFull,
                    hasDisplayedOutline
                )

                Log.d("debugOpti", "hasDisplayedFull: $hasDisplayedFull")
                Log.d("debugOpti", "hasDisplayedOutline: $hasDisplayedOutline")

                // Update state
                lastZoom = decision.snappedZoom
                hasDisplayedFull = decision.showFull
                hasDisplayedOutline = decision.showOutline

                // Fetch tracks with chosen methode/mode
                updateMapViewUseCase(
                    latNorthPadded, latSouthPadded, lonWestPadded, lonEastPadded,
                    decision.showOutline, decision.showFull
                )
            }

            // Send new updated tracks to be displayed
            tracksData?.forEach { (trackId, waypoints) ->
                val points = waypoints.map { wp -> SimpleWaypoint(wp.id, wp.lat, wp.lng) }
                _waypointEvents.emit(WaypointUpdate.ViewChanged(trackId, points))
            }

            /*tracksData?.firstOrNull()?.let { (trackId, waypoints) ->
                val points = waypoints.map { wp -> wp.lat to wp.lng }
                _waypointEvents.emit(WaypointUpdate.ViewChanged(trackId, points))
            }*/

            hasStartedCalculationsInThread = false
            Log.d("debugOpti", "Finished in new thread")

        }


    }


}