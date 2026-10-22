package com.findmychild.app.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * 通过磁力计 + 加速度计融合计算设备朝向。
 * 使用 Android 标准传感器融合方式：getRotationMatrix + getOrientation。
 */
@Singleton
class HeadingProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    fun observeHeading(): Flow<Float> = callbackFlow {
        val accel = accelerometer
        val mag = magnetometer

        // 如果设备没有磁力计或加速度计，Flow 直接结束，不崩溃
        if (accel == null || mag == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = event.values.clone()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = event.values.clone()
                    }
                }

                val g = gravity ?: return
                val m = geomagnetic ?: return

                if (SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) {
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    // orientation[0] = azimuth in radians, range [-π, π]
                    // 转换到 [0, 360) 度
                    var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuthDeg = (azimuthDeg + 360f) % 360f

                    trySend(azimuthDeg)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener, accel,
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            listener, mag,
            SensorManager.SENSOR_DELAY_GAME
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
