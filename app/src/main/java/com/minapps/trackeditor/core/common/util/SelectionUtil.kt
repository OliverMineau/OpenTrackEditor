package com.minapps.trackeditor.core.common.util

enum class SelectionType{
    TRACK_ONLY,
    POINTS,
    POINTS_ERROR,
    TRACK_ERROR
}

object SelectionUtil {

    fun checkPointsAreFromSameTrack(points: List<Pair<Int,Double>>): Boolean{
        var trackId: Int? = null
        points.forEach { it ->
            if(trackId == null){
                trackId = it.first
            }
            else if(trackId != it.first){
                return false
            }
        }
        return true
    }

    /**
     * Get what is selected e.g :
     *
     * trackCount: 1, pointCount: 0, pointsOnSameTrack: null
     * we want only 1 track selected
     *
     * trackCount: 2, pointCount: 2, pointsOnSameTrack: false
     * we want 2 tracks or 2 points not on same track
     *
     * trackCount: 1, pointCount: 2, pointsOnSameTrack: true
     * we want 1 track or 2 points on same track
     *
     * trackCount: null, pointCount: null, pointsOnSameTrack: true
     * we want any number of tracks if we have points they must be on same track
     *
     * @param trackIds
     * @param points
     * @param trackCount
     * @param pointCount
     * @param pointsOnSameTrack
     * @return
     */
    fun getSelectionType(trackIds: MutableList<Int>, points: MutableList<Pair<Int, Double>>, trackCount: Int?, pointCount: Int?, pointsOnSameTrack: Boolean?): SelectionType{

        // If track isn't right size and we want a specific size
        if(trackIds.size != trackCount && trackCount != null){
            return SelectionType.TRACK_ERROR
        }

        // If points isn't the right size and we want a specific size
        if(points.isNotEmpty() && points.size != pointCount && pointCount != null){
            return SelectionType.POINTS_ERROR
        }

        // If points is empty
        if(points.isEmpty()){
            return SelectionType.TRACK_ONLY
        }

        // If points are from the tracks we want or if any track works
        var pointsOST = checkPointsAreFromSameTrack(points)
        if(pointsOST == pointsOnSameTrack || pointsOnSameTrack == null){
            return SelectionType.POINTS
        }

        return SelectionType.POINTS_ERROR
    }

    fun getOrderedPoints(points: MutableList<Pair<Int, Double>>): Pair<Double?, Double?>{
        if(points.size != 2) return null to null

        if(points[0].second > points[1].second){
            return points[1].second to points[0].second
        }
        return points[0].second to points[1].second
    }

}