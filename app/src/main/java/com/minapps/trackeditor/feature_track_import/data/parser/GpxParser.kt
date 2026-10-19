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

class GpxParser @Inject constructor() : TrackParser {
    override fun canHandle(fileUri: Uri): Boolean = fileUri.toString().endsWith(".gpx", ignoreCase = true)

    override suspend fun parse(context: Context, fileUri: Uri, fileSize: Long, chunkSize: Int): Flow<ParsedData> {
        // State lives outside the producer so upstream-only catch can flush after malformed XML.
        val buffer = mutableListOf<Waypoint>()
        var trackName = "Imported GPX Track"
        var segmentActive = false
        var trackActive = false
        var rootOpen = false
        var rootSeen = false

        return flow {
            val stream = context.contentResolver.openInputStream(fileUri)
                ?: throw IOException("Cannot open input stream")

            CountingInputStream(stream).use { countingStream ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(countingStream, null)

                var event = parser.eventType
                var pointId = 0.0
                var latitude = 0.0
                var longitude = 0.0
                var elevation: Double? = null
                var time: String? = null
                var pointValid = false
                var pointActive = false
                var inTrack = false
                var inTrackName = false
                var inPointElevation = false
                var inPointTime = false
                var lastProgress = -1

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "gpx" -> {
                                rootSeen = true
                                rootOpen = true
                            }
                            "trk" -> {
                                inTrack = true
                                trackActive = true
                            }
                            "name" -> if (inTrack && !segmentActive && !pointActive) inTrackName = true
                            "trkseg" -> {
                                segmentActive = true
                                pointId = 0.0
                                emit(ParsedData.TrackMetadata(Track(0, trackName, "", 0, null)))
                            }
                            "trkpt" -> if (segmentActive) {
                                pointActive = true
                                elevation = null
                                time = null
                                val latText = parser.getAttributeValue(null, "lat")
                                val lonText = parser.getAttributeValue(null, "lon")
                                val lat = latText?.toDoubleOrNull()
                                val lon = lonText?.toDoubleOrNull()
                                pointValid = lat != null && lon != null &&
                                    lat.isFinite() && lon.isFinite() &&
                                    lat in -90.0..90.0 && lon in -180.0..180.0
                                if (pointValid) {
                                    latitude = lat!!
                                    longitude = lon!!
                                } else {
                                    emit(ParsedData.Diagnostic(ImportDiagnostic(
                                        null, trackName,
                                        "Invalid GPX waypoint coordinates: lat=$latText, lon=$lonText",
                                        parser.lineNumber.takeIf { it > 0 },
                                        parser.columnNumber.takeIf { it > 0 }
                                    )))
                                }
                            }
                            "ele" -> if (pointActive) inPointElevation = true
                            "time" -> if (pointActive) inPointTime = true
                        }

                        XmlPullParser.TEXT -> {
                            val value = parser.text
                            when {
                                inTrackName -> trackName = value.trim().ifEmpty { trackName }
                                inPointElevation && pointValid -> {
                                    val parsed = value.trim().toDoubleOrNull()
                                    if (parsed != null && parsed.isFinite()) {
                                        elevation = parsed
                                    } else {
                                        pointValid = false
                                        emit(ParsedData.Diagnostic(ImportDiagnostic(
                                            null, trackName, "Invalid GPX waypoint elevation: $value",
                                            parser.lineNumber.takeIf { it > 0 },
                                            parser.columnNumber.takeIf { it > 0 }
                                        )))
                                    }
                                }
                                inPointTime -> time = value.trim()
                            }
                        }

                        XmlPullParser.END_TAG -> when (parser.name) {
                            "name" -> inTrackName = false
                            "ele" -> inPointElevation = false
                            "time" -> inPointTime = false
                            "trkpt" -> {
                                if (pointActive && pointValid) {
                                    buffer.add(Waypoint(pointId++, latitude, longitude, elevation, time, 0))
                                    if (buffer.size >= chunkSize.coerceAtLeast(1)) {
                                        emit(ParsedData.Waypoints(buffer.toList()))
                                        buffer.clear()
                                    }
                                }
                                pointActive = false
                                pointValid = false
                            }
                            "trkseg" -> {
                                if (buffer.isNotEmpty()) {
                                    emit(ParsedData.Waypoints(buffer.toList()))
                                    buffer.clear()
                                }
                                segmentActive = false
                            }
                            "trk" -> {
                                inTrack = false
                                trackActive = false
                                trackName = "Imported GPX Track"
                            }
                            "gpx" -> rootOpen = false
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

                // Some XmlPullParser implementations report END_DOCUMENT for truncated XML
                // without throwing. Do not turn an unfinished GPX document into success.
                if (!rootSeen || rootOpen || trackActive || segmentActive) {
                    throw XmlPullParserException(
                        "Unexpected end of GPX document: unclosed GPX, track, or segment",
                        parser,
                        null
                    )
                }

                if (buffer.isNotEmpty()) {
                    emit(ParsedData.Waypoints(buffer.toList()))
                    buffer.clear()
                }
                emit(ParsedData.Progress(100))
            }
        }.catch { error ->
            // Flow.catch sees upstream exceptions, not failures from downstream collectors.
            if (error !is XmlPullParserException) throw error
            if (segmentActive || trackActive || rootOpen) {
                val scope = when {
                    segmentActive -> "segment"
                    trackActive -> "track"
                    else -> "document"
                }
                val diagnosticName = if (scope == "document") "GPX document" else trackName
                emit(ParsedData.Diagnostic(ImportDiagnostic(
                    null, diagnosticName, "GPX $scope interrupted by malformed XML: ${error.message}",
                    error.lineNumber.takeIf { it > 0 },
                    error.columnNumber.takeIf { it > 0 },
                    scope = if (scope == "document") ImportDiagnosticScope.DOCUMENT else ImportDiagnosticScope.TRACK,
                    kind = ImportDiagnosticKind.STRUCTURAL
                )))
            }
            if (buffer.isNotEmpty()) {
                emit(ParsedData.Waypoints(buffer.toList()))
                buffer.clear()
            }
            throw error
        }
    }
}
