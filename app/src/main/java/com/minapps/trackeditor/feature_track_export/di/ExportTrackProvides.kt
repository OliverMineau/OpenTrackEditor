package com.minapps.trackeditor.feature_track_export.di

import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactory
import com.minapps.trackeditor.feature_track_export.data.factory.ExporterFactoryImpl
import com.minapps.trackeditor.feature_track_export.data.formatter.GpxExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ExportTrackProvidesModule {

    @Provides
    fun provideGpxExporter(): GpxExporter = GpxExporter()

    @Provides
    fun provideExporterFactory(
        gpxExporter: GpxExporter,
        //kmlExporter: KmlExporter
    ): ExporterFactory = ExporterFactoryImpl(gpxExporter/*,kmlExporter*/)
}
