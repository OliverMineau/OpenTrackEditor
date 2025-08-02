// DependencyProvider.kt
package com.minapps.trackeditor.di

import android.content.Context
import androidx.room.Room
import com.minapps.trackeditor.core.domain.repository.EditTrackRepositoryItf
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.local.WaypointDao
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWaypointRepository(
        impl: EditTrackRepositoryImpl
    ): EditTrackRepositoryItf

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "track_editor_db"
            ).build()
        }

        @Provides
        fun provideWaypointDao(database: AppDatabase): WaypointDao {
            return database.waypointDao()
        }
    }
}