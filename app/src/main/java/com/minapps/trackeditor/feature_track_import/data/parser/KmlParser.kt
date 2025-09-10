package com.minapps.trackeditor.feature_track_import.data.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.feature_map_editor.presentation.util.CountingInputStream
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

/**
 * Parser implementation for KML files.
 * Reads KML XML data from the provided Uri and converts it to an ImportedTrack.
 */
class KmlParser @Inject constructor() : TrackParser {

    override fun canHandle(fileUri: Uri): Boolean {
        return fileUri.toString().endsWith(".kml", ignoreCase = true)
    }

    override suspend fun parse(
        context: Context,
        fileUri: Uri,
        fileSize: Long,
        chunkSize: Int
    ): Flow<ParsedData> = flow {
        val waypoints = mutableListOf<Waypoint>()
        var inputStream: InputStream? = null

        try {
            val totalFileSize = fileSize

            val rawInputStream = context.contentResolver.openInputStream(fileUri)
                ?: throw IOException("Cannot open input stream")

            val countingStream = CountingInputStream(rawInputStream)
            inputStream = countingStream

            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(countingStream, null)

            var eventType = parser.eventType
            var name = "Imported KML Track"
            var waypointId = 0.0
            var lat = 0.0
            var lon = 0.0
            var ele: Double? = null
            var time: String? = null

            var inTrack = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {

                            // Get name
                            "name" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    name = parser.text
                                }

                            }

                            // Create track
                            "gx:Track" -> {
                                waypointId = 0.0
                                emit(ParsedData.TrackMetadata(Track(0, name, "", 0, null)))
                                inTrack = true
                            }

                            // Create track
                            "LineString" -> {
                                waypointId = 0.0
                                emit(ParsedData.TrackMetadata(Track(0, name, "", 0, null)))
                                inTrack = true
                            }

                            // Get creation time
                            "when" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    time = parser.text
                                }
                            }

                            // Get coords (for track)
                            "gx:coord" -> {

                                if (inTrack) {

                                    val coordText = parser.nextText().trim()
                                    val parts = coordText.split(" ")

                                    // If contains at least lat lon
                                    if (parts.size >= 2) {
                                        lon = parts[0].toDouble()
                                        lat = parts[1].toDouble()

                                        // If contains elevation
                                        ele = if (parts.size == 3) parts[2].toDouble() else null
                                        waypoints.add(Waypoint(waypointId, lat, lon, ele, time, 0))
                                        waypointId++
                                    }
                                }
                            }

                            // Get coords (for line string)
                            "coordinates" -> {

                                if (inTrack) {

                                    val coordBlock = parser.nextText().trim()
                                    val coords = coordBlock.split("\\s+".toRegex())

                                    // For each block of lat lng (elevation)
                                    for (coord in coords) {
                                        val parts = coord.split(",")
                                        if (parts.size >= 2) {
                                            lon = parts[0].toDouble()
                                            lat = parts[1].toDouble()
                                            ele = if (parts.size == 3) parts[2].toDouble() else null
                                            waypoints.add(
                                                Waypoint(
                                                    waypointId,
                                                    lat,
                                                    lon,
                                                    ele,
                                                    null,
                                                    0
                                                )
                                            )
                                            waypointId++
                                        }
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "gx:Track" -> {
                                if (waypoints.isNotEmpty()) {
                                    emit(ParsedData.Waypoints(waypoints.toList()))
                                    waypoints.clear()
                                }
                                inTrack = false
                            }

                            "LineString" -> {
                                if (waypoints.isNotEmpty()) {
                                    emit(ParsedData.Waypoints(waypoints.toList()))
                                    waypoints.clear()
                                }
                                inTrack = false
                            }
                        }
                    }
                }

                // Emit chunk when buffer full
                if (waypoints.size >= chunkSize) {
                    emit(ParsedData.Waypoints(waypoints.toList()))
                    waypoints.clear()
                }

                // Emit progress every 1000 waypoints
                if (totalFileSize > 0 && waypointId % 1000 == 0.0) {
                    val progressPercent = (countingStream.bytesRead * 100 / totalFileSize).toInt()
                    emit(ParsedData.Progress(progressPercent))
                }

                eventType = parser.next()
            }

            // Flush remaining waypoints
            if (waypoints.isNotEmpty()) {
                emit(ParsedData.Waypoints(waypoints))
                waypoints.clear()
            }

            // Emit 100% progress on finish
            emit(ParsedData.Progress(100))

        } catch (e: XmlPullParserException) {
            Log.e("KmlParser", "XML parsing error: ${e.message}", e)
        } catch (e: IOException) {
            Log.e("KmlParser", "IO error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("KmlParser", "Unexpected error: ${e.message}", e)
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: IOException) {
            }
        }
    }
}
