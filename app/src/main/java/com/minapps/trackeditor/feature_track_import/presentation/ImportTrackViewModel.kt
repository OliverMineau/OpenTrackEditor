package com.minapps.trackeditor.feature_track_import.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minapps.trackeditor.feature_track_import.domain.model.ImportedTrack
import com.minapps.trackeditor.feature_track_import.domain.usecase.TrackImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ImportTrackViewModel @Inject constructor(
    private val trackImportUseCase: TrackImportUseCase
) : ViewModel() {


    /**
     * Launches a coroutine to import a track from the given URI.
     *
     * @param uri The Uri pointing to the track file to be imported.
     */
    fun importTrack(uri: Uri) {
        viewModelScope.launch {
            val importedTrack = trackImportUseCase(uri)
            if (importedTrack != null) {
                // Save to DB via another UseCase or repository method
                //TODO
                //saveImportedTrackUseCase(importedTrack)
                // Update UI state if needed
            } else {
                // Handle error case
                //Display toast error message
            }
        }
    }
}