package com.minapps.trackeditor.feature_track_import.di

import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactory
import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactoryImpl
import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import com.minapps.trackeditor.feature_track_import.data.factory.ImporterFactory
import com.minapps.trackeditor.feature_track_import.data.factory.ImporterFactoryImpl
import com.minapps.trackeditor.feature_track_import.data.parser.GpxParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ImportTrackProvidesModule {

    @Provides
    fun provideGpxParser(): GpxParser = GpxParser()

    @Provides
    fun provideImporterFactory(
        gpxParser: GpxParser,
        //kmlParser: KmlParser
    ): ImporterFactory = ImporterFactoryImpl(gpxParser/*,kmlExporter*/)
}
