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
import com.minapps.trackeditor.feature_map_editor.presentation.WaypointUpdate
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
import kotlin.toString


class MapOverlayRenderer(private val mMap: MapView, private val mapViewModel: MapViewModel) :
    PointInteractionListener {

    private val controller: IMapController = mMap.controller

    //Polylines that are currently displayed
    private val displayedPolylines: MutableMap<Int, MutablePair<Polyline, CustomSimpleFastPointOverlay?>> =
        mutableMapOf()

    private var selectedPolyline: Int? = null

    // Polyline that is being modified
    private val modifyingPolylines: MutableMap<Int, Polyline> = mutableMapOf()


    /**
     * Pure map settings
     *
     */
    fun setSettings() {
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.setMultiTouchControls(true)
        mMap.getLocalVisibleRect(Rect())
        controller.setZoom(6.0)
        mMap.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMap.setHorizontalMapRepetitionEnabled(true);
        mMap.setVerticalMapRepetitionEnabled(false);
        mMap.setScrollableAreaLimitLatitude(
            MapView.getTileSystem().maxLatitude,
            MapView.getTileSystem().minLatitude,
            0
        );
        mMap.minZoomLevel = 4.0
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
    ) {

        displayedPolylines[trackId]?.let { pair ->
            mMap.overlayManager.remove(pair.first)
            pair.second?.let { mMap.overlayManager.remove(it) }
            displayedPolylines.remove(trackId)
        }

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

        val polylinePair = displayWaypoints(
            geoPoints = geoPoints,
            nodes = clickablePoints,
            trackId = trackId,
            color = color,
            center = center
        )

        // Add track to display list
        displayedPolylines[trackId] = polylinePair
    }

    /**
     * TODO For already in polylines
     *
     * @param trackId
     * @param color
     * @param center
     */
    fun displayTrack(
        trackId: Int,
        color: Int,
        center: Boolean
    ) {

        val polyline = displayedPolylines[trackId]?.first
        if (polyline == null) return

        // Remove old polyline and overlay if already displayed
        displayedPolylines[trackId]?.let { pair ->
            mMap.overlayManager.remove(pair.first)
            pair.second?.let { mMap.overlayManager.remove(it) }
            displayedPolylines.remove(trackId)
        }



        // Clickable points
        val clickablePoints = mutableListOf<IGeoPoint>()

        val geoPoints = polyline.actualPoints
        geoPoints.forEachIndexed { index, waypoint ->
            // Interactable labelled point
            val labelled = LabelledGeoPoint(waypoint.latitude, waypoint.longitude, "").apply {
                label = index.toString()
            }
            clickablePoints.add(labelled)
        }

        val polylinePair = displayWaypoints(
            geoPoints = geoPoints,
            nodes = clickablePoints,
            trackId = trackId,
            color = color,
            center = center
        )

        // Add track to display list
        displayedPolylines[trackId] = polylinePair
    }


    /**
     * TODO OOOOOOOOOOOO
     * Display full track (e.g. after import)
     *
     * @param waypoints List of waypoints to display
     * @param trackId Track id
     * @param color Track color
     * @param center If should center after displaying
     */
    fun displayNewAddedPoint(
        waypoint: Pair<Double, Double>,
        trackId: Int,
    ) {

        val polyline = displayedPolylines[trackId]?.first

        // First point of track : Create it
        if (polyline == null) {
            displayTrack(
                waypoints = listOf(waypoint),
                trackId = trackId,
                color = Color.RED,
                center = false
            )
        } else {
            polyline.addPoint(GeoPoint(waypoint.first, waypoint.second))
            displayTrack(
                trackId = trackId,
                color = Color.RED,
                center = false
            )
        }

        mMap.invalidate()

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

        unselectPolyline(trackId)

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

        selectTrack(trackId)
        unselectPolyline(trackId)

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
     * MAIN DISPLAY FUNCTION
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
        geoPoints: MutableList<GeoPoint>,
        nodes: MutableList<IGeoPoint>,
        trackId: Int,
        color: Int,
        center: Boolean,
        isModifying: Boolean = false
    ): MutablePair<Polyline, CustomSimpleFastPointOverlay?> {

        val selectedColor = Color.YELLOW
        val unselectedColor = Color.RED
        val paintColor = if(mapViewModel.editState.value.currentselectedTrack == trackId) selectedColor else color

        selectTrack(trackId)
        unselectPolyline(trackId)

        // Create polyline with geoPoints and style
        val polyline = Polyline().apply {
            setPoints(geoPoints)
            id = System.identityHashCode(this).toString()
            PaintType.SOLID.applyTo(outlinePaint, paintColor)
        }

        polyline.setOnClickListener{ polyline, mapView, eventPos ->

            if (selectedPolyline == trackId) {
                // deselect
                unselectPolyline()
                selectedPolyline = null
                mapViewModel.selectedTrack(null) // if you want VM cleared
            } else {
                // select this one
                unselectPolyline()
                selectedPolyline = trackId
                polyline.outlinePaint.color = selectedColor
                mapViewModel.selectedTrack(trackId)
            }
            mMap.invalidate()
            true
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
        if (!isModifying) {
            val pointAdapter = MutablePointAdapter(nodes.toMutableList(), false)
            val pointOptions = SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setRadius(8f) //Smaller is faster
                .setIsClickable(true)
                .setCellSize(20) //Bigger is faster (less points shown on screen)
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

    fun unselectPolyline(trackId: Int? = null, forceUpdate: Boolean? = false) {
        displayedPolylines.forEach { id, p1 ->
            if (trackId == null || id != trackId) {
                p1.first.outlinePaint.color = Color.RED
            }
        }
        if (forceUpdate == true) {
            mMap.invalidate()
        }
    }

    override fun onPointClicked(selectedBundle: MovingPointBundle) {
        // Ignore if no actual point is selected
        if (selectedBundle.selectedPoint == null) return

        val trackId = selectedBundle.trackId

        // Deselect all others
        unselectPolyline()

        // Select this track
        displayedPolylines[trackId]?.first?.outlinePaint?.color = Color.YELLOW
        selectedPolyline = trackId

        // Notify ViewModel
        mapViewModel.selectedTrack(trackId)

        // Redraw
        mMap.invalidate()
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
            movingPoint.add(
                Waypoint(
                    id = 0.0,
                    lat = it.latitude,
                    lng = it.longitude,
                    elv = null,
                    time = "",
                    trackId = selectedBundle.trackId
                )
            )
        }

        // Add current moving point
        selectedBundle.movingPos?.let {
            movingPoint.add(
                Waypoint(
                    id = 1.0,
                    lat = it.latitude,
                    lng = it.longitude,
                    elv = null,
                    time = "",
                    trackId = selectedBundle.trackId
                )
            )
        }

        // Add next point if exists (track border)
        if (selectedBundle.nextPoint != null && !isSet) {
            val it = selectedBundle.nextPoint!!
            movingPoint.add(
                Waypoint(
                    id = 2.0,
                    lat = it.latitude,
                    lng = it.longitude,
                    elv = null,
                    time = "",
                    trackId = selectedBundle.trackId
                )
            )
        }

        // Call VM movePoint with list of point, idx of point being modified, if set
        mapViewModel.movePoint(
            movingPoint,
            selectedBundle.selectedPointIdx,
            selectedBundle.selectedPoint == null
        )
    }

    fun handleWaypointEvents(event: WaypointUpdate){
        when (event) {
            is WaypointUpdate.Added -> handleWaypointAdded(event.trackId, event.point)
            is WaypointUpdate.AddedList -> handleWaypointAddedList(event.trackId, event.points)
            is WaypointUpdate.ViewChanged -> handleWaypointViewChanged(event.trackId, event.points)
            is WaypointUpdate.Removed -> handleWaypointRemoved(event.trackId, event.index)
            is WaypointUpdate.Moved -> handleWaypointMoved(event.trackId, event.points)
            is WaypointUpdate.Cleared -> handleTrackCleared(event.trackId)
            is WaypointUpdate.MovedDone -> handleWaypointMovedDone(event.trackId, event.pointId, event.point)
        }
    }

    /**
     * TODO
     * Called When Map clicked
     * Add point to polyline in optimised way
     *
     * @param trackId
     * @param point
     */
    private fun handleWaypointAdded(trackId: Int, point: Pair<Double, Double>) {
        displayNewAddedPoint(point, trackId)
        Log.d("debug", "Added point")
    }


    /**
     * Render list of points
     *
     * @param trackId
     * @param points
     */
    private fun handleWaypointAddedList(trackId: Int, points: List<Pair<Double, Double>>) {
        displayTrack(points, trackId, Color.RED, true)
    }

    private fun handleWaypointViewChanged(trackId: Int, points: List<Pair<Double, Double>>) {
        displayTrack(points, trackId, Color.RED, false)
    }

    /**
     * Render moved point
     *
     * @param trackId
     * @param points
     */
    private fun handleWaypointMoved(trackId: Int, points: List<Pair<Double, Double>>) {
        displayLiveModification(points, trackId, Color.rgb(255, 128, 0))
    }

    /**
     * Render final point move
     *
     * @param trackId
     * @param pointId
     * @param point
     */
    private fun handleWaypointMovedDone(trackId: Int, pointId: Int, point: Pair<Double, Double>) {
        displayLiveModificationDone(point, trackId, pointId)
    }

    /**
     * Remove point
     *
     * @param trackId
     * @param index
     */
    private fun handleWaypointRemoved(trackId: Int, index: Int) {
        // TODO
    }

    /**
     * Remove track
     *
     * @param trackId
     */
    private fun handleTrackCleared(trackId: Int) {
        // TODO
    }

    private fun selectTrack(trackId: Int?){
        selectedPolyline = trackId

        if(trackId != null){
            mapViewModel.selectedTrack(trackId)
        }
    }
}
