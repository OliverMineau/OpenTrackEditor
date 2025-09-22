package com.minapps.trackeditor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.local.TrackEntity
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import com.minapps.trackeditor.utils.TestDatabaseHelper.generateRandomString
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditTrackRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TrackDao
    private lateinit var repository: EditTrackRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.trackDao()
        repository = EditTrackRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetWaypoint() = runTest {

        var newTrackId = repository.insertTrack(
            TrackEntity(name = "Test Track", description = null, createdAt = 0)
        ).toInt()

        val waypoint = Waypoint(
            id = 1.0,
            trackId = newTrackId,
            lat = 10.5,
            lng = 20.5,
            elv = null,
            time = null
        )

        repository.addWaypoint(waypoint, updateUI = false)

        val waypoints = repository.getTrackWaypoints(newTrackId)

        Assert.assertEquals(1, waypoints.size)
        Assert.assertEquals(10.5, waypoints.first().lat, 0.0)
        Assert.assertEquals(20.5, waypoints.first().lng, 0.0)
    }

    @Test
    fun insertAndGetMultipleWaypoints() = runTest {

        var newTrackId = repository.insertTrack(
            TrackEntity(name = "Test Track", description = null, createdAt = 0)
        ).toInt()

        var savedWaypoints = mutableMapOf<Double, Waypoint>()

        for (i in 0..100) {
            val waypoint = Waypoint(
                id = i.toDouble(),
                trackId = newTrackId,
                lat = 10.5,
                lng = 20.5,
                elv = null,
                time = null
            )
            savedWaypoints[i.toDouble()] = waypoint
            repository.addWaypoint(waypoint, updateUI = false)
        }

        val waypoints = repository.getTrackWaypoints(newTrackId)

        waypoints.forEach { wp ->

            Assert.assertTrue(savedWaypoints.keys.contains(wp.id))

            Assert.assertNotNull(savedWaypoints[wp.id]!!.id)

            Assert.assertEquals(savedWaypoints[wp.id]!!.id, wp.id, 0.0)
            Assert.assertEquals(savedWaypoints[wp.id]!!.lat, wp.lat, 0.0)
            Assert.assertEquals(savedWaypoints[wp.id]!!.lng, wp.lng, 0.0)
            Assert.assertEquals(savedWaypoints[wp.id]?.elv, wp.elv)
            Assert.assertEquals(savedWaypoints[wp.id]?.time, wp.time)
        }
    }

    @Test
    fun insertAndGetMultipleTracks() = runTest {

        var listOfTrackIDs = mutableMapOf<Int, String>()

        for (i in 0..100) {
            val name = generateRandomString(20)
            val track = TrackEntity(name = name, description = null, createdAt = 0)
            val trackId = repository.insertTrack(track).toInt()
            listOfTrackIDs[trackId] = name
        }

        repository.getTrackIds().forEach { id ->
            Assert.assertTrue(listOfTrackIDs.keys.contains(id))
            Assert.assertEquals(listOfTrackIDs[id], repository.getFullTrack(id)?.name)
        }
    }


}

