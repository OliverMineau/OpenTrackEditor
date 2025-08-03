package com.minapps.trackeditor.feature_track_import.domain.model

import com.minapps.trackeditor.core.domain.model.Waypoint


/**
 * Data model representing a track that has been imported,
 * typically from an external file or source.
 *
 * @param name The name of the imported track.
 * @param waypoints The list of waypoints that make up the track.
 */
data class ImportedTrack(
    //TODO add rest of data
    val name: String,
    val waypoints: List<Waypoint>
)