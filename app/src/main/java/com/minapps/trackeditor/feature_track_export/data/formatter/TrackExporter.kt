package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track

interface TrackExporter {
    fun export(track: Track): String
}
