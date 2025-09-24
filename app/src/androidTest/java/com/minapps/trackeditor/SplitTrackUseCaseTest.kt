package com.minapps.trackeditor

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import com.minapps.trackeditor.feature_map_editor.domain.model.WaypointUpdate
import com.minapps.trackeditor.feature_map_editor.domain.usecase.JoinTracksUseCase
import com.minapps.trackeditor.feature_map_editor.tools.cut.CutTrackUseCase
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
class SplitTrackUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EditTrackRepositoryImpl
    private lateinit var cutTrackUseCase: CutTrackUseCase


    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = TestDatabaseHelper.createRepository(context)
        db = data.db
        repository = data.repository

        cutTrackUseCase = CutTrackUseCase(repository)
    }

    @After
    fun tearDown() {
        TestDatabaseHelper.closeDb(db)
    }

    private fun runSplitTrackTest(
        trackSize: Int,
        waypointSelector: suspend (Int) -> Double?,
        expectSplit: Boolean,
        validate: suspend (WaypointUpdate.SplitTrack?, Int, Double) -> Unit = { _, _, _ -> }
    ) = runTest {
        val trackId = addTrackAndWaypoints(repository, trackSize)
        val selectedWp = waypointSelector(trackId)

        Assert.assertNotNull(selectedWp)
        if (selectedWp == null) return@runTest


        val selection = listOf(trackId to selectedWp)
        val result = cutTrackUseCase(selection) as? WaypointUpdate.SplitTrack

        if (expectSplit) {
            Assert.assertNotNull(result)
            validate(result, trackId, selectedWp)
        } else {
            Assert.assertNull(result)
        }
    }

    fun splitTrackMiddle(size: Int, splitIndex: Int) = runSplitTrackTest(
        trackSize = size,
        waypointSelector = { trackId -> repository.getWaypoint(trackId, splitIndex)?.id },
        expectSplit = true
    ) { result, trackId, selectedId ->
        val ids = result?.trackIds

        Assert.assertNotNull(ids)
        if (ids == null) return@runSplitTrackTest

        Assert.assertEquals(trackId, ids.first())

        val newId = ids.last()
        val track1 = repository.getTrackWaypoints(trackId)
        val track2 = repository.getTrackWaypoints(newId)

        Assert.assertEquals(size, track1.size + track2.size)
        Assert.assertEquals(selectedId, track2.first().id, 0.0)
    }


    @Test
    fun splitTrackMiddleSmall(){
        splitTrackMiddle(3,1)
    }

    @Test
    fun splitTrackMiddleMedium(){
        splitTrackMiddle(100,90)
    }

    @Test
    fun splitTrackMiddleLarge(){
        splitTrackMiddle(1000,30)
    }

    @Test
    fun splitTrackMiddleRandom(){
        for(i in 0..50){
            val size = generateRandomInt(3,20)
            val splitInd = generateRandomInt(1,size-1)
            splitTrackMiddle(size, splitInd)
        }
    }

    @Test
    fun splitTrackStart() = runSplitTrackTest(
        trackSize = 100,
        waypointSelector = { trackId -> repository.getTrackFirstWaypointId(trackId) },
        expectSplit = false
    )

    @Test
    fun splitTrackEnd() = runSplitTrackTest(
        trackSize = 100,
        waypointSelector = { trackId -> repository.getTrackLastWaypointId(trackId) },
        expectSplit = false
    )

    @Test
    fun splitTrackTwoPoints() = runSplitTrackTest(
        trackSize = 2,
        waypointSelector = { trackId -> repository.getTrackLastWaypointId(trackId) },
        expectSplit = false
    )

    @Test
    fun splitTrackOnePoint() = runSplitTrackTest(
        trackSize = 1,
        waypointSelector = { trackId -> repository.getTrackLastWaypointId(trackId) },
        expectSplit = false
    )
}