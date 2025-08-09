package com.minapps.trackeditor.feature_track_import.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.feature_map_editor.domain.usecase.AddImportedTrackUseCase
import com.minapps.trackeditor.feature_track_import.domain.usecase.TrackImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ImportTrackViewModel @Inject constructor(
    private val trackImportUseCase: TrackImportUseCase,
    private val addImportedTrackUseCase: AddImportedTrackUseCase,
) : ViewModel() {

    /**
     * Launches a coroutine to import a track from the given URI.
     *
     * @param uri The Uri pointing to the track file to be imported.
     */
    fun importTrack(uri: Uri) {
        viewModelScope.launch {
            val importedTrack = trackImportUseCase(uri) ?: return@launch
            Log.d("debug", "Imported track")

            //TODO tell track was or wasnt imported
            Log.d("debug", "Displaying track...")
            //addImportedTrackUseCase(importedTrack)
        }
    }
}