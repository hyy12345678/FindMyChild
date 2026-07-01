package com.findmychild.app.data.repository

import com.findmychild.app.data.sensor.HeadingProvider
import com.findmychild.app.data.sensor.LocationProvider
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationProvider: LocationProvider,
    private val headingProvider: HeadingProvider
) : LocationRepository {

    override fun observeMyLocation(): Flow<LocationData> =
        locationProvider.observeLocation()

    override fun observeDeviceHeading(): Flow<Float> =
        headingProvider.observeHeading()

    override suspend fun getLastKnownLocation(): LocationData? =
        locationProvider.getLastLocation()
}
