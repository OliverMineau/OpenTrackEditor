package com.minapps.trackeditor.feature_map_editor.presentation.ui

import ToolboxPopup
import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
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
import com.minapps.trackeditor.feature_map_editor.presentation.ActionDescriptor
import com.minapps.trackeditor.feature_map_editor.presentation.ActionType
import com.minapps.trackeditor.feature_map_editor.presentation.DataDestination
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
import com.minapps.trackeditor.feature_map_editor.presentation.EditState
import com.minapps.trackeditor.feature_map_editor.presentation.ProgressData
import com.minapps.trackeditor.feature_track_export.presentation.utils.showSaveFileDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

@AndroidEntryPoint
class MapActivity : AppCompatActivity(), MapListener {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private lateinit var mapRenderer: MapOverlayRenderer
    private lateinit var toolboxPopup: ToolboxPopup

    private val mapViewModel: MapViewModel by viewModels()

    private var lastUpdateTime: Long = 0L
    private var trailingJob: Job? = null
    private val updateInterval = 400L // 200ms



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
            ToolboxPopup(findViewById(R.id.popup_container), layoutInflater, lifecycleScope)

        // Start observing ViewModel state flows
        observeViewModel()

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
                return true
            }

            override fun longPressHelper(p: GeoPoint?) = false
        }
        binding.osmmap.overlays.add(MapEventsOverlay(mapEventsReceiver))
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
                R.id.nav_edit -> editNav.visibility = View.VISIBLE
                R.id.nav_add_track -> openFileExplorer()
            }

            mapRenderer.unselectPolyline(forceUpdate = true)
            mapViewModel.selectedTool(ActionType.NONE)
            true
        }

        editNav.setOnItemSelectedListener {

            mapRenderer.unselectPolyline(forceUpdate = true)

            when (it.itemId) {
                R.id.nav_toolbox -> {
                    toolboxPopup.show()
                    mapViewModel.selectTool(ActionType.NONE)
                    true
                }

                R.id.nav_add -> {
                    mapViewModel.selectTool(ActionType.ADD)
                    true
                }

                R.id.nav_remove -> {
                    mapViewModel.selectTool(ActionType.REMOVE)
                    true
                }

                else -> {
                    mapViewModel.selectedTool(ActionType.NONE)
                    toolboxPopup.hide()
                    true
                }

            }
        }
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
                        showSaveFileDialog(this@MapActivity) { fileName ->
                            mapViewModel.toolExport(fileName)
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
                        handleProgressEvent(progressData)
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
    }

    //TODO BUG progress somewhere
    fun handleProgressEvent(data: ProgressData) {

        if (data.message != null) {
            Toast.makeText(this, data.message, Toast.LENGTH_SHORT).show()
        }

        val progressBar = binding.progressBar
        val progressTextView = binding.progressTextView
        if (data.isDisplayed) {
            progressBar.visibility = View.VISIBLE
            progressTextView.visibility = View.VISIBLE
        } else {
            Log.d("debug", "Mapacti, set progress 0")
            progressBar.setProgress(0, false)
            progressTextView.text = "0%"
            progressBar.visibility = View.GONE
            progressTextView.visibility = View.GONE
            return
        }

        progressBar.setProgress(data.progress, true)
        progressTextView.text = "${data.progress}%"
    }

    private fun updateSelectedToolUI(tool: ActionType) {
        val navBinding = BottomNavigationBinding.bind(binding.root)
        val editNav = navBinding.editBottomNavigation

        if (tool.group == 1) {
            // Uncheck all menu if not type items
            for (i in 0 until editNav.menu.size) {
                val item = editNav.menu[i]

                if (item.itemId == R.id.nav_add && tool == ActionType.ADD) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_remove && tool == ActionType.REMOVE) {
                    item.isChecked = true
                    return
                } else if (item.itemId == R.id.nav_toolbox && tool != ActionType.REMOVE && tool != ActionType.ADD) {
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

        /*updateDelay(500){

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
        }*/

        scheduleUpdate()


        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        if (event == null) return true


        /*updateDelay(500) {
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
        }*/

        scheduleUpdate()


        return true
    }



    private fun updateDelay(periodMs: Long, action: () -> Unit): Boolean {
        val now = System.currentTimeMillis()
        return if (now - lastUpdateTime >= periodMs) {
            lastUpdateTime = now
            action()
            true
        } else {
            false
        }
    }

    private fun scheduleUpdate() {
        val now = System.currentTimeMillis()

        // Leading call: only if last update was more than updateInterval ago
        if (now - lastUpdateTime > updateInterval) {
            //triggerUpdate()
            lastUpdateTime = now
        }

        // Trailing call: after user stops interacting
        trailingJob?.cancel()
        trailingJob = lifecycleScope.launch {
            delay(updateInterval * 2) // wait for ms after last event
            triggerUpdate() // final update
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    binding.osmmap.controller.setCenter(geoPoint)
                    binding.osmmap.controller.animateTo(geoPoint)
                }
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
            } else {
                // Permission denied, handle accordingly (show message or fallback)
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }


}