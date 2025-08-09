package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track
import jakarta.inject.Inject
import java.util.Locale
import kotlin.text.iterator

class GpxExporter @Inject constructor() : TrackExporter{

    override fun export(track: Track): String {
        val sb = StringBuilder()

        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append(
            """<gpx version="1.1" creator="TrackEditorApp" xmlns="http://www.topografix.com/GPX/1/1">"""
        ).append("\n")

        sb.append("<trk>").append("\n")
        sb.append("  <name>${escapeXml(track.name)}</name>").append("\n")
        sb.append("  <trkseg>").append("\n")

        for (wp in track.waypoints) {
            val latStr = String.format(Locale.US, "%f", wp.lat)
            val lonStr = String.format(Locale.US, "%f", wp.lng)

            sb.append("    <trkpt lat=\"$latStr\" lon=\"$lonStr\">").append("\n")
            wp.elv?.let { sb.append("      <ele>${String.format(Locale.US, "%f", it)}</ele>").append("\n") }

            wp.time?.let { sb.append("      <time>$it</time>").append("\n") }

            sb.append("    </trkpt>").append("\n")
        }

        sb.append("  </trkseg>").append("\n")
        sb.append("</trk>").append("\n")
        sb.append("</gpx>").append("\n")

        return sb.toString()
    }

    private fun escapeXml(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return buildString {
            for (ch in input) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }
    }
}
