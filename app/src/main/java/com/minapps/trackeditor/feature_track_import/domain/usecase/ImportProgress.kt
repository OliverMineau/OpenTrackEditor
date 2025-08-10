package com.minapps.trackeditor.feature_track_import.domain.usecase

import com.minapps.trackeditor.core.domain.model.Track

sealed class ImportProgress {
    data class Progress(val percent: Int) : ImportProgress()
    data class Completed(val trackId: Int) : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}
