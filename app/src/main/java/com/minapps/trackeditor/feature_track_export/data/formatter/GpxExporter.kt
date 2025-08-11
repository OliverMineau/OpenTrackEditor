package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import jakarta.inject.Inject
import java.io.Writer
import java.util.Locale
import kotlin.text.iterator

/**
 * GPX Exporter
 * Creates GPX file from in app data
 *
 * TODO Populate all possible GPX metadata
 * TODO Make it work for multiple tracks
 *
 */
class GpxExporter @Inject constructor() : TrackExporter {

    // Export File Header
    override fun exportHeader(
        track: Track, writer: Writer
    ) {
        writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
        writer.write("\n")
        writer.write(
            """
    <gpx
        version="1.1"
        creator="OpenTrackEditorApp"
        xmlns="http://www.topografix.com/GPX/1/1"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"
    >
    """.trimIndent()
        )
        writer.write("\n")
        writer.write(
            """
    <metadata>
      <link href="https://olivermineau.com/opentrackeditor">
        <text>Edited with OpenTrackEditor, an OpenSource app</text>
        <type>website</type>
      </link>
    </metadata>
    """.trimIndent()
        )
        writer.write("\n")

    }

    // Export Track Segment Header
    override fun exportTrackSegmentHeader(name: String, writer: Writer) {
        writer.write("<trk>")
        writer.write("\n")

        writer.write("  <name>${escapeXml(name)}</name>")
        writer.write("\n")
        writer.write("  <trkseg>")
        writer.write("\n")
    }

    // Export Waypoints
    override fun exportWaypoints(waypoints: List<Waypoint>, writer: Writer) {

        for (wp in waypoints) {

            // Use Locale.US format to get '.' in coords
            val latStr = String.format(Locale.US, "%f", wp.lat)
            val lonStr = String.format(Locale.US, "%f", wp.lng)

            writer.write("    <trkpt lat=\"$latStr\" lon=\"$lonStr\">")
            writer.write("\n")
            wp.elv?.let {
                writer.write("      <ele>${String.format(Locale.US, "%f", it)}</ele>")
                writer.write("\n")
            }

            wp.time?.let {
                writer.write("      <time>$it</time>")
                writer.write("\n")
            }

            writer.write("    </trkpt>")
            writer.write("\n")
        }


    }

    // Export Track Segment Footer
    override fun exportTrackSegmentFooter(writer: Writer) {
        writer.write("  </trkseg>")
        writer.write("\n")
        writer.write("</trk>")
        writer.write("\n")
    }

    // Export File Footer
    override fun exportFooter(writer: Writer) {
        writer.write("</gpx>")
        writer.write("\n")
    }

    // Convert to safe string
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
