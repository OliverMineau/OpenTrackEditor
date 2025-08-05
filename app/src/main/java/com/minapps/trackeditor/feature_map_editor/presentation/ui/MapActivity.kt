package com.minapps.trackeditor.feature_map_editor.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.minapps.trackeditor.R
import com.minapps.trackeditor.databinding.ActivityMapBinding
import com.minapps.trackeditor.feature_map_editor.presentation.overlay.MapOverlayRenderer
import com.minapps.trackeditor.feature_map_editor.presentation.MapViewModel
import com.minapps.trackeditor.feature_map_editor.presentation.WaypointUpdate
import com.minapps.trackeditor.feature_track_import.presentation.ImportTrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MapActivity : AppCompatActivity(), MapListener {

    private lateinit var mMap: MapView
    private lateinit var loadTrackButton: Button
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private lateinit var mapRenderer: MapOverlayRenderer

    private val mapViewModel: MapViewModel by viewModels()
    private val importTrackViewModel: ImportTrackViewModel by viewModels()

    //When file picked call viewmodel's importTrack
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            importTrackViewModel.importTrack(it)
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
            //if (isGranted) setupMyLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load osmdroid configuration *before* setContentView
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )

        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup osmmap
        mMap = binding.osmmap
        mapRenderer = MapOverlayRenderer(mMap, mapViewModel)
        mapRenderer.setSettings()

        // Request location permission before enabling overlay
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            //setupMyLocation()
        }

        mMap.addMapListener(this)

        //Bind button to openFile
        loadTrackButton = binding.loadTrackBtn
        loadTrackButton.setOnClickListener {
            Log.d("debug", "Loading file")
            openFileExplorer()
        }

        // Add map click listener that calls viewmodel
        val mapEventsReceiver = object : MapEventsReceiver {
            //map to mapViewModel singleTap
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    lifecycleScope.launch {
                        mapViewModel.singleTapConfirmed(it)
                    }
                }
                return true
            }

            //map to mapViewModel longPress
            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        val overlayEvents = MapEventsOverlay(mapEventsReceiver)
        mMap.overlays.add(overlayEvents)

        //Optimise track display updates
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                //Safely collect waypoint events from the ViewModel, only while the Activity is visible
                launch {
                    mapViewModel.waypointEvents.collect { event ->
                        when (event) {
                            is WaypointUpdate.Added -> handleWaypointAdded(event.trackId, event.point)
                            is WaypointUpdate.AddedList -> handleWaypointAddedList(event.trackId, event.points)
                            is WaypointUpdate.Removed -> handleWaypointRemoved(event.trackId, event.index)
                            is WaypointUpdate.Moved -> handleWaypointMoved(event.trackId, event.points)
                            is WaypointUpdate.Cleared -> handleTrackCleared(event.trackId)
                            is WaypointUpdate.MovedDone -> handleWaypointMovedDone(event.trackId, event.pointId, event.point)
                        }
                    }
                }
            }
        }

        //Todo Remove (for testing)
        testDisplayWaypoints()

    }

    /**
     * Setup location of user
     */
    private fun setupMyLocation() {
        mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mMap)
        mMyLocationOverlay.enableMyLocation()
        mMyLocationOverlay.enableFollowLocation()
        mMyLocationOverlay.isDrawAccuracyEnabled = true

        mMyLocationOverlay.runOnFirstFix {
            runOnUiThread {
                mMyLocationOverlay.myLocation?.let { loc ->
                    val geoPoint = GeoPoint(loc.latitude, loc.longitude)
                    /*controller.setCenter(geoPoint)
                    controller.animateTo(geoPoint)*/
                }
            }
        }

        mMap.overlays.add(mMyLocationOverlay)
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        event?.source?.mapCenter?.let { center ->
        }
        return true
    }

    /**
     * TODO Trigger largeFileDisplayMethods
     *
     * @param event
     * @return
     */
    override fun onZoom(event: ZoomEvent?): Boolean {
        return true
    }


    /**
     * TODO : Change for selection of predetermined visible colors
     * Get random color for track
     *
     * @param trackId
     * @return Color (Int)
     */
    private fun randomColorForTrack(trackId: Int): Int {
        // Return some distinct colors per trackId
        // Or use a fixed palette cycling through colors
        return when(trackId % 5) {
            0 -> Color.BLUE
            1 -> Color.RED
            2 -> Color.GREEN
            3 -> Color.MAGENTA
            else -> Color.CYAN
        }
    }

    /**
     * TODO
     * Add point to polyline in optimised way
     *
     * @param trackId
     * @param point
     */
    private fun handleWaypointAdded(trackId: Int, point: Pair<Double, Double>) {
        val polyline = polylines.getOrPut(trackId) {
            Polyline().apply {
                outlinePaint.color = randomColorForTrack(trackId)
                outlinePaint.strokeWidth = 8f
                mMap.overlays.add(this)
            }
        }
        polyline.addPoint(GeoPoint(point.first, point.second))
        mMap.invalidate()
    }


    /**
     * Render list of points
     *
     * @param trackId
     * @param points
     */
    private fun handleWaypointAddedList(trackId: Int, points: List<Pair<Double, Double>>){
        mapRenderer.displayTrack(points, trackId, Color.RED, true)
    }

    /**
     * Render moved point
     *
     * @param trackId
     * @param points
     */
    private fun handleWaypointMoved(trackId: Int, points: List<Pair<Double, Double>>){
        mapRenderer.displayLiveModification(points, trackId, Color.rgb(255,128,0))
    }

    /**
     * Render final point move
     *
     * @param trackId
     * @param pointId
     * @param point
     */
    private fun handleWaypointMovedDone(trackId: Int, pointId: Int, point: Pair<Double, Double>){
        mapRenderer.displayLiveModificationDone(point, trackId, pointId)
    }

    /**
     * Remove point
     *
     * @param trackId
     * @param index
     */
    private fun handleWaypointRemoved(trackId: Int, index: Int) {
        polylines[trackId]?.apply {
            actualPoints.removeAt(index)
            mMap.invalidate()
        }
    }

    /**
     * Remove track
     *
     * @param trackId
     */
    private fun handleTrackCleared(trackId: Int) {
        polylines[trackId]?.let {
            mMap.overlays.remove(it)
            polylines.remove(trackId)
            mMap.invalidate()
        }
    }

    /**
     * Function to open file picker
     *
     * @param mimeType
     */
    fun openFileExplorer(mimeType: String = "*/*") {
        filePicker.launch(mimeType)
    }



















    //TODO Testing in progress

    fun testDisplayWaypoints() {

        // Sample list of waypoints
        val testPoints = listOf(
            48.8566 to 2.3522,    // Paris
            51.5074 to -0.1278,   // London
            52.52 to 13.4050      // Berlin
        )

        val polylineColor = Color.RED

        // Call the display function
        mapRenderer.displayTrack(
            waypoints = testPoints,
            trackId = 0,
            color = polylineColor,
            center = true
        )
    }



}