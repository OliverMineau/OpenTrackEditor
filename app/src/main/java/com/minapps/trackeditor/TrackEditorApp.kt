package com.minapps.trackeditor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackEditorApp : Application() {

    /*lateinit var database: AppDatabase
        private set

    lateinit var dependencyProvider: DependencyProvider
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "track_editor_db"
        ).build()

        dependencyProvider = DependencyProvider(this)

    }*/
}
