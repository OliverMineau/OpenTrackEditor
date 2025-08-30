package com.minapps.trackeditor.core.domain.usecase

import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.core.domain.util.MapUpdateViewHelper
import jakarta.inject.Inject

data class Viewport(
    val latNorth: Double,
    val latSouth: Double,
    val lonWest: Double,
    val lonEast: Double,
    val zoom: Double
)

data class MapUpdateResult(
    val tracks: List<Pair<Int, List<Waypoint>>>,
    val snappedZoom: Int,
    val showFull: Boolean,
    val showOutline: Boolean
)

class HandleMapViewportChangeUseCase @Inject constructor(
    private val updateMapViewUseCase: UpdateMapViewUseCase,
    private val mapUpdateViewHelper: MapUpdateViewHelper // your decision helper
) {

    suspend operator fun invoke(
        viewport: Viewport,
        lastZoom: Int?,
        hasDisplayedFull: Boolean,
        hasDisplayedOutline: Boolean
    ): MapUpdateResult? {
        val padding = 0.05
        val latNorthPadded = viewport.latNorth + padding
        val latSouthPadded = viewport.latSouth - padding
        val lonWestPadded = viewport.lonWest - padding
        val lonEastPadded = viewport.lonEast + padding

        // Count the visible waypoints
        val count = updateMapViewUseCase.getVisiblePointCount(
            latNorthPadded, latSouthPadded, lonWestPadded, lonEastPadded
        )

        // Decide whether to show :
        // - full : all visible points of a track or
        // - outline : small amount of visible points (this may create weired segments)
        val decision = mapUpdateViewHelper.decide(
            viewport.zoom,
            count,
            lastZoom,
            hasDisplayedFull,
            hasDisplayedOutline
        )

        // Fetch the actual track data based on the decision
        val tracks = updateMapViewUseCase(
            latNorthPadded, latSouthPadded, lonWestPadded, lonEastPadded,
            decision.showOutline, decision.showFull
        ) ?: return null

        return MapUpdateResult(
            tracks = tracks,
            snappedZoom = decision.snappedZoom,
            showFull = decision.showFull,
            showOutline = decision.showOutline
        )
    }
}
