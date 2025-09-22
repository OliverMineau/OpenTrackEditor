package com.minapps.trackeditor.utils

import android.content.Context
import androidx.room.Room
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.local.TrackEntity
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import org.junit.Assert
import kotlin.random.Random

object TestDatabaseHelper {

    data class TestRepoContainer(
        val db: AppDatabase,
        val repository: EditTrackRepositoryImpl
    )

    fun createRepository(context: Context): TestRepoContainer {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // safe for tests
            .build()

        val dao = db.trackDao()
        val repository = EditTrackRepositoryImpl(dao)

        return TestRepoContainer(db, repository)
    }

    fun closeDb(db: AppDatabase) {
        db.close()
    }



    suspend fun addTrackAndWaypoints(repository: EditTrackRepositoryImpl, waypointCount: Int): Int {

        var newTrackId = repository.insertTrack(
            TrackEntity(name = generateRandomString(20), description = generateRandomString(20), createdAt = 0)
        ).toInt()

        var savedWaypoints = mutableMapOf<Double, Waypoint>()

        for (i in 0..waypointCount-1) {
            val waypoint = Waypoint(
                id = i.toDouble(),
                trackId = newTrackId,
                lat = generateRandomDouble(),
                lng = generateRandomDouble(),
                elv = null,
                time = null
            )
            savedWaypoints[i.toDouble()] = waypoint
            repository.addWaypoint(waypoint, updateUI = false)
        }

        return newTrackId
    }

    fun generateRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun generateRandomDouble() : Double {
        return Random.nextDouble()
    }

}
