package com.minapps.trackeditor

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterSelection
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.usecase.EvenIntervalDecimationUseCase
import com.minapps.trackeditor.utils.TestDatabaseHelper
import com.minapps.trackeditor.utils.TestDatabaseHelper.addTrackAndWaypoints
import com.minapps.trackeditor.utils.TestDatabaseHelper.generateRandomInt
import com.minapps.trackeditor.utils.TestDatabaseHelper.getRandomWaypoint
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvenIntervalDecimationUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EditTrackRepositoryImpl
    private lateinit var evenIntervalDecimationUseCase: EvenIntervalDecimationUseCase


    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = TestDatabaseHelper.createRepository(context)
        db = data.db
        repository = data.repository

        evenIntervalDecimationUseCase = EvenIntervalDecimationUseCase(repository)
    }

    @After
    fun tearDown() {
        TestDatabaseHelper.closeDb(db)
    }

    private suspend fun runEvenIntervalDecimation(
        waypoints: List<Waypoint>,
        startIndex: Int,
        endIndex: Int,
        keepCount: Int
    ): List<Waypoint> {
        val trackId = addTrackAndWaypoints(repository, waypoints)
        val selection = FilterSelection(trackId, waypoints[startIndex].id, waypoints[endIndex].id)
        val params = FilterParams(FilterType.EVEN_INTERVAL_DECIMATION(keepCount), false)

        evenIntervalDecimationUseCase(selection, params)
        return repository.getTrackWaypoints(trackId)
    }

    @Test
    fun filterFullNoChange() = runTest {
        val waypoints = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val keep = waypoints.size
        val wp = runEvenIntervalDecimation(
            waypoints = waypoints,
            startIndex = 0,
            endIndex = waypoints.lastIndex,
            keepCount = keep
        )
        Log.d("test", "expected :$keep, actual:${wp.size}")
        Assert.assertEquals(keep, wp.size)
    }

    @Test
    fun filterSegmentNoChange() = runTest {
        val waypoints = (-10..10).map { getRandomWaypoint(it.toDouble()) }
        val keep = waypoints.size
        val wp = runEvenIntervalDecimation(
            waypoints = waypoints,
            startIndex = 5,
            endIndex = 6,
            keepCount = keep
        )
        Log.d("test", "expected :$keep, actual:${wp.size}")
        Assert.assertEquals(keep, wp.size)
    }

    @Test
    fun filterFullDeleteOne() = runTest {
        val waypoints = (0..10).map { getRandomWaypoint(it.toDouble()) }
        val keep = waypoints.size - 1
        val wp = runEvenIntervalDecimation(
            waypoints = waypoints,
            startIndex = 0,
            endIndex = waypoints.lastIndex,
            keepCount = keep
        )
        Log.d("test", "expected :$keep, actual:${wp.size}")
        Assert.assertEquals(keep.toDouble(), wp.size.toDouble(), 1.0)
    }

    @Test
    fun filterFullDeleteTwo() = runTest {
        val waypoints = (0..10).map { getRandomWaypoint(it.toDouble()) }
        val keep = waypoints.size - 2
        val wp = runEvenIntervalDecimation(
            waypoints = waypoints,
            startIndex = 0,
            endIndex = waypoints.lastIndex,
            keepCount = keep
        )

        Log.d("test", "expected :$keep, actual:${wp.size}")
        Assert.assertEquals(keep.toDouble(), wp.size.toDouble(), 1.0)
    }

    @Test
    fun filterFullDeleteMore() = runTest {
        val waypoints = (0..10).map { getRandomWaypoint(it.toDouble()) }
        val keep = 2
        val wp = runEvenIntervalDecimation(
            waypoints = waypoints,
            startIndex = 0,
            endIndex = waypoints.lastIndex,
            keepCount = 2
        )

        Log.d("test", "expected :$keep, actual:${wp.size}")
        Assert.assertEquals(keep.toDouble(), wp.size.toDouble(), 1.0)
    }

    @Test
    fun filterFullDeleteRandom() = runTest {

        for (i in 0..20){
            val waypoints = (generateRandomInt(-50,50)..generateRandomInt(1000,5000)).map { getRandomWaypoint(it.toDouble()) }
            val keep = generateRandomInt(0, waypoints.size)
            val wp = runEvenIntervalDecimation(
                waypoints = waypoints,
                startIndex = 0,
                endIndex = waypoints.lastIndex,
                keepCount = keep
            )

            Log.d("test", "expected :$keep, actual:${wp.size}")
            Assert.assertEquals(keep.toDouble(), wp.size.toDouble(), 1.0)
        }

    }





}

