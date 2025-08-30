package com.minapps.trackeditor.feature_map_editor.presentation.overlay

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.common.MutablePair
import com.minapps.trackeditor.feature_map_editor.domain.model.SimpleWaypoint
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import com.minapps.trackeditor.core.domain.type.ActionType
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
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import kotlin.math.max
import kotlin.math.min


class MapOverlayRenderer(private val mMap: MapView, private val mapViewModel: MapViewModel) :
    PointInteractionListener {

    private val controller: IMapController = mMap.controller

    data class SelectedPoint(
        var trackId: Int,
        var id: Double,
        var overlay: SimpleFastPointOverlay
    )

    data class TrackRenderData(
        // Polyline displayed
        var polyline: Polyline,
        // Clickable points
        var overlay: CustomSimpleFastPointOverlay?,
        // Mapped ID to Index
        val indexMap: MutableMap<Double, Int>,
        val reverseIndexMap: MutableMap<Int, Double> = mutableMapOf()

    ) {

        /**
         * Insert a new id into the indexMap, shifting indices as needed.
         * Returns the correct index where it should be placed.

         * @param map
         * @param id
         * @return
         */
        fun insertId(id: Double): Int {
            // Sort map by id
            val sorted = indexMap.toList().sortedBy { it.first }
            // Find index where id should be placed
            val insertIndex =
                sorted.indexOfFirst { it.first > id }.takeIf { it != -1 } ?: indexMap.size

            // Shift all indices >= insertIndex
            indexMap.keys.forEach { key ->
                val idx = indexMap[key]!!
                if (idx >= insertIndex) {
                    indexMap[key] = idx + 1
                    reverseIndexMap[idx + 1] = key
                }
            }

            // Insert new id at correct index
            indexMap[id] = insertIndex
            reverseIndexMap[insertIndex] = id

            return insertIndex
        }

        /**
         * Remove id from maps
         *
         * @param id
         * @return
         */
        fun removeId(id: Double): Int? {
            // Find index of the id
            val removeIndex = indexMap[id] ?: return null

            // Remove from both maps
            indexMap.remove(id)
            reverseIndexMap.remove(removeIndex)

            // Shift all indices > removeIndex down by 1
            val keysToShift = indexMap.filterValues { it > removeIndex }.keys
            keysToShift.forEach { key ->
                val oldIndex = indexMap[key]!!
                val newIndex = oldIndex - 1
                indexMap[key] = newIndex
                reverseIndexMap.remove(oldIndex)
                reverseIndexMap[newIndex] = key
            }

            return removeIndex
        }

        fun removeIndex(start: Int, end: Int) {

            // Find index of the id
            val idStart = reverseIndexMap[start] ?: return
            val idEnd = reverseIndexMap[end] ?: return

            // Remove from both maps
            indexMap.entries.removeIf { (_, idx) -> idx in (start + 1) until end }
            reverseIndexMap.entries.removeIf { (idx, _) -> idx in (start + 1) until end }

            // How many points got removed
            val shift = (end - start - 1).coerceAtLeast(0)

            if (shift > 0) {
                // Shift all indices >= end down by 'shift'
                val keysToShift = indexMap.filterValues { it >= end }.keys.toList()

                keysToShift.forEach { id ->
                    val oldIndex = indexMap[id]!!
                    val newIndex = oldIndex - shift
                    indexMap[id] = newIndex
                    reverseIndexMap.remove(oldIndex)
                    reverseIndexMap[newIndex] = id
                }
            }
        }

    }

    // Polylines that are currently displayed with render data
    private val displayedPolylines: MutableMap<Int, TrackRenderData> = mutableMapOf()


    private var selectedPolylines: MutableList<Int> = mutableListOf()
    private var selectedPoints: MutableList<SelectedPoint> = mutableListOf()
    private var selectedTrackOverlay: Polyline? = null

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
            MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0
        );
        mMap.minZoomLevel = 4.0
    }


    /**
     * Display full track (e.g. after import)
     * Create UI track from scratch
     *
     * @param waypoints List of waypoints to display
     * @param trackId Track id
     * @param color Track color
     * @param center If should center after displaying
     */
    fun displayTrack(
        waypoints: List<SimpleWaypoint>, trackId: Int, color: Int, center: Boolean
    ) {

        // Remove all polyline data
        val renderData = displayedPolylines[trackId]
        if (renderData != null) {
            mMap.overlayManager.remove(renderData.overlay)
            mMap.overlayManager.remove(renderData.polyline)
            displayedPolylines.remove(trackId)
        }

        // List of points to display in GeoPoint format
        val geoPoints = mutableListOf<GeoPoint>()

        // Clickable points
        val clickablePoints = mutableListOf<IGeoPoint>()

        // Convert all coords to geoPoints
        val indexMap: MutableMap<Double, Int> = mutableMapOf()
        val reverseIndexMap: MutableMap<Int, Double> = mutableMapOf()
        waypoints.forEachIndexed { index, waypoint ->
            val geoPoint = GeoPoint(waypoint.lat, waypoint.lng)
            geoPoints.add(geoPoint)

            // Interactable labelled point
            val labelled = LabelledGeoPoint(waypoint.lat, waypoint.lng, "").apply {
                label = waypoint.id.toString()
            }

            indexMap[waypoint.id] = index
            reverseIndexMap[index] = waypoint.id
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
        displayedPolylines[trackId] =
            TrackRenderData(polylinePair.first, polylinePair.second, indexMap, reverseIndexMap)

        mMap.invalidate()
    }

    /**
     * TODO For already in polylines
     *
     * Display tracks by TrackID
     * NewAddedWaypoint
     * ModifyWaypoint
     *
     * @param trackId
     * @param color
     * @param center
     */
    fun displayTrack(
        trackId: Int, color: Int, center: Boolean
    ) {

        val renderData = displayedPolylines[trackId]
        if (renderData == null) return

        // Remove old polyline and overlay if already displayed
        mMap.overlayManager.remove(renderData.overlay)
        mMap.overlayManager.remove(renderData.polyline)

        // Clickable points
        val clickablePoints = mutableListOf<IGeoPoint>()

        // Get polyline points
        val geoPoints = renderData.polyline.actualPoints

        // Create clickable point with label ID
        geoPoints.forEachIndexed { index, waypoint ->

            // Interactable labelled point
            val labelled = LabelledGeoPoint(waypoint.latitude, waypoint.longitude, "").apply {
                label = renderData.reverseIndexMap[index].toString()
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

        // Update display list
        displayedPolylines[trackId]?.polyline = polylinePair.first
        displayedPolylines[trackId]?.overlay = polylinePair.second

        mMap.invalidate()
    }


    /**
     * Display full track (e.g. after import)
     *
     * @param waypoints List of waypoints to display
     * @param trackId Track id
     * @param color Track color
     * @param center If should center after displaying
     */
    fun displayNewAddedPoint(
        waypoint: SimpleWaypoint,
        trackId: Int,
    ) {

        /* TODO :
            - Get polyline data
            - Add new point and update map
            -
         */

        var renderData = displayedPolylines[trackId]

        // First point of track : Create it
        if (renderData == null) {
            displayTrack(
                waypoints = listOf(waypoint), trackId = trackId, color = Color.RED, center = false
            )
        }
        // If track exists, update original points
        else {

            val polyline = renderData.polyline
            val points = polyline.actualPoints.toMutableList()

            // Update map and get index
            val insertIndex = renderData.insertId(waypoint.id)

            // Insert into polyline at correct index
            points.add(insertIndex, GeoPoint(waypoint.lat, waypoint.lng))
            polyline.setPoints(points)

            // Display to refresh clickable overlay
            displayTrack(
                trackId = trackId, color = Color.RED, center = false
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
        pointId: Double,
    ) {

        // Remove temporary moving polyline
        modifyingPolylines[trackId]?.let {
            mMap.overlayManager.remove(it)
            modifyingPolylines.remove(trackId)
        }

        // TODO ON LARGE TRACKS INDEX IS FALSE
        Log.d("point", "Point id set : $pointId")

        selectTrack(trackId, true)

        // Get the displayed polyline
        val renderData = displayedPolylines[trackId] ?: return
        val updatedPolylinePoints = renderData.polyline.actualPoints.toMutableList()

        // Get the index
        val index = renderData.indexMap[pointId]

        // Update position of point
        if (index != null) {
            updatedPolylinePoints[index] = GeoPoint(waypoint.first, waypoint.second)
        }

        // Replace polyline with updated points
        renderData.polyline.setPoints(updatedPolylinePoints)

        // Display and change color
        displayTrack(trackId, Color.YELLOW, false)
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

        Log.d("select", "display wp trkid : $trackId")
        val selectedColor = Color.YELLOW
        val unselectedColor = Color.RED

        val paintColor =
            if (mapViewModel.editState.value.currentSelectedTracks.isNotEmpty() && mapViewModel.editState.value.currentSelectedTracks.contains(
                    trackId
                )
            ) selectedColor else color

        // Create polyline with geoPoints and style
        val polyline = Polyline().apply {
            setPoints(geoPoints)
            id = System.identityHashCode(this).toString()
            PaintType.SOLID.applyTo(outlinePaint, paintColor)
        }

        polyline.setOnClickListener { polyline, mapView, eventPos ->

            Log.d("polyline", "clicked $trackId")

            if (selectedPolylines.contains(trackId)) {
                // deselect
                selectTrack(trackId, false)
            } else {
                // select this one
                polyline.outlinePaint.color = selectedColor
                selectTrack(trackId, true)
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
                .setPointStyle(primaryPaint).setSelectedPointStyle(selectedPaint)
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

    override fun onPointClicked(selectedBundle: MovingPointBundle) {
        // Ignore if no actual point is selected
        if (selectedBundle.selectedPoint == null) return

        val trackId = selectedBundle.trackId

        // Notify ViewModel
        selectTrack(trackId, true, selectedBundle.selectedPointRealId)


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

        // Check if is allowed to move point
        if (mapViewModel.editState.value.currentSelectedTool != ActionType.HAND) return

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
            selectedBundle.selectedPointRealId,
            selectedBundle.selectedPoint == null
        )
    }

    fun handleWaypointEvents(event: WaypointUpdate) {
        when (event) {
            is WaypointUpdate.Added -> handleWaypointAdded(event.trackId, event.point)
            is WaypointUpdate.AddedList -> handleWaypointAddedList(
                event.trackId, event.points, event.center
            )

            is WaypointUpdate.ViewChanged -> handleWaypointViewChanged(event.trackId, event.points)
            is WaypointUpdate.Removed -> handleWaypointRemoved(event.trackId, event.index)
            is WaypointUpdate.Moved -> handleWaypointMoved(event.trackId, event.points)
            is WaypointUpdate.Cleared -> handleTrackCleared(event.trackId)
            is WaypointUpdate.MovedDone -> handleWaypointMovedDone(
                event.trackId, event.pointId, event.point
            )

            is WaypointUpdate.RemovedById -> handleWaypointRemovedById(event.trackId, event.id)
            is WaypointUpdate.RemovedSegment -> handleRemovedSegment(
                event.trackId,
                event.startId,
                event.endId
            )

            is WaypointUpdate.RemovedTracks -> handleRemovedTrack(event.trackIds)
        }

        redrawSelection()
    }

    private fun handleRemovedSegment(trackId: Int, startId: Double, endId: Double) {
        val renderData = displayedPolylines[trackId]
        if (renderData == null) return

        // Get index
        val idx1 = renderData.indexMap[startId] ?: return
        val idx2 = renderData.indexMap[endId] ?: return

        val min = min(idx1, idx2)
        val max = max(idx1, idx2)

        // Get Geopoint because cant delete by index
        val polyline = renderData.polyline.actualPoints

        val newPoints = polyline.toMutableList()
        if (min < max && min in newPoints.indices && max in newPoints.indices) {
            newPoints.subList(min + 1, max).clear()
            renderData.polyline.setPoints(newPoints)
        }

        renderData.removeIndex(min, max)

        Log.d("delete points", "delete : ${polyline.size}")

        clearAllSelections()

        displayTrack(trackId, Color.YELLOW, false)

        //mMap.invalidate()
    }

    private fun handleRemovedTrack(trackIds: List<Int>) {
        trackIds.forEach { trackId ->
            mMap.overlays.remove(displayedPolylines[trackId]?.polyline)
            mMap.overlays.remove(displayedPolylines[trackId]?.overlay)
            displayedPolylines.remove(trackId)
        }
        clearAllSelections()
    }

    /**
     * TODO
     * Called When Map clicked
     * Add point to polyline in optimised way
     *
     * @param trackId
     * @param point
     */

    private fun handleWaypointAdded(trackId: Int, point: SimpleWaypoint) {
        displayNewAddedPoint(point, trackId)
        Log.d("debug", "Added point")
    }


    /**
     * Render list of points
     *
     * @param trackId
     * @param points
     */
    private fun handleWaypointAddedList(
        trackId: Int, points: List<SimpleWaypoint>, center: Boolean
    ) {
        displayTrack(points, trackId, Color.RED, center)
    }

    private fun handleWaypointViewChanged(trackId: Int, points: List<SimpleWaypoint>) {
        Log.d("debugOpti", "Sent To update")
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
    private fun handleWaypointMovedDone(
        trackId: Int, pointId: Double, point: Pair<Double, Double>
    ) {
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
     * Remove waypoint by id
     *
     * @param trackId
     * @param id
     */
    private fun handleWaypointRemovedById(trackId: Int, id: Double) {


        val renderData = displayedPolylines[trackId]
        if (renderData == null) return

        // Get index
        val index = renderData.indexMap[id]
        if (index == null) return

        // Get Geopoint because cant delete by index
        val point = renderData.polyline.actualPoints[index]
        renderData.polyline.actualPoints.remove(point)

        // Update Maps
        renderData.removeId(id)

        clearAllSelections()

        // Rebuild overlay
        displayTrack(trackId, Color.YELLOW, false)
    }

    /**
     * Remove track
     *
     * @param trackId
     */
    private fun handleTrackCleared(trackId: Int) {
        // TODO
    }

    fun selectTrack(trackId: Int?, select: Boolean, pointId: Double? = null) {
        val state = mapViewModel.selectedTrack(trackId, select, pointId)
        selectedPolylines = state.selectedTrackIds
        colorTracks()
        colorPoints(state.selectedPoints)
        colorTrackSegment()
    }

    private fun colorTracks() {
        displayedPolylines.forEach { id, renderData ->
            if (selectedPolylines.contains(id)) {
                renderData.polyline.outlinePaint.color = Color.YELLOW
            } else {
                renderData.polyline.outlinePaint.color = Color.RED
            }
        }

        mMap.invalidate()
    }

    private fun colorPoints(newSelectedPoints: MutableList<Pair<Int, Double>>) {

        // Remove last overlays
        clearAllSelections()

        // Display new overlays
        newSelectedPoints.forEachIndexed { index, newPoint ->
            val overlay = getPointOverlay(newPoint.first, newPoint.second, index)
            if (overlay == null) return@forEachIndexed
            selectedPoints.add(SelectedPoint(newPoint.first, newPoint.second, overlay))
            mMap.overlays.add(overlay)
        }

        mMap.invalidate()
    }

    private fun getPointOverlay(
        trackId: Int,
        id: Double,
        colorIndex: Int
    ): SimpleFastPointOverlay? {

        val renderData = displayedPolylines[trackId] ?: return null

        val index = renderData.indexMap[id] ?: return null

        val point = listOf(renderData.polyline.actualPoints[index])
        val theme = SimplePointTheme(point)

        val colorList = mapOf(0 to Color.GREEN, 1 to Color.MAGENTA)

        // Create paint
        val paint = Paint().apply {
            color = colorList[colorIndex] ?: Color.GREEN
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Set circle point
        val options = SimpleFastPointOverlayOptions.getDefaultStyle()
            .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
            .setRadius(14f)
            .setIsClickable(false)
            .setPointStyle(paint)
            .setSymbol(SimpleFastPointOverlayOptions.Shape.CIRCLE)

        // Create overlay
        val overlay = SimpleFastPointOverlay(theme, options)

        return overlay
    }

    private fun colorTrackSegment() {

        if (selectedPoints.size != 2) return

        var trackId: Int? = null
        var first: Double? = null
        var last: Double? = null
        selectedPoints.forEach { point ->
            if (trackId == null) {
                trackId = point.trackId
                first = point.id
            } else if (trackId != point.trackId) {
                return
            } else {
                last = point.id
            }
        }

        val renderData = displayedPolylines[trackId] ?: return
        val points = renderData.polyline.actualPoints
        val ind1 = renderData.indexMap[first] ?: return
        val ind2 = renderData.indexMap[last] ?: return

        val f = min(ind1, ind2)
        val l = max(ind1, ind2)
        val polyline = Polyline(mMap).apply {
            setPoints(points.subList(f, l + 1))
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 10f
            setOnClickListener { _, _, _ -> false }
        }

        selectedTrackOverlay?.let { mMap.overlays.remove(it) }
        selectedTrackOverlay = polyline

        mMap.overlays.add(polyline)
        mMap.invalidate()
    }

    fun clearAllSelections() {
        clearTrackOverlaySelection()
        clearPointOverlaySelection()
        mMap.invalidate()
    }

    fun clearTrackOverlaySelection() {
        // Remove track overlays
        if (selectedTrackOverlay != null) {
            mMap.overlays.remove(selectedTrackOverlay)
            selectedTrackOverlay = null
        }
    }

    fun clearPointOverlaySelection() {
        // Remove point overlays
        selectedPoints.forEach { selectedPoint ->
            mMap.overlays.remove(selectedPoint.overlay)
        }
        selectedPoints.clear()
    }

    fun redrawSelection() {
        if (selectedTrackOverlay != null) {
            mMap.overlays.remove(selectedTrackOverlay)
            mMap.overlays.add(selectedTrackOverlay)
        }

        selectedPoints.forEach { selectedPoint ->
            mMap.overlays.remove(selectedPoint.overlay)
            mMap.overlays.add(selectedPoint.overlay)
        }

        mMap.invalidate()
    }

}
