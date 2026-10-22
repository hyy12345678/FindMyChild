package com.findmychild.app.data.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import com.findmychild.app.domain.model.LocationData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L  // interval 5s
    ).apply {
        setMinUpdateIntervalMillis(2000L)  // fastest 2s
        setWaitForAccurateLocation(true)
    }.build()

    fun observeLocation(): Flow<LocationData> = callbackFlow {
        // 未授权时 requestLocationUpdates 会抛 SecurityException，而调用方（CompassViewModel /
        // LocationService）都在 viewModelScope / lifecycleScope 里 collect，异常会直接崩掉。
        // 这里提前关流，让上层看到"没有数据"而不是崩溃；提示用户授权的 UI 由
        // ui/permission/LocationPermissionGate 负责。
        if (!context.hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        @SuppressLint("MissingPermission")
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc.toLocationData())
            }
        }

        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): LocationData? {
        return try {
            val loc = Tasks.await(fusedClient.lastLocation)
            loc?.toLocationData()
        } catch (e: Exception) {
            null
        }
    }

    /** 是否已持有定位权限。与 ui.permission 的门用同一判据，但各层自查、不跨层引用。 */
    private fun Context.hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun Location.toLocationData() = LocationData(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = time
    )
}
