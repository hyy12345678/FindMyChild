package com.findmychild.app.di

import android.content.Context
import com.findmychild.app.data.sensor.HeadingProvider
import com.findmychild.app.data.sensor.LocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider = LocationProvider(context)

    @Provides
    @Singleton
    fun provideHeadingProvider(
        @ApplicationContext context: Context
    ): HeadingProvider = HeadingProvider(context)
}
