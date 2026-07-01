package com.findmychild.app.domain.usecase

import com.findmychild.app.domain.model.LocationData
import javax.inject.Inject
import kotlin.math.*

/**
 * 纯函数：根据两个 GPS 坐标计算方位角和距离。
 * 不依赖 Android API，可在单元测试中直接验证。
 */
class CalculateBearingUseCase @Inject constructor() {

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0
    }

    /**
     * 计算从 from 到 to 的真北方位角 (0-360°)
     */
    fun bearing(from: LocationData, to: LocationData): Float {
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)

        val y = sin(dLng) * cos(toLat)
        val x = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLng)

        val bearingRad = atan2(y, x)
        return Math.toDegrees(bearingRad).toFloat().let { (it + 360f) % 360f }
    }

    /**
     * Haversine 公式计算两点距离 (米)
     */
    fun distance(from: LocationData, to: LocationData): Float {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)

        val a = sin(dLat / 2).pow(2) +
                sin(dLng / 2).pow(2) * cos(fromLat) * cos(toLat)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_M * c).toFloat()
    }

    /**
     * 将角度归一化到 [-180, 180] 范围
     * 用于箭头旋转: normalizeAngle(bearing - heading)
     */
    fun normalizeAngle(angle: Float): Float {
        var normalized = (angle + 180f) % 360f
        if (normalized < 0) normalized += 360f
        return normalized - 180f
    }
}
