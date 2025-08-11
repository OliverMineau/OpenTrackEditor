package com.minapps.trackeditor.feature_track_import.data.parser

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint

/**
 * Data sent from parsers to trackimport repo
 *
 */
sealed class ParsedData {
    data class TrackMetadata(val metadata: Track) : ParsedData()
    data class Waypoints(val waypoints: List<Waypoint>) : ParsedData()
    data class Progress(val progress: Int) : ParsedData()
}
