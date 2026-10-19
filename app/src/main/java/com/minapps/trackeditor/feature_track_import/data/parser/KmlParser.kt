package com.minapps.trackeditor.feature_track_import.data.parser

import android.content.Context
import android.net.Uri
import com.minapps.trackeditor.core.domain.model.Track
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.feature_map_editor.presentation.util.CountingInputStream
import com.minapps.trackeditor.feature_track_import.domain.model.ImportDiagnostic
import com.minapps.trackeditor.feature_track_import.domain.model.ImportDiagnosticKind
import com.minapps.trackeditor.feature_track_import.domain.model.ImportDiagnosticScope
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException

class KmlParser @Inject constructor() : TrackParser {
    override fun canHandle(fileUri: Uri): Boolean = fileUri.toString().endsWith(".kml", ignoreCase = true)

    override suspend fun parse(context: Context, fileUri: Uri, fileSize: Long, chunkSize: Int): Flow<ParsedData> {
        val buffer = mutableListOf<Waypoint>()
        var trackName = "Imported KML Track"
        var inTrack = false
        val coordinateText = StringBuilder()
        var inCoordinates = false

        return flow {
            val stream = context.contentResolver.openInputStream(fileUri)
                ?: throw IOException("Cannot open input stream")

            CountingInputStream(stream).use { countingStream ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(countingStream, null)

                var event = parser.eventType
                var pointId = 0.0
                var pointTime: String? = null
                var placemarkDepth = 0
                var nameDepth = -1
                var whenDepth = -1
                var depth = 0
                var lastProgress = -1
                var trackKind: String? = null
                var rootSeen = false
                var rootClosed = false

                suspend fun kotlinx.coroutines.flow.FlowCollector<ParsedData>.flush() {
                    if (buffer.isNotEmpty()) {
                        emit(ParsedData.Waypoints(buffer.toList()))
                        buffer.clear()
                    }
                }

                suspend fun kotlinx.coroutines.flow.FlowCollector<ParsedData>.diagnose(message: String) {
                    emit(ParsedData.Diagnostic(ImportDiagnostic(
                        null, trackName, message,
                        parser.lineNumber.takeIf { it > 0 },
                        parser.columnNumber.takeIf { it > 0 }
                    )))
                }

                suspend fun kotlinx.coroutines.flow.FlowCollector<ParsedData>.append(
                    lonText: String?, latText: String?, eleText: String?, raw: String, timestamp: String?
                ) {
                    val lon = lonText?.toDoubleOrNull()
                    val lat = latText?.toDoubleOrNull()
                    val ele = eleText?.toDoubleOrNull()
                    if (lon == null || lat == null || !lon.isFinite() || !lat.isFinite() ||
                        lon !in -180.0..180.0 || lat !in -90.0..90.0 ||
                        (eleText != null && (ele == null || !ele.isFinite()))
                    ) {
                        diagnose("Invalid KML coordinate: $raw")
                        return
                    }
                    buffer.add(Waypoint(pointId++, lat, lon, ele, timestamp, 0))
                    if (buffer.size >= chunkSize.coerceAtLeast(1)) flush()
                }

                suspend fun kotlinx.coroutines.flow.FlowCollector<ParsedData>.consumeCoordinates(
                    text: String, finish: Boolean = false
                ) {
                    coordinateText.append(text)
                    var end = 0
                    while (end < coordinateText.length) {
                        if (!coordinateText[end].isWhitespace()) {
                            end++
                            continue
                        }
                        val tuple = coordinateText.substring(0, end)
                        coordinateText.delete(0, end + 1)
                        end = 0
                        if (tuple.isNotEmpty()) {
                            val parts = tuple.split(",")
                            if (parts.size !in 2..3) diagnose("Invalid KML coordinate tuple: $tuple")
                            else append(parts[0], parts[1], parts.getOrNull(2), tuple, null)
                        }
                    }
                    if (finish && coordinateText.isNotEmpty()) {
                        val tuple = coordinateText.toString()
                        coordinateText.clear()
                        val parts = tuple.split(",")
                        if (parts.size !in 2..3) diagnose("Invalid KML coordinate tuple: $tuple")
                        else append(parts[0], parts[1], parts.getOrNull(2), tuple, null)
                    }
                }

                while (event != XmlPullParser.END_DOCUMENT) {
                    val localName = parser.name?.substringAfter(':')
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            depth++
                            when (localName) {
                                "kml" -> if (depth == 1) rootSeen = true
                                "Placemark" -> {
                                    placemarkDepth = depth
                                    trackName = "Imported KML Track"
                                    pointTime = null
                                }
                                "name" -> if (placemarkDepth > 0 && depth == placemarkDepth + 1) {
                                    nameDepth = depth
                                }
                                "LineString", "Track" -> if (!inTrack && placemarkDepth > 0) {
                                    inTrack = true
                                    trackKind = localName
                                    pointId = 0.0
                                    emit(ParsedData.TrackMetadata(Track(0, trackName, "", 0, null)))
                                }
                                "when" -> if (inTrack) whenDepth = depth
                                "coord" -> if (inTrack && trackKind == "Track") {
                                    val raw = parser.nextText().trim()
                                    val parts = raw.split(Regex("\\s+"))
                                    if (parts.size !in 2..3) {
                                        diagnose("Invalid KML gx:coord: $raw")
                                    } else {
                                        append(parts[0], parts[1], parts.getOrNull(2), raw, pointTime)
                                    }
                                    depth--
                                }
                                "coordinates" -> if (inTrack && trackKind == "LineString") {
                                    inCoordinates = true
                                    coordinateText.clear()
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inCoordinates) consumeCoordinates(parser.text)
                            if (nameDepth > 0) {
                                trackName = parser.text.trim().ifEmpty { trackName }
                            }
                            if (whenDepth > 0) pointTime = parser.text.trim()
                        }
                        XmlPullParser.END_TAG -> {
                            when (localName) {
                                "kml" -> if (depth == 1) rootClosed = true
                                "coordinates" -> if (inCoordinates) {
                                    consumeCoordinates("", finish = true)
                                    inCoordinates = false
                                }
                                "name" -> if (depth == nameDepth) nameDepth = -1
                                "when" -> if (depth == whenDepth) whenDepth = -1
                                "LineString", "Track" -> if (inTrack && localName == trackKind) {
                                    flush()
                                    inTrack = false
                                    trackKind = null
                                }
                                "Placemark" -> if (depth == placemarkDepth) placemarkDepth = 0
                            }
                            depth--
                        }
                    }

                    if (fileSize > 0 && pointId > 0 && pointId % 1000.0 == 0.0) {
                        val percent = (countingStream.bytesRead * 100 / fileSize).toInt().coerceIn(0, 100)
                        if (percent != lastProgress) {
                            lastProgress = percent
                            emit(ParsedData.Progress(percent))
                        }
                    }
                    event = parser.next()
                }

                if (!rootSeen || !rootClosed || depth != 0 || inTrack || inCoordinates) {
                    throw XmlPullParserException("Unexpected end of KML document: unclosed KML structure", parser, null)
                }

                flush()
                emit(ParsedData.Progress(100))
            }
        }.catch { error ->
            if (error !is XmlPullParserException) throw error
            val scope = if (inTrack) ImportDiagnosticScope.TRACK else ImportDiagnosticScope.DOCUMENT
            val diagnosticName = if (inTrack) trackName else "KML document"
            val location = if (inTrack) "track" else "document"
            emit(ParsedData.Diagnostic(ImportDiagnostic(
                null, diagnosticName, "KML $location interrupted by malformed XML: ${error.message}",
                error.lineNumber.takeIf { it > 0 },
                error.columnNumber.takeIf { it > 0 },
                scope = scope,
                kind = ImportDiagnosticKind.STRUCTURAL
            )))
            if (buffer.isNotEmpty()) {
                emit(ParsedData.Waypoints(buffer.toList()))
                buffer.clear()
            }
            throw error
        }
    }
}
