package com.minapps.trackeditor.feature_map_editor.presentation.overlay

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.Rect
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.common.MutablePair
import com.minapps.trackeditor.feature_map_editor.presentation.MapViewModel
import com.minapps.trackeditor.feature_map_editor.presentation.MovingPointBundle
import com.minapps.trackeditor.feature_map_editor.presentation.MutablePointAdapter
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.PointInteractionListener
import com.minapps.trackeditor.feature_map_editor.presentation.util.PaintType
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions


class MapOverlayRenderer (private val mMap : MapView, private val mapViewModel: MapViewModel) : PointInteractionListener {

    private val controller: IMapController = mMap.controller

    //Polylines that are currently displayed
    private val displayedPolylines: MutableMap<Int, MutablePair<Polyline, CustomSimpleFastPointOverlay?>> = mutableMapOf()
    // Polyline that is being modified
    private val modifyingPolylines: MutableMap<Int, Polyline> = mutableMapOf()


    /**
     * Pure map settings
     *
     */
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


    /**
     * Display full track (e.g. after import)
     *
     * @param waypoints List of waypoints to display
     * @param trackId Track id
     * @param color Track color
     * @param center If should center after displaying
     */
    fun displayTrack(
        waypoints: List<Pair<Double, Double>>,
        trackId: Int,
        color: Int,
        center: Boolean
    ){

        val polylinePair = displayWaypoints(
            waypoints = waypoints,
            trackId = trackId,
            color = color,
            center = center
        )

        // Add track to display list
        displayedPolylines[trackId] = polylinePair
    }


    /**
     * Display live modifications of moving track point
     *
     * @param waypoints List of waypoints to draw
     * @param trackId
     * @param color
     */
    fun displayLiveModification(
        waypoints: List<Pair<Double, Double>>,
        trackId: Int,
        color: Int,
    ) {
        // Remove previous modification overlay if it exists
        modifyingPolylines[trackId]?.let {
            mMap.overlayManager.remove(it)
        }

        // Convert to GeoPoints
        val movingPoints = waypoints.map { GeoPoint(it.first, it.second) }

        // Create new modifying polyline
        val movedPolyline = Polyline().apply {
            setPoints(movingPoints)
            id = System.identityHashCode(this).toString()
            PaintType.DASHED.applyTo(outlinePaint, color)
        }

        // Add to map and track it
        mMap.overlayManager.add(movedPolyline)

        // Add reference to polyline
        modifyingPolylines[trackId] = movedPolyline

        mMap.invalidate()
    }


    /**
     * Display the modified track (point moved by user)
     *
     * @param waypoint Waypoint moved (new location)
     * @param trackId Track id
     * @param pointId Waypoint id
     */
    fun displayLiveModificationDone(
        waypoint: Pair<Double, Double>,
        trackId: Int,
        pointId: Int,
    ) {

        // Remove temporary moving polyline
        modifyingPolylines[trackId]?.let {
            mMap.overlayManager.remove(it)
            modifyingPolylines.remove(trackId)
        }

        // Get the displayed polyline and clickable overlay and update its point
        val polyline = displayedPolylines[trackId]?.first ?: return
        val overlay = displayedPolylines[trackId]?.second ?: return

        val updatedPolylinePoints = polyline.actualPoints.toMutableList()
        updatedPolylinePoints[pointId] = GeoPoint(waypoint.first, waypoint.second)
        polyline.setPoints(updatedPolylinePoints)

        // Get the adapter and update its data
        val adapter = overlay.pointAdapter
        adapter.updatePoint(pointId, GeoPoint(waypoint.first, waypoint.second))

        // Force rebuild the overlay
        mMap.overlayManager.remove(overlay)

        val pointOptions = overlay.style
        val newOverlay = CustomSimpleFastPointOverlay(adapter, pointOptions, this, trackId)

        mMap.overlayManager.add(newOverlay)

        // Replace in stored data
        displayedPolylines[trackId] = MutablePair(polyline, newOverlay)

        // Redraw
        mMap.invalidate()
    }


    /**
     * Display a list of waypoints
     *
     * @param waypoints List of points to be displayed
     * @param trackId Track id
     * @param color color of track
     * @param center If view should center on track after displaying
     * @param isModifying If track to be displayed is a segment that is being modified (no need for CSFPO)
     * @return Polyline and clickable points
     */
    private fun displayWaypoints(
        waypoints: List<Pair<Double, Double>>,
        trackId: Int,
        color: Int,
        center: Boolean,
        isModifying: Boolean = false
    ): MutablePair<Polyline, CustomSimpleFastPointOverlay?> {

        // List of points to display in GeoPoint format
        val geoPoints = mutableListOf<GeoPoint>()

        // Clickable points
        val clickablePoints = mutableListOf<IGeoPoint>()

        // Convert all coords to geoPoints
        waypoints.forEachIndexed { index, waypoint ->
            val geoPoint = GeoPoint(waypoint.first, waypoint.second)
            geoPoints.add(geoPoint)

            // Interactable labelled point
            val labelled = LabelledGeoPoint(waypoint.first, waypoint.second, "").apply {
                label = index.toString()
            }
            clickablePoints.add(labelled)
        }

        // Create polyline with geoPoints and style
        val polyline = Polyline().apply {
            setPoints(geoPoints)
            id = System.identityHashCode(this).toString()
            PaintType.SOLID.applyTo(outlinePaint, color)
        }

        // Add to polyline to be displayed
        mMap.overlayManager.add(polyline)

        // Point overlay styles
        val primaryPaint = Paint().apply {
            editColor(color, 0, 0.70f, 0.60f)
        }
        val selectedPaint = Paint().apply {
            editColor(color, 0, 0.25f, 0.40f)
        }

        // Create clickable points overlay
        var pointOverlay: CustomSimpleFastPointOverlay? = null
        if(!isModifying){
            val pointAdapter = MutablePointAdapter(clickablePoints.toMutableList(), false)
            val pointOptions = SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setRadius(7f) //Smaller is faster
                .setIsClickable(true)
                .setCellSize(25) //Bigger is faster (less points shown on screen)
                .setPointStyle(primaryPaint)
                .setSelectedPointStyle(selectedPaint)
                .setSymbol(SimpleFastPointOverlayOptions.Shape.CIRCLE)

            pointOverlay = CustomSimpleFastPointOverlay(pointAdapter, pointOptions, this, trackId)

            // Add overlay to be displayed
            mMap.overlayManager.add(pointOverlay)
        }

        // Center to track if asked for
        if (center && geoPoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPointsSafe(geoPoints)
            mMap.post {
                mMap.zoomToBoundingBox(boundingBox.increaseByScale(2f), true)
                mMap.invalidate() // Force redraw
            }
        }

        // Return track line and clickable overlay
        return MutablePair(polyline, pointOverlay)
    }


    fun editColor(color: Int, alpha: Int, saturation: Float, value: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = saturation
        hsv[2] = value
        return Color.HSVToColor(alpha, hsv)
    }


    /**
     * When user moves point, renderer listens for move notification.
     * (Called by CustomSimpleFastPointOverlay)
     *
     * converts coords to waypoint and send it to the ViewModel
     *
     * @param selectedBundle
     */
    override fun onPointMoved(selectedBundle: MovingPointBundle) {
        /**TODO How do we manage elevation ?
         * - Null then interpolate ?
         * - Interpolate when ? Saving to DB ? Exporting ?
         */

        // Create list of moved points
        val movingPoint = mutableListOf<Waypoint>()

        // If selectedPoint is null then point has been set (user lifted finger)
        val isSet = selectedBundle.selectedPoint == null

        // Add previous point if exists (track border)
        if (selectedBundle.previousPoint != null && !isSet) {
            val it = selectedBundle.previousPoint!!
            movingPoint.add(Waypoint(id = 0.0, lat = it.latitude, lng = it.longitude, elv = null, trackId = selectedBundle.trackId))
        }

        // Add current moving point
        selectedBundle.movingPos?.let {
            movingPoint.add(Waypoint(id = 1.0, lat = it.latitude, lng = it.longitude, elv = null, trackId = selectedBundle.trackId))
        }

        // Add next point if exists (track border)
        if (selectedBundle.nextPoint != null && !isSet) {
            val it = selectedBundle.nextPoint!!
            movingPoint.add(Waypoint(id = 2.0, lat = it.latitude, lng = it.longitude, elv = null, trackId = selectedBundle.trackId))
        }

        // Call VM movePoint with list of point, idx of point being modified, if set
        mapViewModel.movePoint(movingPoint, selectedBundle.selectedPointIdx, selectedBundle.selectedPoint == null)
    }
}
