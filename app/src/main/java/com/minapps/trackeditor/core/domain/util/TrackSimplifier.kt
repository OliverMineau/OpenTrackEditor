package com.minapps.trackeditor.core.domain.util

import com.minapps.trackeditor.core.domain.model.Waypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.*

/*
class TrackSimplifier @Inject constructor() {

    suspend fun simplify(points: List<Waypoint>, tolerance: Double): MutableList<Waypoint> {
        if (points.size < 3) return points.toMutableList()

        // 1. Fast radial distance pre-pass
        val reduced = radialDistance(points, tolerance)

        // 2. Douglas–Peucker on reduced set
        return douglasPeucker(reduced, tolerance)
    }

    /**
     * Radial distance simplification (O(n)).
     * Keeps points farther than tolerance from the last kept point.
     */
    private fun radialDistance(points: List<Waypoint>, epsilon: Double): MutableList<Waypoint> {
        if (points.isEmpty()) return mutableListOf()

        val result = mutableListOf(points.first())
        var last = points.first()

        for (i in 1 until points.size) {
            val d = haversineDistance(last, points[i])
            if (d >= epsilon) {
                result.add(points[i])
                last = points[i]
            }
        }

        if (result.last() != points.last()) result.add(points.last())
        return result
    }

    /**
     * Iterative Douglas–Peucker (stack-based).
     * Runs on the reduced set from radial distance.
     */
    private suspend fun douglasPeucker(
        points: List<Waypoint>,
        epsilon: Double
    ): MutableList<Waypoint> = withContext(Dispatchers.Default) {
        if (points.size < 3) return@withContext points.toMutableList()

        val stack = ArrayDeque<Pair<Int, Int>>()
        val keep = BooleanArray(points.size) { false }

        keep[0] = true
        keep[points.lastIndex] = true
        stack.addFirst(0 to points.lastIndex)

        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeFirst()

            var maxDistance = 0.0
            var index = -1
            val a = points[start]
            val b = points[end]

            for (i in (start + 1) until end) {
                val d = perpendicularDistance(points[i], a, b)
                if (d > maxDistance) {
                    maxDistance = d
                    index = i
                }
            }

            if (maxDistance > epsilon && index != -1) {
                keep[index] = true
                stack.addFirst(start to index)
                stack.addFirst(index to end)
            }
        }

        points.filterIndexed { i, _ -> keep[i] }.toMutableList()
    }

    /**
     * Straight-line haversine distance between two points (meters).
     */
    private fun haversineDistance(a: Waypoint, b: Waypoint): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)

        val h = sin(dLat / 2).pow(2.0) +
                cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        return 2 * R * asin(sqrt(h))
    }

    /**
     * Perpendicular distance from point p to segment ab (approx, using planar projection).
     */
    private fun perpendicularDistance(p: Waypoint, start: Waypoint, end: Waypoint): Double {
        val R = 6371000.0

        val lat1 = Math.toRadians(start.lat)
        val lon1 = Math.toRadians(start.lng)
        val lat2 = Math.toRadians(end.lat)
        val lon2 = Math.toRadians(end.lng)
        val lat0 = Math.toRadians(p.lat)
        val lon0 = Math.toRadians(p.lng)

        val x1 = lon1 * cos((lat1 + lat2) / 2)
        val y1 = lat1
        val x2 = lon2 * cos((lat1 + lat2) / 2)
        val y2 = lat2
        val x0 = lon0 * cos((lat1 + lat2) / 2)
        val y0 = lat0

        val dx = x2 - x1
        val dy = y2 - y1

        if (dx == 0.0 && dy == 0.0) {
            return R * sqrt((x0 - x1).pow(2) + (y0 - y1).pow(2))
        }

        val t = ((x0 - x1) * dx + (y0 - y1) * dy) / (dx * dx + dy * dy)
        val xProj = x1 + t * dx
        val yProj = y1 + t * dy

        return R * sqrt((x0 - xProj).pow(2) + (y0 - yProj).pow(2))
    }

}
*/

class TrackSimplifier @Inject constructor(){

    suspend fun simplify(points: List<Waypoint>, tolerance: Double): MutableList<Waypoint> {
        if (points.size < 3) return points.toMutableList()
        return douglasPeucker(points, tolerance)
    }

    suspend fun douglasPeucker(
        points: List<Waypoint>,
        epsilon: Double
    ): MutableList<Waypoint> = withContext(Dispatchers.Default) {
        if (points.size < 3) return@withContext points.toMutableList()

        val stack = ArrayDeque<Pair<Int, Int>>() // index ranges to process
        val keep = BooleanArray(points.size) { false }

        // Always keep endpoints
        keep[0] = true
        keep[points.lastIndex] = true
        stack.addFirst(0 to points.lastIndex)

        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeFirst()

            var maxDistance = 0.0
            var index = -1
            val a = points[start]
            val b = points[end]

            for (i in (start + 1) until end) {
                val d = perpendicularDistance(points[i], a, b)
                if (d > maxDistance) {
                    maxDistance = d
                    index = i
                }
            }

            if (maxDistance > epsilon && index != -1) {
                keep[index] = true
                stack.addFirst(start to index)
                stack.addFirst(index to end)
            }
        }

        points.filterIndexed { i, _ -> keep[i] }.toMutableList()
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