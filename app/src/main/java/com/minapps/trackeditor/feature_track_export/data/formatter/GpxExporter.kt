package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track
import jakarta.inject.Inject
import java.io.OutputStream
import java.util.Locale
import kotlin.text.iterator

class GpxExporter @Inject constructor() : TrackExporter {

    override fun export(track: Track, outputStream: OutputStream) {

        outputStream.writer().use { writer ->

            writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.write("\n")
            writer.write(
                """<gpx version="1.1" creator="TrackEditorApp" xmlns="http://www.topografix.com/GPX/1/1">"""
            )
            writer.write("\n")

            writer.write("<trk>")
            writer.write("\n")

            writer.write("  <name>${escapeXml(track.name)}</name>")
            writer.write("\n")
            writer.write("  <trkseg>")
            writer.write("\n")

            for (wp in track.waypoints) {
                val latStr = String.format(Locale.US, "%f", wp.lat)
                val lonStr = String.format(Locale.US, "%f", wp.lng)

                writer.write("    <trkpt lat=\"$latStr\" lon=\"$lonStr\">")
                writer.write("\n")
                wp.elv?.let {
                    writer.write("      <ele>${String.format(Locale.US, "%f", it)}</ele>")
                    writer.write("\n")
                }

                wp.time?.let { writer.write("      <time>$it</time>")
                    writer.write("\n") }

                writer.write("    </trkpt>")
                writer.write("\n")
            }

            writer.write("  </trkseg>")
            writer.write("\n")
            writer.write("</trk>")
            writer.write("\n")
            writer.write("</gpx>")
            writer.write("\n")
        }
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
