package com.minapps.trackeditor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackEditorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Delete track database on load
        deleteDatabase("track_editor_db")
    }



}
