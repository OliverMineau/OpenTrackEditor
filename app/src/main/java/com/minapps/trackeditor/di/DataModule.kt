// DependencyProvider.kt
package com.minapps.trackeditor.di

import android.content.Context
import androidx.room.Room
import com.minapps.trackeditor.core.domain.repository.EditTrackRepository
import com.minapps.trackeditor.data.local.AppDatabase
import com.minapps.trackeditor.data.local.TrackDao
import com.minapps.trackeditor.data.repository.EditTrackRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger-Hilt Module for providing data-related dependencies.
 * This module is installed in the SingletonComponent, so all
 * provided dependencies live for the entire app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * Bind impl with itf
     *
     * @param impl
     * @return
     */
    @Binds
    @Singleton
    abstract fun bindWaypointRepository(
        impl: EditTrackRepositoryImpl
    ): EditTrackRepository

    /*/**
     * Bind impl with itf
     *
     * @param impl
     * @return
     */
    @Binds
    @Singleton
    abstract fun bindTrackImportRepository(
        impl: TrackImportRepositoryImpl
    ): TrackImportRepository*/

    companion object {

        /**
         * Provide a singleton AppDatabase instance.
         * Uses Room's databaseBuilder to create the database.
         *
         * @param appContext The Application Context, injected by Hilt using @ApplicationContext qualifier
         * @return AppDatabase instance for data persistence
         */
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "track_editor_db"
            )
                .fallbackToDestructiveMigration(true)
                .build()
        }

        /**
         * Provide the DAO (Data Access Object) for tracks.
         * This depends on AppDatabase instance provided above.
         *
         * @param database The AppDatabase instance
         * @return TrackDao instance to perform DB operations
         */
        @Provides
        fun provideTrackDao(database: AppDatabase): TrackDao {
            return database.trackDao()
        }
    }
}