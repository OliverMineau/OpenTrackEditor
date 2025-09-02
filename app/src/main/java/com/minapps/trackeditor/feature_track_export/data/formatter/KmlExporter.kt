package com.minapps.trackeditor.feature_track_export.data.formatter

import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import jakarta.inject.Inject
import java.io.Writer
import java.util.Locale
import kotlin.text.iterator

/**
 * KML Exporter
 * Creates KML file from in app data
 *
 * TODO Populate all possible KML metadata
 *
 */
class KmlExporter @Inject constructor() : TrackExporter {

    // Export File Header
    override fun exportHeader(
        writer: Writer
    ) {
        writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
        writer.write("\n")
        writer.write(
            """
                <kml xmlns="http://www.opengis.net/kml/2.2">
                <Document>
                <name>OpenTrackEditor</name>
                <description>
                <![CDATA[
                  Edited with <a href="https://olivermineau.com/opentrackeditor">OpenTrackEditor</a>
                ]]>
                </description>
            """.trimIndent()
        )
        writer.write("\n")
    }


    // Export Track Segment Header
    override fun exportTrackSegmentHeader(name: String, writer: Writer) {
        writer.write("<Placemark>")
        writer.write("\n")

        writer.write("  <name>${escapeXml(name)}</name>")
        writer.write("\n")
        writer.write("  <LineString>")
        writer.write("\n")
        writer.write("      <coordinates>")
        writer.write("\n")
    }

    // Export Waypoints
    override fun exportWaypoints(waypoints: List<Waypoint>, writer: Writer) {

        for (wp in waypoints) {

            // Use Locale.US format to get '.' in coords
            val latStr = String.format(Locale.US, "%f", wp.lat)
            val lonStr = String.format(Locale.US, "%f", wp.lng)

            writer.write("    $lonStr,$latStr${if(wp.elv!=null) ",${wp.elv}" else ""}")
            writer.write("\n")
        }
    }

    // Export Track Segment Footer
    override fun exportTrackSegmentFooter(writer: Writer) {
        writer.write("      </coordinates>")
        writer.write("\n")
        writer.write("  </LineString>")
        writer.write("\n")
        writer.write("</Placemark>")
        writer.write("\n")
    }

    // Export File Footer
    override fun exportFooter(writer: Writer) {
        writer.write("</Document>")
        writer.write("\n")
        writer.write("</kml>")
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
