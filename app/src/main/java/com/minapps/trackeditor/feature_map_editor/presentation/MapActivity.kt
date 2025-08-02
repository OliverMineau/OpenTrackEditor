package com.minapps.trackeditor.feature_map_editor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.minapps.trackeditor.R
import com.minapps.trackeditor.TrackEditorApp
import com.minapps.trackeditor.databinding.ActivityMapBinding
import dagger.hilt.android.AndroidEntryPoint
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MapActivity : AppCompatActivity(), MapListener {

    private lateinit var mMap: MapView
    private lateinit var controller: IMapController
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private val mapViewModel: MapViewModel by viewModels()
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








        // Setup repository + use case + viewmodel manually (DI later)
        //val app = application as TrackEditorApp
        //mapViewModel = app.dependencyProvider.provideMapViewModel()

        mapViewModel.clearAll()

        // Add map click listener
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    mapViewModel.addWaypoint(it.latitude, it.longitude)
                    addMarker(it)  // ✅ Draw marker on map
                    Log.d("Waypoint", "Added waypoint at: ${it.latitude}, ${it.longitude}")
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }

        val overlayEvents = MapEventsOverlay(mapEventsReceiver)
        mMap.overlays.add(overlayEvents)





    }


    override fun onRestart() {
        super.onRestart()

        //mapViewModel.loadWaypoints()
    }

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
            Log.d("MapScroll", "Lat: ${center.latitude}, Lon: ${center.longitude}")
        }
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        Log.d("MapZoom", "Zoom level: ${event?.zoomLevel}")
        return true
    }

    private fun addMarker(point: GeoPoint) {
        val marker = Marker(mMap)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mMap.overlays.add(marker)
        mMap.invalidate()
    }
}