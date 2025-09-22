package com.minapps.trackeditor

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.local.TrackEntity
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.utils.TestDatabaseHelper
import com.minapps.trackeditor.utils.TestDatabaseHelper.addTrackAndWaypoints
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.Double
import kotlin.Int
import kotlin.Pair
import kotlin.collections.List

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
    fun joinTracksEndEnd() = runTest {

        val size_1 = 100
        val size_2 = 50

        val trackId_1 = addTrackAndWaypoints(repository, size_1)
        val trackId_2 = addTrackAndWaypoints(repository, size_2)

        val end_1 = repository.getTrackLastWaypointId(trackId_1)
        val end_2 = repository.getTrackLastWaypointId(trackId_2)

        Assert.assertNotNull(end_1)
        Assert.assertNotNull(end_2)

        if (end_1 == null) return@runTest
        if (end_2 == null) return@runTest

        var selectedPoints = listOf(trackId_1 to end_1, trackId_2 to end_2)
        joinTracksUseCase(selectedPoints)

        val wp_1 = repository.getTrackWaypoints(trackId_1)
        val wp_2 = repository.getTrackWaypoints(trackId_2)
        Log.d("tests", "wp_1 = ${wp_1.size},  wp_2 = ${wp_2.size}")

        Assert.assertTrue(wp_2.size == size_1 + size_2)
        Assert.assertTrue(wp_1.isEmpty())
    }


}

