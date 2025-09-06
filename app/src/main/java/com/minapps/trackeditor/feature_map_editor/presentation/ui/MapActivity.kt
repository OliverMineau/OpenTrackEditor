package com.minapps.trackeditor.feature_map_editor.presentation.ui

import ToolboxPopup
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.minapps.trackeditor.R
import com.minapps.trackeditor.databinding.ActivityMapBinding
import com.minapps.trackeditor.databinding.BottomNavigationBinding
import com.minapps.trackeditor.feature_map_editor.presentation.overlay.MapOverlayRenderer
import com.minapps.trackeditor.feature_map_editor.presentation.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.core.view.size
import androidx.core.view.get
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.tool.ToolDialog
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.domain.model.ProgressData
import com.minapps.trackeditor.core.domain.type.ActionType
import com.minapps.trackeditor.core.domain.type.DataDestination
import com.minapps.trackeditor.core.domain.util.ToolGroup
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.ToolResultListener
import com.minapps.trackeditor.feature_map_editor.presentation.model.ActionDescriptor
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_settings.presentation.SettingsFragment
import com.minapps.trackeditor.feature_track_export.presentation.util.showSaveFileDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MapActivity : AppCompatActivity(), MapListener, ToolUiContext, ToolResultListener {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private lateinit var mapRenderer: MapOverlayRenderer
    private lateinit var toolboxPopup: ToolboxPopup

    private val mapViewModel: MapViewModel by viewModels()

    private var lastUpdateTime: Long = 0L
    private var trailingJob: Job? = null
    private val updateInterval = 400L // 200ms
    private val fallbackGeoPoint = GeoPoint(45.71232, 5.12749)
    private var zoomToEgg = false



    //When file picked call viewmodel's importTrack
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            //importTrackViewModel.importTrack(it)
            mapViewModel.importTrack(it)
        }
    }

    /**
     * TODO :
     * Reduce the number of points on the poly lines using the douglas pucker algorithm
     * and then dynamically readjust as needed when the map zooms in and out. Also,
     * you can clip the polyline at the bounds of the screen, then as the user pans,
     * rerender the lines based on the new extent. Lines that are completely out of
     * the view can then be removed from the overlay manager. Using these mechanisms,
     * you can get pretty good performance with a lot of overlays
     */
    private val polylines = mutableMapOf<Int, Polyline>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) centerMapOnMyLocationOnce()
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userAgentValue = "com.minapps.trackeditor"
        Configuration.getInstance().userAgentValue = userAgentValue


        // Inflate layout
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize MapRenderer with the MapView instance
        mapRenderer = MapOverlayRenderer(binding.osmmap, mapViewModel)
        mapRenderer.setSettings()
        binding.osmmap.addMapListener(this)

        // Request location permission here or check it
        requestLocationPermissionIfNeeded()

        // Setup map event listeners (tap, long press)
        setupMapClickListener()

        // Setup bottom navigation UI and event listeners
        setupBottomNav()

        // Setup the zoom
        setupZoom()

        // Initialize toolbox popup
        toolboxPopup =
            ToolboxPopup(
                this,
                findViewById(R.id.popup_container),
                layoutInflater,
                lifecycleScope,
                this
            )

        // Start observing ViewModel state flows
        observeViewModel()

        handleIntent(intent)

    }


    private fun requestLocationPermissionIfNeeded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            centerMapOnMyLocationOnce()
        }
    }

    private fun setupMapClickListener() {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    lifecycleScope.launch {
                        mapViewModel.singleTapConfirmed(it)
                    }
                }
                zoomToEgg = false
                return true
            }

            override fun longPressHelper(p: GeoPoint?) : Boolean{
                zoomToEgg = false
                return true
            }
        }
        binding.osmmap.overlays.add(MapEventsOverlay(mapEventsReceiver))

        binding.osmmap.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("OSM", "User touched the map")
                    zoomToEgg = false
                    v.performClick()
                }
            }
            false
        }

    }


    private fun setupBottomNav() {
        val navBinding = BottomNavigationBinding.bind(binding.root)
        val mainNav = navBinding.mainBottomNavigation
        val editNav = navBinding.editBottomNavigation

        mainNav.setOnItemSelectedListener {

            editNav.visibility = View.GONE
            toolboxPopup.hide()

            // Inform ViewModel of navigation selection or do UI changes like showing/hiding editNav
            when (it.itemId) {
                R.id.nav_edit -> {
                    editNav.visibility = View.VISIBLE
                    mapViewModel.onToolSelected(ActionType.EDIT)
                    editNav.post { clearBottomNavSelection(editNav) }
                    hideSettings()
                }

                R.id.nav_add_track -> {
                    openFileExplorer()
                    mapViewModel.onToolSelected(ActionType.NONE)
                    hideSettings()
                }

                R.id.nav_view -> {
                    mapViewModel.onToolSelected(ActionType.VIEW)
                    hideSettings()
                }

                R.id.nav_settings -> {
                    mapViewModel.onToolSelected(ActionType.NONE)
                    val fragment = SettingsFragment()

                    // Load the fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.settings_container, fragment)
                        .addToBackStack(null) // optional, for back navigation
                        .commit()
                }
            }
            mapRenderer.selectTrack(null, false)
            true
        }

        editNav.setOnItemSelectedListener {

            //mapRenderer.selectTrack(null, false)

            when (it.itemId) {
                R.id.nav_toolbox -> {
                    val wasShown = toolboxPopup.show()
                    if (wasShown == 1) {
                        mapViewModel.onToolSelected(ActionType.TOOLBOX)
                        false
                    } else if (wasShown == 0) {
                        mapViewModel.onToolSelected(ActionType.TOOLBOX)
                        //clearBottomNavSelection(editNav)
                        false
                    }
                    false
                }

                R.id.nav_add -> {
                    mapViewModel.onToolSelected(ActionType.ADD)
                    true
                }

                R.id.nav_remove -> {
                    mapViewModel.onToolSelected(ActionType.REMOVE)
                    true
                }

                R.id.nav_hand -> {
                    mapViewModel.onToolSelected(ActionType.HAND)
                    true
                }

                R.id.nav_selection -> {
                    mapViewModel.onToolSelected(ActionType.SELECT)
                    true
                }

                else -> {
                    mapViewModel.onToolSelected(ActionType.NONE)
                    toolboxPopup.hide()
                    true
                }

            }
        }
    }

    private fun hideSettings(){
        val fragment = supportFragmentManager.findFragmentById(R.id.settings_container)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
    }

    private fun clearBottomNavSelection(bottomNav: BottomNavigationView) {
        val menu = bottomNav.menu
        if (menu.size == 0) return
        val groupId = menu[0].groupId

        // Temporarily allow multiple check so we can uncheck all
        menu.setGroupCheckable(groupId, true, false)
        for (i in 0 until menu.size) {
            menu[i].isChecked = false
        }
        // Restore exclusive single-check behaviour
        menu.setGroupCheckable(groupId, true, true)
    }


    private fun observeViewModel() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                //Safely collect waypoint events from the ViewModel, only while the Activity is visible
                launch {
                    mapViewModel.waypointEvents.collectLatest { event ->
                        mapRenderer.handleWaypointEvents(event)
                    }
                }

                launch {
                    mapViewModel.actions.collect { actionMap ->
                        handleActionEvents(actionMap)
                    }
                }

                launch {
                    mapViewModel.editState.collect { state ->
                        handleEditStateEvents(state)
                    }
                }

                launch {
                    mapViewModel.showExportDialog.collect { defaultFilename ->
                        showSaveFileDialog(this@MapActivity) { fileName, exportFormat, exportAll ->
                            mapViewModel.toolExport(fileName, exportFormat)
                        }
                    }
                }

                launch {
                    mapViewModel.exportResult.collect { result ->
                        result.fold(
                            onSuccess = { uri ->
                                Toast.makeText(
                                    this@MapActivity,
                                    "Exported to: $uri",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { error ->
                                Toast.makeText(
                                    this@MapActivity,
                                    "Export failed: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                launch {
                    mapViewModel.progressState.collect { progressData ->
                        //handleProgressEvent(progressData)
                        handleLoadingEvent(progressData)
                    }
                }

                launch {
                    mapViewModel.toastEvents.collect { message ->
                        showToastEvent(message)
                    }
                }
            }
        }
    }

    fun handleActionEvents(event: Map<DataDestination, List<ActionDescriptor>>) {
        event.forEach { (destination, actions) ->
            when (destination) {
                DataDestination.EDIT_TOOL_POPUP -> handleToolAdded(actions)
                DataDestination.EDIT_BOTTOM_NAV -> return
            }
        }
    }

    /**
     * Function used when listening for action updates (Tool usecases)
     *
     * @param actions
     */
    fun handleToolAdded(actions: List<ActionDescriptor>) {
        toolboxPopup.menuItems = actions
    }

    fun handleEditStateEvents(event: EditState) {
        toolboxPopup.toolSelected(event.currentSelectedTool)
        updateSelectedToolUI(event.currentSelectedTool)

        if (event.currentSelectedTool != ActionType.SELECT &&
            event.currentSelectedTool != ActionType.TOOLBOX
        ) {
            mapRenderer.clearAllSelections()
        }

        if (event.currentSelectedTracks.isEmpty()) {
            mapRenderer.deselectTracks()
        }
    }


    fun handleLoadingEvent(data: ProgressData) {
        val progressBar = binding.progressBar
        val progressTextView = binding.progressTextView

        val show = data.isDisplayed

        if (show) {
            if (!data.isDeterminate) {
                // Indeterminate mode
                progressBar.isIndeterminate = true
                progressTextView.text = data.message
                progressTextView.visibility = View.VISIBLE
            } else {
                // Determinate mode
                progressBar.isIndeterminate = false
                progressBar.setProgress(data.progress, true)

                val text =
                    "${if (data.message == null) "" else "${data.message} - "}${data.progress}%"
                progressTextView.text = text
                progressTextView.visibility = View.VISIBLE
            }

            progressBar.visibility = View.VISIBLE

        } else {
            progressTextView.visibility = View.GONE
            progressBar.isIndeterminate = false
            progressBar.setProgress(0, false)
            progressBar.visibility = View.GONE

        }
    }


    private fun updateSelectedToolUI(tool: ActionType) {
        val navBinding = BottomNavigationBinding.bind(binding.root)
        val editNav = navBinding.editBottomNavigation
        val mainNav = navBinding.mainBottomNavigation

        if (tool.group == ToolGroup.TRACK_EDITING) {
            // Uncheck all menu if not type items
            for (i in 0 until editNav.menu.size) {
                val item = editNav.menu[i]

                if (item.itemId == R.id.nav_add && tool == ActionType.ADD) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_remove && tool == ActionType.REMOVE) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_toolbox && tool != ActionType.REMOVE && tool != ActionType.ADD && tool != ActionType.VIEW && tool != ActionType.SELECT) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_hand && tool == ActionType.HAND) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_selection && tool == ActionType.SELECT) {
                    item.isChecked = true
                    return
                }
            }
            for (i in 0 until mainNav.menu.size) {
                val item = mainNav.menu[i]

                if (item.itemId == R.id.nav_view && tool == ActionType.VIEW) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_edit && tool == ActionType.EDIT) {
                    item.isChecked = true
                    return
                }
            }
        }
    }

    fun openFileExplorer(mimeType: String = "*/*") {
        filePicker.launch(mimeType)
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        if (event == null) return true
        scheduleUpdate()
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        if (event == null) return true
        scheduleUpdate()
        return true
    }

    /**
     * Merge updates from scroll and zoom
     *
     */
    private fun scheduleUpdate() {

        // Only call if last update was more than updateInterval ago
        // For smoothish updates (disabled for now, I have to optimise loading points before)
        /*
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime > updateInterval) {
            triggerUpdate()
            lastUpdateTime = now
        }*/

        // Call when scrolling and zooming stopped
        trailingJob?.cancel()
        trailingJob = lifecycleScope.launch {
            delay(updateInterval * 2)
            triggerUpdate()
            lastUpdateTime = System.currentTimeMillis()
        }
    }

    private fun triggerUpdate() {
        val latNorth = binding.osmmap.boundingBox.latNorth
        val latSouth = binding.osmmap.boundingBox.latSouth
        val lonWest = binding.osmmap.boundingBox.lonWest
        val lonEast = binding.osmmap.boundingBox.lonEast

        mapViewModel.viewChanged(
            latNorth,
            latSouth,
            lonWest,
            lonEast,
            binding.osmmap.zoomLevelDouble
        )
    }

    fun setupZoom() {
        binding.plusBtn.setOnClickListener {
            if(zoomToEgg){
                binding.osmmap.controller.setCenter(fallbackGeoPoint)
            }
            binding.osmmap.controller.zoomIn()
        }
        binding.minusBtn.setOnClickListener {
            binding.osmmap.controller.zoomOut()
        }
    }

    private fun centerMapOnMyLocationOnce() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted, get location
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Check if location is enabled
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isLocationEnabled) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val geoPoint = GeoPoint(it.latitude, it.longitude)
                        binding.osmmap.controller.setCenter(geoPoint)
                        binding.osmmap.controller.animateTo(geoPoint)
                    }
                }
            } else {
                // Fallback location if GPS is OFF
                zoomToEgg = true
                binding.osmmap.controller.setCenter(fallbackGeoPoint)
                binding.osmmap.controller.animateTo(fallbackGeoPoint)
            }



        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerMapOnMyLocationOnce()
            }else{
                val geoPoint = GeoPoint(45.71232, 5.12749)
                binding.osmmap.controller.setCenter(geoPoint)
                binding.osmmap.controller.animateTo(geoPoint)
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Only process VIEW intents
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                try {
                    mapViewModel.importTrack(uri)
                } catch (e: Exception) {
                    Log.e("MapActivity", "Failed to read GPX file", e)
                }
            }
        }
    }

    fun showToastEvent(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    /************* EditorTool interface Functions *************/

    /**
     *  Called when tool clicked
     *
     * @param T
     * @param dialog
     * @return
     */
    override suspend fun <T : Any> showDialog(dialog: ToolDialog<T>): T? {
        return dialog.show(this)
    }

    /**
     * Called when tool error/finished
     *
     * @param message
     */
    override fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun getEditState(): EditState {
        return mapViewModel.editState.value
    }

    override fun onToolResult(tool: ActionType, result: Any?) {
        lifecycleScope.launch {
            mapViewModel.onToolResult(tool, result)
        }
    }


}