package com.minapps.trackeditor.feature_track_import.data.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import com.minapps.trackeditor.data.mapper.toEntity
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

class GpxParser @Inject constructor(
    private val editTrackRepository: EditTrackRepositoryItf
) : TrackParser {

    override fun canHandle(fileUri: Uri): Boolean {
        return fileUri.toString().endsWith(".gpx", ignoreCase = true)
    }

    /**
     * Parse GPX file from Uri, processing waypoints in batches via the provided callback.
     * This method streams and does not keep all waypoints in memory.
     *
     * @param context Application context
     * @param uri File Uri to parse
     * @param onWaypointsBatch suspend callback invoked with each batch of parsed waypoints
     * @return Track metadata (id=0 because not stored yet, name, etc.)
     */
    override suspend fun parse(context: Context, fileUri: Uri): Boolean {
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType

            var trackName = "Imported GPX Track"
            var lat = 0.0
            var lon = 0.0
            var ele: Double? = null
            var time: String? = null
            var waypointId = 0.0

            // Insert Track first to get trackId
            val newTrackId = editTrackRepository.insertTrack(
                Track(0, trackName, "", System.currentTimeMillis(), emptyList()).toEntity()
            ).toInt()

            val waypointBatch = mutableListOf<Waypoint>()
            val batchSize = 50

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "trkpt" -> {
                                lat = parser.getAttributeValue(null, "lat").toDouble()
                                lon = parser.getAttributeValue(null, "lon").toDouble()
                                ele = null
                                time = null
                            }
                            "ele" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    ele = parser.text.toDoubleOrNull()
                                }
                            }
                            "time" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    time = parser.text
                                }
                            }
                            "name" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    trackName = parser.text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "trkpt") {
                            val waypoint = Waypoint(
                                id = waypointId,
                                lat = lat,
                                lng = lon,
                                elv = ele,
                                time = time,
                                trackId = newTrackId
                            )
                            waypointBatch.add(waypoint)
                            waypointId++

                            if (waypointBatch.size >= batchSize) {
                                editTrackRepository.addWaypoints(waypointBatch)
                                waypointBatch.clear()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            // Insert any remaining waypoints
            if (waypointBatch.isNotEmpty()) {
                editTrackRepository.addWaypoints(waypointBatch)
            }

            // Update track name if changed during parsing (optional)
            //editTrackRepository.updateTrackName(newTrackId, trackName)

            // Return full Track with waypoints loaded
            return true
        }
        return false
    }



    // You can keep your original parse() for backward compatibility,
    // or just remove it in favor of this streaming approach.
}


/**
 * Parses the GPX file, extracting track name and waypoints.
 * Handles XML parsing exceptions and returns null if parsing fails.
 */
/*
override suspend fun parse(context: Context, fileUri: Uri): Track? {
    val waypoints = mutableListOf<Waypoint>()
    var inputStream: InputStream? = null

    try {
        inputStream = context.contentResolver.openInputStream(fileUri)
            ?: throw IOException("Cannot open input stream")

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var name = "Imported GPX Track"

        var waypointId = 0.0
        var trackId = 0
        var lat = 0.0
        var lon = 0.0
        var ele: Double? = null
        var time: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "trkseg" -> trackId++

                        "trkpt" -> {
                            lat = parser.getAttributeValue(null, "lat").toDouble()
                            lon = parser.getAttributeValue(null, "lon").toDouble()
                            ele = null // reset elevation for this point
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
                    if (parser.name == "trkpt") {
                        waypoints.add(Waypoint(waypointId, lat, lon, ele, time, trackId))
                        waypointId++
                    }
                }
            }
            eventType = parser.next()
        }


        Log.d("GpxParser", "Track parsed successfully: $name with ${waypoints.size} waypoints")
        //TODO set real values
        return Track(0,name, "",0, waypoints)

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

    // Return null on error
    return null
}*/

