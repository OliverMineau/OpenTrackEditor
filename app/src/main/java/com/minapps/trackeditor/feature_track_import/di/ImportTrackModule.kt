package com.minapps.trackeditor.feature_track_import.di

import com.minapps.trackeditor.feature_track_export.data.repository.ExportTrackRepositoryImpl
import com.minapps.trackeditor.core.domain.repository.ExportTrackRepository
import com.minapps.trackeditor.feature_track_import.data.repository.TrackImportRepositoryImpl
import com.minapps.trackeditor.core.domain.repository.TrackImportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ImportTrackModule {

    @Binds
    abstract fun bindTrackImportRepository(
        impl: TrackImportRepositoryImpl
    ): TrackImportRepository
}
