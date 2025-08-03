package com.minapps.trackeditor.feature_map_editor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.minapps.trackeditor.R
import com.minapps.trackeditor.TrackEditorApp
import com.minapps.trackeditor.databinding.ActivityMapBinding
import com.minapps.trackeditor.feature_track_import.presentation.ImportTrackViewModel
import com.minapps.trackeditor.feature_track_import.presentation.TrackImportEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


@AndroidEntryPoint
class MapActivity : AppCompatActivity(), MapListener {

    private lateinit var mMap: MapView
    private lateinit var loadTrackButton: Button
    private lateinit var controller: IMapController
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
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
            if (isGranted) setupMyLocation()
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

        //Setup osmmap
        mMap = binding.osmmap
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.setMultiTouchControls(true)
        mMap.getLocalVisibleRect(Rect())

        controller = mMap.controller
        controller.setZoom(6.0)

        // Request location permission before enabling overlay
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            setupMyLocation()
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
                            is WaypointUpdate.Removed -> handleWaypointRemoved(event.trackId, event.index)
                            is WaypointUpdate.Cleared -> handleTrackCleared(event.trackId)
                        }
                    }
                }

                launch {
                    importTrackViewModel.events.collect { event ->
                        when (event) {
                            is TrackImportEvent.TrackAdded -> {
                                Log.d("debug", "Track imported ${event.trackId}, loading waypoints...")
                                lifecycleScope.launch {
                                    mapViewModel.loadTrackWaypoints(event.trackId)
                                }
                            }
                        }
                    }
                }
            }
        }




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
                    controller.setCenter(geoPoint)
                    controller.animateTo(geoPoint)
                }
            }
        }

        mMap.overlays.add(mMyLocationOverlay)
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        event?.source?.mapCenter?.let { center ->
            //Log.d("MapScroll", "Lat: ${center.latitude}, Lon: ${center.longitude}")
        }
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        //Log.d("MapZoom", "Zoom level: ${event?.zoomLevel}")
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
     * TODO
     *
     * Remove point in optimised way
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
     * TODO
     *
     * Remove track in optimised way
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
     * TODO
     * Function to open file picker, you call this when you want to launch
     *
     * @param mimeType
     */
    fun openFileExplorer(mimeType: String = "*/*") {
        filePicker.launch(mimeType)
    }



}