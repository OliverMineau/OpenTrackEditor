package com.minapps.trackeditor.feature_track_export.di

import com.minapps.trackeditor.feature_track_export.data.repository.ExportTrackRepositoryImpl
import com.minapps.trackeditor.core.domain.repository.ExportTrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ExportTrackModule {

    @Binds
    abstract fun bindExportTrackRepository(
        impl: ExportTrackRepositoryImpl
    ): ExportTrackRepository
}
