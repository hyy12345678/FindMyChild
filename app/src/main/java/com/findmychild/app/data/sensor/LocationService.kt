package com.findmychild.app.data.sensor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.findmychild.app.MainActivity
import com.findmychild.app.data.repository.SyncRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 前台定位服务：孩子在后台持续上报 GPS 位置。
 * 配对成功后由子端自动启动。
 */
@AndroidEntryPoint
class LocationService : LifecycleService() {

    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var syncRepository: SyncRepositoryImpl

    companion object {
        const val CHANNEL_ID = "location_service"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startLocationReporting()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun startLocationReporting() {
        lifecycleScope.launch {
            locationProvider.observeLocation().collectLatest { location ->
                syncRepository.sendLocation(location)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "位置共享",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "持续共享位置"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("位置共享中")
        .setContentText("正在向家长共享位置")
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
}
