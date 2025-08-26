package com.minapps.trackeditor.feature_map_editor.presentation.overlay

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import com.minapps.trackeditor.feature_map_editor.presentation.MovingPointBundle
import com.minapps.trackeditor.feature_map_editor.presentation.MutablePointAdapter
import com.minapps.trackeditor.feature_map_editor.presentation.interaction.PointInteractionListener
import com.minapps.trackeditor.feature_map_editor.presentation.util.vibrate
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions

class CustomSimpleFastPointOverlay(
    //private val pointList: PointAdapter,
    val pointAdapter: MutablePointAdapter,
    style: SimpleFastPointOverlayOptions,
    private val listener: PointInteractionListener,
    private val trackId: Int,
) : SimpleFastPointOverlay(pointAdapter, style), SimpleFastPointOverlay.OnClickListener {

    var isDragging: Boolean = false
    var selectedBundle: MovingPointBundle = MovingPointBundle(trackId = trackId)

    init {
        // Set this instance as the click listener
        setOnClickListener(this)
    }

    override fun onClick(points: PointAdapter, pointId: Int) {
        val geoPoint = points.get(pointId)
        val realId = (geoPoint as? LabelledGeoPoint)?.label?.toDouble()

        Log.d("debug","Clicked point real index: $realId, fake index: $pointId at (${geoPoint.latitude}, ${geoPoint.longitude})")

        selectedBundle.clear()
        selectedBundle.selectedPointIdx = pointId
        selectedBundle.selectedPoint = points.get(pointId)
        selectedBundle.selectedPointRealId = realId
        if(pointId > 0) selectedBundle.previousPoint = points.get(pointId-1)
        if(pointId+1 < points.size()) selectedBundle.nextPoint = points.get(pointId+1)

        listener.onPointClicked(selectedBundle)

    }


    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        // TODO: Implement touch handling

        val x = event.x
        val y = event.y

        if(isDragging && selectedBundle.isInit){
            when (event.action) {
                ACTION_DOWN, ACTION_MOVE -> {

                    val newPoint = mapView.getProjection().fromPixels(x.toInt(), y.toInt())
                    selectedBundle.movingPos = newPoint
                    listener.onPointMoved(selectedBundle)
                    //Log.d("debug","Moving point to $newPoint")

                }
                ACTION_UP, ACTION_CANCEL -> {
                    val finalPos = mapView.getProjection().fromPixels(x.toInt(), y.toInt())
                    selectedBundle.movingPos = finalPos
                    selectedBundle.selectedPoint = null
                    listener.onPointMoved(selectedBundle)
                    isDragging = false
                    selectedBundle.clear()
                }
            }

        }

        return super.onTouchEvent(event, mapView)
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        Log.d("debug","Long pressed point index")
        // TODO: Implement long press handling

        isDragging = onSingleTapConfirmed(e,mapView)

        if(selectedBundle.isInit){
            mapView.context.vibrate(20)
        }

        return isDragging
    }

    fun updatePoint(index: Int, newPoint: GeoPoint) {
            pointAdapter.updatePoint(index, newPoint)
    }

}




/*class CustomSimpleFastPointOverlay(
    private val pointList: MutablePointAdapter,
    private val polyline: Polyline,
    style: SimpleFastPointOverlayOptions
) : SimpleFastPointOverlay(pointList, style), SimpleFastPointOverlay.OnClickListener {


    private var isDragging = false
    private var selectedPointIndex: Int? = null

    init {
        setOnClickListener(this)
    }

    override fun onClick(points: PointAdapter, point: Int) {
        selectedPointIndex = point
        Log.d("debug", "Clicked point index: $point")
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        if (selectedPointIndex != null) {
            isDragging = true
            Log.d("debug", "Long press on point: $selectedPointIndex")
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        if (isDragging && selectedPointIndex != null) {
            when (event.action) {
                ACTION_MOVE -> {
                    val geoPoint = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt())

                    // 1. Update point in adapter
                    pointList.set(selectedPointIndex!!, geoPoint)

                    // 2. Update corresponding polyline node
                    polyline.actualPoints[selectedPointIndex!!] = geoPoint as GeoPoint

                    // 3. Redraw
                    mapView.invalidate()
                }
                ACTION_UP, ACTION_CANCEL -> {
                    isDragging = false
                    selectedPointIndex = null
                    Log.d("debug", "Drag end")
                }
            }
        }
        return super.onTouchEvent(event, mapView)
    }

}*/