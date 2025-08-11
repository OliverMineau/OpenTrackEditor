package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import java.io.OutputStream
import java.io.Writer

/**
 * Track exporters interface they should implement
 *
 */
interface TrackExporter {

    fun exportHeader(track: Track, writer: Writer)
    fun exportFooter(writer: Writer)

    fun exportWaypoints(waypoints: List<Waypoint>, writer: Writer)

    fun exportTrackSegmentHeader(name: String, writer: Writer)
    fun exportTrackSegmentFooter(writer: Writer)


}
