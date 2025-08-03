package com.minapps.trackeditor.feature_map_editor.presentation

import android.content.Context
import android.provider.Settings.Global.getString
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.R
import com.minapps.trackeditor.feature_map_editor.domain.usecase.AddWaypointUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.ClearAllUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.CreateTrackUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.GetTrackWaypointsUseCase
import com.minapps.trackeditor.feature_track_import.domain.model.ImportedTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

sealed class WaypointUpdate {
    data class Added(val trackId: Int, val point: Pair<Double, Double>) : WaypointUpdate()
    data class Removed(val trackId: Int, val index: Int) : WaypointUpdate()
    data class Cleared(val trackId: Int) : WaypointUpdate()
}


@HiltViewModel
class MapViewModel @Inject constructor(
    private val addWaypointUseCase: AddWaypointUseCase,
    private val clearAllUseCase: ClearAllUseCase,
    private val createTrackUseCase: CreateTrackUseCase,
    private val getTrackWaypointsUseCase: GetTrackWaypointsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _waypointEvents = MutableSharedFlow<WaypointUpdate>()
    val waypointEvents: SharedFlow<WaypointUpdate> = _waypointEvents

    private val trackWaypointIndexes = mutableMapOf<Int, Double>()
    private var selectedTrackId:Int = 0

    /**
     * Clear database at init
     */
    init {
        viewModelScope.launch {
            clearAll()
        }
    }

    /**
     * Add waypoint to selected track
     *
     * @param lat
     * @param lng
     */
    fun addWaypoint(lat: Double, lng: Double) {
        // If no track is selected, do nothing
        if (selectedTrackId == 0) return

        val currentIndex = trackWaypointIndexes[selectedTrackId] ?: 0.0

        viewModelScope.launch {
            addWaypointUseCase(lat, lng, currentIndex.toDouble(), selectedTrackId)
            // Notify observers that a waypoint was added
            _waypointEvents.emit(WaypointUpdate.Added(selectedTrackId, lat to lng))
            // Update index only after successful addition
            trackWaypointIndexes[selectedTrackId] = currentIndex + 1
        }
    }


    /**
     * Create new track entity if no track is selected
     *
     * @return
     */
    suspend fun createNewTrackIfNeeded(): Int {
        // If no track selected
        if (selectedTrackId == 0) {
            //Set default track name
            val newId = createTrackUseCase(context.getString(R.string.track_no_name))
            selectedTrackId = newId
            trackWaypointIndexes[selectedTrackId] = 0.0
            Log.d("debug", "Created new track with ID: $newId")
        }
        return selectedTrackId
    }

    fun clearAll(){
        viewModelScope.launch {
            clearAllUseCase()
        }
    }

    /**
     * Manage single tap
     *
     * @param geoPoint
     */
    suspend fun singleTapConfirmed(geoPoint: GeoPoint){
        val trackId = createNewTrackIfNeeded()
        addWaypoint(geoPoint.latitude, geoPoint.longitude)
        Log.d("debug", "Added waypoint to track $trackId at: ${geoPoint.latitude}, ${geoPoint.longitude}")
    }




}