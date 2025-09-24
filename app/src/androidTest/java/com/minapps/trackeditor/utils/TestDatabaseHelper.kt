package com.minapps.trackeditor.utils

import android.content.Context
import androidx.room.Room
import com.minapps.trackeditor.core.domain.model.Waypoint
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.local.TrackEntity
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import com.minapps.trackeditor.feature_track_import.data.parser.ParsedData
import org.junit.Assert
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

    suspend fun addTrackAndWaypoints(repository: EditTrackRepositoryImpl, waypoints: List<Waypoint>): Int {

        var newTrackId = repository.insertTrack(
            TrackEntity(name = generateRandomString(20), description = generateRandomString(20), createdAt = 0)
        ).toInt()

        waypoints.map { wp ->
            wp.trackId = newTrackId
        }

        repository.addWaypoints(waypoints)

        return newTrackId
    }

    fun getRandomWaypoint(id : Double): Waypoint{
        return Waypoint(id, generateRandomDouble(), generateRandomDouble(), generateRandomDouble(), generateRandomTime(), -1)
    }

    fun generateRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun generateRandomTime(): String {
        // Choose a random time within the last year
        val now = Instant.now()
        val oneYearAgo = now.minusSeconds(365L * 24 * 60 * 60)

        val randomEpoch = Random.nextLong(oneYearAgo.epochSecond, now.epochSecond)
        val randomInstant = Instant.ofEpochSecond(randomEpoch)

        return DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneOffset.UTC)
            .format(randomInstant)
    }

    fun generateRandomDouble() : Double {
        return Random.nextDouble()
    }

    fun generateRandomInt(min: Int, max: Int) : Int {
        return Random.nextInt(min, max)
    }

}
