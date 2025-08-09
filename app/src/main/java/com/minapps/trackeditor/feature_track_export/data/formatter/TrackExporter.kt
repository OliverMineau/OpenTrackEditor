package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track
import java.io.OutputStream

interface TrackExporter {
    fun export(track: Track, outputStream: OutputStream)
}
