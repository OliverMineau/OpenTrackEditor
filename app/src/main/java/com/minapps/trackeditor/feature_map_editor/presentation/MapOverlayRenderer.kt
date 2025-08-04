package com.minapps.trackeditor.feature_map_editor.presentation

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.Rect
import android.util.Log
import com.minapps.trackeditor.feature_map_editor.presentation.listeners.PointInteractionListener
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme


class MapOverlayRenderer (private val mMap : MapView, private val mapViewModel: MapViewModel) : PointInteractionListener {

    private val controller: IMapController = mMap.controller

    private val displayedPolylines: MutableMap<Int, Polyline> = mutableMapOf()
    private val polylinesOverlay: MutableMap<Int, CustomSimpleFastPointOverlay> = mutableMapOf()
    private val modifyingPolylines: MutableMap<Int, Polyline> = mutableMapOf()

    fun setSettings(){
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.setMultiTouchControls(true)
        mMap.getLocalVisibleRect(Rect())
        controller.setZoom(6.0)
        mMap.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMap.setHorizontalMapRepetitionEnabled(true);
        mMap.setVerticalMapRepetitionEnabled(false);
        mMap.setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0);
        mMap.minZoomLevel =3.0
    }

    fun displayTrack(
        waypoints: List<Pair<Double, Double>>,
        trackId: Int,
        color: Int,
        center: Boolean
    ): Pair<Polyline, List<Marker>> {

        val polylineAndMarker = displayWaypoints(
            waypoints = waypoints,
            trackId = 0,
            color = color,
            center = center
        )

        displayedPolylines[trackId] = polylineAndMarker.first

        return polylineAndMarker
    }

    fun displayLiveModification(
        waypoints: List<Pair<Double, Double>>,
        trackId: Int,
        color: Int,
        center: Boolean
    ): Pair<Polyline, List<Marker>> {

        val polylineAndMarker = displayWaypoints(
            waypoints = waypoints,
            trackId = 0,
            color = color,
            center = center,
            isModifying = true
        )

        modifyingPolylines[trackId] = polylineAndMarker.first

        return polylineAndMarker
    }


    private fun displayWaypoints(
        waypoints: List<Pair<Double, Double>>,
        trackId: Int,
        color: Int,
        center: Boolean,
        isModifying: Boolean = false
    ): Pair<Polyline, List<Marker>> {

        val geoPoints = mutableListOf<GeoPoint>()
        val markers = mutableListOf<Marker>()
        val labelledPoints = mutableListOf<IGeoPoint>()

        waypoints.forEachIndexed { index, waypoint ->
            val geoPoint = GeoPoint(waypoint.first, waypoint.second)
            geoPoints.add(geoPoint)

            // Optional: You can assign names based on index or ID if needed
            /**val marker = Marker(map).apply {
            title = "Waypoint ${waypoint.id.toInt()}" // or index
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
            markers.add(marker)*/

            // Interactable labelled point
            val labelled = LabelledGeoPoint(waypoint.first, waypoint.second, "").apply {
                label = index.toString()
            }
            labelledPoints.add(labelled)
        }

        // Create polyline with style
        val polyline = Polyline().apply {
            setPoints(geoPoints)
            id = System.identityHashCode(this).toString()
            outlinePaint.apply {
                this.color = color

                // Optional: Use dashed style if needed
                val pathShape = Path().apply {
                    moveTo(-5f, -5f)
                    lineTo(5f, -5f)
                    lineTo(5f, 5f)
                    lineTo(-5f, 5f)
                }
                pathEffect = PathDashPathEffect(pathShape, 12f, 0f, PathDashPathEffect.Style.ROTATE)
            }
        }
        mMap.overlayManager.add(polyline)

        // Point overlay styles
        val primaryPaint = Paint().apply {
            editColor(color, 0, 0.70f, 0.60f)
        }
        val selectedPaint = Paint().apply {
            editColor(color, 0, 0.25f, 0.40f)
        }

        if(!isModifying){
            val pointTheme = SimplePointTheme(labelledPoints, false)
            val pointOptions = SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setRadius(10f)
                .setIsClickable(true)
                .setCellSize(15)
                .setPointStyle(primaryPaint)
                .setSelectedPointStyle(selectedPaint)
                .setSymbol(SimpleFastPointOverlayOptions.Shape.CIRCLE)

            //val pointOverlay = SimpleFastPointOverlay(pointTheme, pointOptions)
            val pointOverlay = CustomSimpleFastPointOverlay(pointTheme, pointOptions, this, trackId)
            mMap.overlayManager.add(pointOverlay)

            polylinesOverlay[trackId] = pointOverlay
        }


        /*val mutableAdapter = MutablePointAdapter(labelledPoints.toMutableList())

        val pointOptions = SimpleFastPointOverlayOptions.getDefaultStyle()
            .setRadius(10f)
            .setIsClickable(true)

        val pointOverlay = CustomSimpleFastPointOverlay(mutableAdapter, pointOptions)
        mMap.overlayManager.add(pointOverlay)*/

        if (center && geoPoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPointsSafe(geoPoints)

            mMap.post {
                mMap.zoomToBoundingBox(boundingBox.increaseByScale(2f), true)
                mMap.invalidate() // Force redraw
            }
        }



        return Pair(polyline, markers)
    }

    fun editColor(color: Int, alpha: Int, saturation: Float, value: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = saturation
        hsv[2] = value
        return Color.HSVToColor(alpha, hsv)
    }

    override fun onPointSelected(index: Int) {
        TODO("Not yet implemented")
    }

    override fun onPointMoved(selectedBundle: MovingPointBundle) {

        //mapViewModel.movePoint(selectedBundle)

        //Remove previous line:
        if(modifyingPolylines[selectedBundle.trackId] != null){
            mMap.overlayManager.remove(modifyingPolylines[selectedBundle.trackId])
        }

        mMap.invalidate()

        if(selectedBundle.selectedPoint == null){

            //displayedPolylines.get(selectedBundle.trackId)?.actualPoints[]
            //Call View Model to replace points
            //VM replaces, sends new data to DB,
            //VM sends display to Activity
            Log.d("debug", "Stopped move")

            return
        }



        val movingPoints = mutableListOf<GeoPoint>()
        selectedBundle.previousPoint?.let {
            movingPoints.add(GeoPoint(it.latitude, it.longitude))
        }
        selectedBundle.movingPos?.let {
            movingPoints.add(GeoPoint(it.latitude, it.longitude))
        }
        selectedBundle.nextPoint?.let {
            movingPoints.add(GeoPoint(it.latitude, it.longitude))
        }

        val movedPolyline = Polyline().apply {
            setPoints(movingPoints)
            id = System.identityHashCode(this).toString()
            outlinePaint.apply {
                this.color = color

                // Optional: Use dashed style if needed
                val pathShape = Path().apply {
                    moveTo(-5f, -5f)
                    lineTo(5f, -5f)
                    lineTo(5f, 5f)
                    lineTo(-5f, 5f)
                }
                pathEffect = PathDashPathEffect(pathShape, 12f, 0f, PathDashPathEffect.Style.ROTATE)
            }
        }

        modifyingPolylines[selectedBundle.trackId] = movedPolyline

        displayLiveModification(selectedBundle.getPoints(), selectedBundle.trackId, Color.BLUE, false)

    }

}