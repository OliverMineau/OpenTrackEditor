package com.minapps.trackeditor.feature_track_import.domain.usecase

/**
 * Data on import/export status
 *
 */
sealed class DataStreamProgress {
    data class Progress(val percent: Int) : DataStreamProgress()
    data class Completed(val trackId: Int) : DataStreamProgress()
    data class Error(val message: String) : DataStreamProgress()
}
