package com.minapps.trackeditor.feature_map_editor.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.core.domain.usecase.UpdateMapViewUseCase
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

sealed class WaypointUpdate {
    data class Added(val trackId: Int, val point: Pair<Double, Double>) : WaypointUpdate()
    data class ViewChanged(val trackId: Int, val points: List<Pair<Double, Double>>) : WaypointUpdate()
    data class AddedList(val trackId: Int, val points: List<Pair<Double, Double>>) :
        WaypointUpdate()

    data class Removed(val trackId: Int, val index: Int) : WaypointUpdate()
    data class Moved(val trackId: Int, val points: List<Pair<Double, Double>>) : WaypointUpdate()
    data class MovedDone(val trackId: Int, val pointId: Int, val point: Pair<Double, Double>) :
        WaypointUpdate()

    data class Cleared(val trackId: Int) : WaypointUpdate()
}

data class ActionDescriptor(
    val icon: Int?,
    val label: String?,
    val action: UIAction?,
    val selectionCount: Int?,
    val type: ActionType,
    val group: Int?
)

enum class ActionType(
    val icon: Int?,
    val label: String?,
    val selectionCount: Int?,
    val group: Int? = 0
) {
    // No action
    NONE(null, null, null),
    SPACER(null, null, null),

    // File & system actions
    EXPORT(R.drawable.file_export_24, "Export", 1, 2),
    SCREENSHOT(R.drawable.mode_landscape_24, "Screenshot", 1, 2),
    CLEAR(R.drawable.trash_24, "Clear", 1, 2),

    // Visual tools
    ELEVATION(R.drawable.curve_arrow_24, "Elevation", 1),
    LAYERS(R.drawable.land_layers_24, "Layers", 1),

    // Editing tools
    REVERSE(R.drawable.rotate_reverse_24, "Reverse", -1, 1),
    REMOVE_DUPS(R.drawable.circle_overlap_24, "Remove dups", -1, 1),
    REMOVE_BUGS(R.drawable.bug_slash_24, "Remove bugs", -1, 1),
    CUT(R.drawable.scissors_24, "Cut", -1, 1),
    JOIN(R.drawable.link_alt_24, "Join", -1, 1),
    REDUCE_NOISE(R.drawable.noise_cancelling_headphones_24, "Reduce noise", -1, 1),
    FILTER(R.drawable.filter_24, "Filter", -1, 1),
    MAGIC_FILTER(R.drawable.sweep_24, "Magic filter", -1, 1),

    // Edit Bottom Navigation
    ADD(R.drawable.map_marker_plus_24, "Add", -1, 1),
    REMOVE(R.drawable.map_marker_minus_24, "Remove", -1, 1),

}

data class EditState(
    val currentSelectedTool: ActionType = ActionType.NONE,
    val currentselectedTrack: Int? = null,
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
            repository.addedTracks.collect { trackId ->
                //If added load waypoints of given track id
                loadTrackWaypoints(trackId)
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


    //TODO Temporary
    fun selectTool(action: ActionType) {
        selectedTool(action)
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
                    currentselectedTrack = null, version = System.nanoTime()
                )
            }
        }

        Log.d("debug", "Selected ${action.label}")

        viewModelScope.launch {
            when (action) {
                ActionType.EXPORT -> toolExportTrack()
                ActionType.SCREENSHOT -> toolGetScreenshot()

                else -> Log.d("debug", "Not Implemented")
            }
        }
    }




    /*fun toolExport(name: String) {

        viewModelScope.launch {

            /*val trackId = editState.value.currentselectedTrack ?: return@launch
            val track = getFullTrackUseCase(trackId)
            if (track == null || track.waypoints.isEmpty()) {
                Toast.makeText(context, "No waypoints to export", Toast.LENGTH_SHORT).show()
                Log.d("debug", "No waypoints to export")
                return@launch
            }

            // Convert to GPX
            val exporter = GpxExporter()
            val gpxString = exporter.export(track)

            val uri = FileExporter.saveTextAndGetUri(context, name, gpxString)

            Toast.makeText(context, "Exported to : $uri", Toast.LENGTH_SHORT).show()*/

        }

    }*/

    private suspend fun toolExportTrack() {
        Log.d("debug", "Exporting Track Fun")

        // Emit event to UI to show dialog with default filename
        _showExportDialog.emit("track.gpx")


        /*val trackId = editState.value.currentselectedTrack ?: return
        val track = getFullTrackUseCase(trackId)
        if (track == null || track.waypoints.isEmpty()) {
            Toast.makeText(context, "No waypoints to export", Toast.LENGTH_SHORT).show()
            Log.d("debug", "No waypoints to export")
            return
        }

        // Convert to GPX
        val exporter = GpxExporter()
        val gpxString = exporter.export(track)

        FileExporter.promptFileNameAndExport(context,gpxString)
        // Save & Share
        //val uri = FileExporter.saveTextAndGetUri(context, "track_$trackId.gpx", gpxString)
        //FileExporter.shareFile(context, uri, "application/gpx+xml")

        //Log.d("debug", "Exported to $uri")
        //Toast.makeText(context, "Exported to : $uri", Toast.LENGTH_SHORT).show()
    */
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

        val selectedTrackId = editState.value.currentselectedTrack
        val currentTool = editState.value.currentSelectedTool

        // If no track is selected, do nothing
        if (selectedTrackId == null) return

        // If not Add as selected tool
        if (currentTool != ActionType.ADD) return


        viewModelScope.launch {

            val currentIndex =
                trackWaypointIndexes[selectedTrackId] ?: (getTrackLastWaypointIndexUseCase(
                    selectedTrackId
                ) + 1.0)

            addWaypointUseCase(lat, lng, currentIndex.toDouble(), selectedTrackId)
            // Notify observers that a waypoint was added
            _waypointEvents.emit(WaypointUpdate.Added(selectedTrackId, lat to lng))
            // Update index only after successful addition
            trackWaypointIndexes[selectedTrackId] = currentIndex + 1
        }
    }


    /**
     * Create new track entity if no track is selected
     *
     * @return Track id (Never Null)
     */
    suspend fun createNewTrackIfNeeded(): Int {

        var selectedTrackId = editState.value.currentselectedTrack

        // If no track selected
        if (selectedTrackId == null) {
            //Set default track name
            val newId = createTrackUseCase(context.getString(R.string.track_no_name))

            selectedTrackId = newId
            trackWaypointIndexes[selectedTrackId] = 0.0
            _editState.update {
                it.copy(
                    currentselectedTrack = selectedTrackId,
                    version = System.nanoTime()
                )
            }

            Log.d("debug", "Created new track with ID: $newId")
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
        val trackId = createNewTrackIfNeeded()
        addWaypoint(geoPoint.latitude, geoPoint.longitude)
        Log.d(
            "debug",
            "Added waypoint to track $trackId at: ${geoPoint.latitude}, ${geoPoint.longitude}"
        )
    }


    /**
     * Reemit imported track to flow listened to by mapActivity
     *
     * @param trackId
     */
    suspend fun loadTrackWaypoints(trackId: Int) {
        val waypoints = getTrackWaypointsUseCase(trackId, null, null, null, null)

        if (waypoints.isNotEmpty()) {
            //Reemit points for mapActivity
            val points = waypoints.map { wp -> Pair(wp.lat, wp.lng) }
            _waypointEvents.emit(WaypointUpdate.AddedList(trackId, points))
        }
    }

    /**
     * Send coord to activity and to database if set
     *
     * @param waypointList
     * @param pointId
     * @param isPointSet
     */
    fun movePoint(waypointList: List<Waypoint>, pointId: Int?, isPointSet: Boolean = false) {

        //val currentTool = editState.value.currentSelectedTool
        //if(currentTool == ActionType.NONE)

        viewModelScope.launch {
            val trackId = waypointList.first().trackId

            // Point is set, update database
            if (isPointSet && pointId != null) {
                addWaypointUseCase(
                    waypointList.first().lat,
                    waypointList.first().lng,
                    pointId.toDouble(),
                    trackId = trackId
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

    /**
     * Updates selected track
     *
     * @param trackId
     * @param vibrate
     */
    fun selectedTrack(trackId: Int?, vibrate: Boolean = false) {
        Log.d("debug", "Selected track $trackId")
        _editState.update { it.copy(currentselectedTrack = trackId, version = System.nanoTime()) }

        if (vibrate) {
            context.vibrate(30)
        }
    }

    fun toolExport(fileName: String) {

        val format = ExportFormat.GPX

        viewModelScope.launch {
            val trackId = editState.value.currentselectedTrack
            if (trackId == null) {
                _exportResult.emit(Result.failure(Exception("No track selected")))
                return@launch
            }

            Log.d("debug", "Progress show")
            _progressState.update { it.copy(isDisplayed = true) }

            exportTrackUseCase(trackId, fileName, format).collect { exportResult ->
                when(exportResult){
                    is DataStreamProgress.Completed -> {
                        Log.d("debug", "export success, mapvm")
                        Log.d("debug", "Progress close")
                        _progressState.update { it.copy(isDisplayed = false, message = "Track exported successfully !") }
                    }
                    is DataStreamProgress.Error -> {
                        //_exportResult.emit(Result.failure(exportResult.message))
                        Log.d("debug", "export fail : ${exportResult.message}")
                        _progressState.update { it.copy(isDisplayed = false, message = "Exporting track failed.") }
                    }
                    is DataStreamProgress.Progress -> {
                        Log.d("debug", "export progress ${exportResult.percent}, mapvm")
                        _progressState.update { it.copy(progress = exportResult.percent, message = null) }
                    }
                }
            }
        }
    }

    fun importTrack(uri: Uri) {

        viewModelScope.launch {
            trackImportUseCase(uri).collect { importProgress ->
                _progressState.update { it.copy(isDisplayed = true) }

                when(importProgress){
                    is DataStreamProgress.Completed -> {
                        val displayed = displayTrackUseCase(importProgress.trackId)
                        _progressState.update { it.copy(isDisplayed = false, message = "Imported track successfully") }
                        Log.d("debug", "VM received finished, trackID:${importProgress.trackId}, displayed:$displayed")
                    }
                    is DataStreamProgress.Error -> {
                        Log.d("debug", "VM received ERROR! ${importProgress.message}")
                        _progressState.update { it.copy(isDisplayed = false, message = "Importing failed") }
                    }
                    is DataStreamProgress.Progress -> {
                        Log.d("debug", "VM received progress : ${importProgress.percent}%")
                        _progressState.update { it.copy(progress = importProgress.percent, message = null) }
                    }
                }
            }
        }
    }

    private var changed = false

    // TODO replace track in view and set new visible track
    fun viewChanged(latNorth: Double, latSouth: Double, lonWest: Double, lonEast: Double, zoom: Double){

        Log.d("viewchanged", "View changed : $latNorth, $latSouth, $lonWest, $lonEast")

        // TODO get list of all visible tracks and their waypoints
        //  and display them

        // Add padding
        val padding = 0.009   // ~1 km padding (depends on latitude!)

        val latNorthPadded = latNorth + padding
        val latSouthPadded = latSouth - padding
        val lonWestPadded = lonWest - padding
        val lonEastPadded = lonEast + padding

        var updateZoom = false
        val step = 3
        val snappedZoom = (zoom.toInt() / step) * step
        if (lastZoom == null || lastZoom!!.toInt() != snappedZoom) {
            lastZoom = snappedZoom
            updateZoom = true
            changed = true
            Log.d("ZOOM", "ZOOM updating full track, zoom at : $lastZoom")
        }


        viewModelScope.launch {

            Log.d("opti", "LOADING NEW TRACK")
            if(changed && updateMapViewUseCase.getVisiblePointCount(latNorthPadded, latSouthPadded, lonWestPadded, lonEastPadded) >= 10000){
                updateZoom = true
                changed = false
            }
            val tracks = updateMapViewUseCase(latNorthPadded, latSouthPadded, lonWestPadded, lonEastPadded, updateZoom)

            if(tracks != null && tracks.isNotEmpty()){

                val trackId = tracks.first().first
                val waypoints = tracks.first().second
                val points = waypoints.map { wp -> Pair(wp.lat, wp.lng) }
                _waypointEvents.emit(WaypointUpdate.ViewChanged(trackId, points))
            }
            /*if (waypoints.isNotEmpty()) {
                //Reemit points for mapActivity
                val points = waypoints.map { wp -> Pair(wp.lat, wp.lng) }
                _waypointEvents.emit(WaypointUpdate.AddedList(trackId, points))
            }*/
        }



    }


}