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
 * Parser implementation for GPX files.
 * Reads GPX XML data from the provided Uri and converts it to an ImportedTrack.
 */
class GpxParser @Inject constructor() : TrackParser {

    /**
     * Checks if the file Uri corresponds to a GPX file by extension.
     */
    override fun canHandle(fileUri: Uri): Boolean {
        return fileUri.toString().endsWith(".gpx", ignoreCase = true)
    }

    /**
     * Parses the GPX file, extracting track name and waypoints.
     * Handles XML parsing exceptions and returns null if parsing fails.
     */
    override suspend fun parse(context: Context, fileUri: Uri, fileSize : Long, chunkSize: Int): Flow<ParsedData> = flow {
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
            var name = "Imported GPX Track"
            var waypointId = 0.0
            //var trackId = 0
            var lat = 0.0
            var lon = 0.0
            var ele: Double? = null
            var time: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {

                            // If start of segment get track id
                            "trkseg" -> {
                                //trackId++
                                waypointId = 0.0
                                emit(ParsedData.TrackMetadata(Track(0, name, "", 0, null)))
                            }

                            "trkpt" -> {
                                lat = parser.getAttributeValue(null, "lat").toDouble()
                                lon = parser.getAttributeValue(null, "lon").toDouble()
                                ele = null
                            }

                            "ele" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    ele = parser.text.toDouble()
                                }
                            }

                            "name" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    name = parser.text
                                }
                            }

                            "time" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    time = parser.text
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {

                        // If waypoint read, add to list
                        if (parser.name == "trkpt") {
                            waypoints.add(Waypoint(waypointId, lat, lon, ele, time, 0))
                            waypointId++

                            // If list full (chunkSize), send it to be added to database
                            if (waypoints.size >= chunkSize) {
                                emit(ParsedData.Waypoints(waypoints))
                                waypoints.clear()
                            }
                        }

                        // If end of segment flush waypoints
                        if (parser.name == "trkseg") {
                            if (waypoints.isNotEmpty()) {
                                emit(ParsedData.Waypoints(waypoints))
                                waypoints.clear()
                            }
                            waypointId = 0.0
                        }
                    }
                }

                // Emit progress every 1000 waypoints
                if (totalFileSize > 0 && waypointId % 1000 == 0.0) {
                    val progressPercent = (countingStream.bytesRead * 100 / totalFileSize).toInt()
                    emit(ParsedData.Progress(progressPercent))
                }

                eventType = parser.next()
            }

            if (waypoints.isNotEmpty()) {
                emit(ParsedData.Waypoints(waypoints))
                waypoints.clear()
            }


            // Emit 100% progress on finish
            emit(ParsedData.Progress(100))

        } catch (e: XmlPullParserException) {
            Log.e("GpxParser", "XML parsing error: ${e.message}", e)
        } catch (e: IOException) {
            Log.e("GpxParser", "IO error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("GpxParser", "Unexpected error: ${e.message}", e)
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: IOException) {
            }
        }
    }

}
