package com.minapps.trackeditor.feature_track_import.domain.model

/**
 * Data on import/export status
 *
 */
sealed class DataStreamProgress {
    data class Progress(val percent: Int) : DataStreamProgress()
    data class Completed(val trackIds: MutableList<Int>) : DataStreamProgress()
    data class Error(val message: String) : DataStreamProgress()
}