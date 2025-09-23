package com.minapps.trackeditor

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.utils.TestDatabaseHelper
import com.minapps.trackeditor.utils.TestDatabaseHelper.addTrackAndWaypoints
import com.minapps.trackeditor.utils.TestDatabaseHelper.getRandomWaypoint
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JoinTracksUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EditTrackRepositoryImpl
    private lateinit var joinTracksUseCase: JoinTracksUseCase


    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = TestDatabaseHelper.createRepository(context)
        db = data.db
        repository = data.repository

        joinTracksUseCase = JoinTracksUseCase(repository)
    }

    @After
    fun tearDown() {
        TestDatabaseHelper.closeDb(db)
    }

    @Test
    fun joinTracksEndEndRandom() = runTest {
        runJoinTracksTest(
            selectPoint1 = { repository.getTrackLastWaypointId(it) },
            selectPoint2 = { repository.getTrackLastWaypointId(it) },
            expectSurvivor = 2
        )
    }

    @Test
    fun joinTracksStartEndRandom() = runTest {
        runJoinTracksTest(
            selectPoint1 = { repository.getTrackFirstWaypointId(it) },
            selectPoint2 = { repository.getTrackLastWaypointId(it) },
            expectSurvivor = 2
        )
    }

    @Test
    fun joinTracksStartStartRandom() = runTest {
        runJoinTracksTest(
            selectPoint1 = { repository.getTrackFirstWaypointId(it) },
            selectPoint2 = { repository.getTrackFirstWaypointId(it) },
            expectSurvivor = 1
        )
    }

    private suspend fun runJoinTracksTest(
        size1: Int = 1000,
        size2: Int = 500,
        selectPoint1: suspend (trackId: Int) -> Double?,
        selectPoint2: suspend (trackId: Int) -> Double?,
        expectSurvivor: Int // 1 = track1 survives, 2 = track2 survives
    ) {
        val trackId1 = addTrackAndWaypoints(repository, size1)
        val trackId2 = addTrackAndWaypoints(repository, size2)

        val point1 = selectPoint1(trackId1)
        val point2 = selectPoint2(trackId2)

        Assert.assertNotNull(point1)
        Assert.assertNotNull(point2)
        if (point1 == null || point2 == null) return

        val selectedPoints = listOf(trackId1 to point1, trackId2 to point2)
        joinTracksUseCase(selectedPoints)

        val wp1 = repository.getTrackWaypoints(trackId1)
        val wp2 = repository.getTrackWaypoints(trackId2)
        Log.d("tests", "wp1 = ${wp1.size}, wp2 = ${wp2.size}")

        when (expectSurvivor) {
            1 -> {
                Assert.assertTrue(wp1.size == size1 + size2)
                Assert.assertTrue(wp2.isEmpty())
            }

            2 -> {
                Assert.assertTrue(wp2.size == size1 + size2)
                Assert.assertTrue(wp1.isEmpty())
            }
        }
    }

    @Test
    fun joinTracksStartStartSmall() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val waypoints2 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val idsWhenJoined = listOf(-2.0, -1.0, 0.0, 1.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp2.reversed() + wp1 },
            joinPointSelector = { wp1, wp2 -> wp1[0].id to wp2[0].id }
        )
    }

    @Test
    fun joinTracksStartStartDifferentIds() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(37.3), getRandomWaypoint(45.43), getRandomWaypoint(45.44), getRandomWaypoint(51.0))
        val waypoints2 = listOf(getRandomWaypoint(-54.2), getRandomWaypoint(-50.33), getRandomWaypoint(-3.0))
        val idsWhenJoined = listOf(34.3, 35.3, 36.3, 37.3, 45.43, 45.44, 51.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp2.reversed() + wp1 },
            joinPointSelector = { wp1, wp2 -> wp1[0].id to wp2[0].id }
        )
    }

    @Test
    fun joinTracksEndEndSmall() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val waypoints2 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val idsWhenJoined = listOf(0.0, 1.0, 2.0, 3.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp1 + wp2.reversed() },
            joinPointSelector = { wp1, wp2 -> wp1.last().id to wp2.last().id }
        )
    }

    @Test
    fun joinTracksEndEndDifferentIds() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(37.3), getRandomWaypoint(45.43), getRandomWaypoint(45.44), getRandomWaypoint(51.0))
        val waypoints2 = listOf(getRandomWaypoint(-54.2), getRandomWaypoint(-50.33), getRandomWaypoint(-3.0))
        val idsWhenJoined = listOf(37.3, 45.43, 45.44, 51.0, 52.0, 53.0, 54.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp1 + wp2.reversed() },
            joinPointSelector = { wp1, wp2 -> wp1.last().id to wp2.last().id }
        )
    }


    @Test
    fun joinTracksStartEndSmall() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val waypoints2 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val idsWhenJoined = listOf(0.0, 1.0, 2.0, 3.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp2 + wp1 },
            joinPointSelector = { wp1, wp2 -> wp1.first().id to wp2.last().id }
        )
    }

    @Test
    fun joinTracksStartEndDifferentIds() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(37.3), getRandomWaypoint(45.43), getRandomWaypoint(45.44), getRandomWaypoint(51.0))
        val waypoints2 = listOf(getRandomWaypoint(-54.2), getRandomWaypoint(-50.33), getRandomWaypoint(-3.0))
        val idsWhenJoined = listOf(-54.2, -50.33, -3.0, -2.0, -1.0, 0.0, 1.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp2 + wp1 },
            joinPointSelector = { wp1, wp2 -> wp1.first().id to wp2.last().id }
        )
    }


    @Test
    fun joinTracksEndStartSmall() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val waypoints2 = listOf(getRandomWaypoint(0.0), getRandomWaypoint(1.0))
        val idsWhenJoined = listOf(0.0, 1.0, 2.0, 3.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp1 + wp2},
            joinPointSelector = { wp1, wp2 -> wp1.last().id to wp2.first().id }
        )
    }

    @Test
    fun joinTracksEndStartDifferentIds() = runTest {
        val waypoints1 = listOf(getRandomWaypoint(37.3), getRandomWaypoint(45.43), getRandomWaypoint(45.44), getRandomWaypoint(51.0))
        val waypoints2 = listOf(getRandomWaypoint(-54.2), getRandomWaypoint(-50.33), getRandomWaypoint(-3.0))
        val idsWhenJoined = listOf(37.3, 45.43, 45.44, 51.0, 52.0, 53.0, 54.0)

        runJoinTest(
            waypoints1,
            waypoints2,
            idsWhenJoined,
            fullListBuilder = { wp1, wp2 -> wp1 + wp2 },
            joinPointSelector = { wp1, wp2 -> wp1.last().id to wp2.first().id }
        )
    }



    private suspend fun runJoinTest(
        waypoints1: List<Waypoint>,
        waypoints2: List<Waypoint>,
        idsWhenJoined: List<Double>,
        fullListBuilder: (List<Waypoint>, List<Waypoint>) -> List<Waypoint>,
        joinPointSelector: (List<Waypoint>, List<Waypoint>) -> Pair<Double?, Double?>
    ) {
        val trackId1 = addTrackAndWaypoints(repository, waypoints1)
        val trackId2 = addTrackAndWaypoints(repository, waypoints2)

        val (point1, point2) = joinPointSelector(waypoints1, waypoints2)

        Assert.assertNotNull(point1)
        Assert.assertNotNull(point2)
        if (point1 == null || point2 == null) return

        val selectedPoints = listOf(trackId1 to point1, trackId2 to point2)
        joinTracksUseCase(selectedPoints)

        val wp1 = repository.getTrackWaypoints(trackId1)
        val wp2 = repository.getTrackWaypoints(trackId2)

        Log.d("tests", "wp1 = ${wp1.size}, wp2 = ${wp2.size}")

        val fullList = fullListBuilder(waypoints1, waypoints2)

        wp1.forEachIndexed { i, wp ->
            Assert.assertEquals(idsWhenJoined[i], wp.id, 0.0)
            Assert.assertEquals(fullList[i].lat, wp.lat, 0.0)
            Assert.assertEquals(fullList[i].lng, wp.lng, 0.0)
            Assert.assertEquals(fullList[i].elv, wp.elv)
            Assert.assertEquals(fullList[i].time, wp.time)
        }
    }



}

