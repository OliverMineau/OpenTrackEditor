package com.minapps.trackeditor.feature_map_editor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.feature_map_editor.domain.usecase.AddWaypointUseCase
import com.minapps.trackeditor.feature_map_editor.domain.usecase.ClearAllUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MapViewModel @Inject constructor(
    private val addWaypointUseCase: AddWaypointUseCase,
    private val clearAllUseCase: ClearAllUseCase
) : ViewModel() {

    fun addWaypoint(lat: Double, lng: Double) {
        viewModelScope.launch {
            addWaypointUseCase(lat, lng)
        }
    }

    fun clearAll(){
        viewModelScope.launch {
            clearAllUseCase()
        }
    }
}