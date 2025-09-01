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

    fun getSelectionType(trackIds: MutableList<Int>, points: MutableList<Pair<Int, Double>>): SelectionType{

        if(trackIds.size != 1){
            return SelectionType.TRACK_ERROR
        }

        if(points.isNotEmpty() && points.size != 2){
            return SelectionType.POINTS_ERROR
        }

        if(points.isEmpty()){
            return SelectionType.TRACK_ONLY
        }

        var pointsOnSameTrack = checkPointsAreFromSameTrack(points)
        if(pointsOnSameTrack){
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