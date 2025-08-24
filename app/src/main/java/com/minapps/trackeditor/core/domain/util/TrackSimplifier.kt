package com.minapps.trackeditor.core.domain.util

import android.view.WindowInsetsAnimation
import com.minapps.trackeditor.core.domain.model.Waypoint
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

class TrackSimplifier @Inject constructor(){

    fun simplify(points: List<Waypoint>, tolerance: Double): MutableList<Waypoint> {
        if (points.size < 3) return points.toMutableList()
        return douglasPeucker(points, tolerance)
    }

    private fun douglasPeucker(points: List<Waypoint>, epsilon: Double): MutableList<Waypoint> {
        if (points.size < 3) return points.toMutableList()

        var maxDistance = 0.0
        var index = 0
        val end = points.lastIndex

        for (i in 1 until end) {
            val d = perpendicularDistance(points[i], points[0], points[end])
            if (d > maxDistance) {
                index = i
                maxDistance = d
            }
        }

        return if (maxDistance > epsilon) {
            val recResults1 = douglasPeucker(points.subList(0, index + 1), epsilon)
            val recResults2 = douglasPeucker(points.subList(index, points.size), epsilon)

            // merge: avoid duplicating the split point
            (recResults1.dropLast(1) + recResults2).toMutableList()
        } else {
            mutableListOf(points.first(), points.last())
        }
    }

    private fun perpendicularDistance(p: Waypoint, start: Waypoint, end: Waypoint): Double {
        val R = 6371000.0 // Earth radius in meters

        val lat1 = Math.toRadians(start.lat)
        val lon1 = Math.toRadians(start.lng)
        val lat2 = Math.toRadians(end.lat)
        val lon2 = Math.toRadians(end.lng)
        val lat0 = Math.toRadians(p.lat)
        val lon0 = Math.toRadians(p.lng)

        val x1 = lon1 * cos((lat1 + lat2)/2)
        val y1 = lat1
        val x2 = lon2 * cos((lat1 + lat2)/2)
        val y2 = lat2
        val x0 = lon0 * cos((lat1 + lat2)/2)
        val y0 = lat0

        val dx = x2 - x1
        val dy = y2 - y1

        if (dx == 0.0 && dy == 0.0) {
            return R * sqrt((x0 - x1)*(x0 - x1) + (y0 - y1)*(y0 - y1))
        }

        val t = ((x0 - x1)*dx + (y0 - y1)*dy) / (dx*dx + dy*dy)
        val xProj = x1 + t*dx
        val yProj = y1 + t*dy

        return R * sqrt((x0 - xProj)*(x0 - xProj) + (y0 - yProj)*(y0 - yProj))
    }



    fun getVisibleSegment(points: List<Waypoint>, north: Double, south: Double, east: Double, west: Double): List<Waypoint>? {
        // Filter points within bounds, optionally simplify
        return null
    }

}
