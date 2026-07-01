package com.findmychild.app.domain.repository

import com.findmychild.app.domain.model.LocationData
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    /** 持续获取我的 GPS 位置 */
    fun observeMyLocation(): Flow<LocationData>

    /** 持续获取设备朝向 (0-360°，真北基准) */
    fun observeDeviceHeading(): Flow<Float>

    /** 获取最后一次已知位置快照 */
    suspend fun getLastKnownLocation(): LocationData?
}
