package com.findmychild.app.domain.repository

import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * 中继通信抽象接口。
 * MVP 用 WebSocket 直连中继服务器实现；
 * 后续可替换为 Firebase Realtime Database / MQTT / WebRTC 等。
 */
interface SyncRepository {

    /** 连接到中继服务器 */
    suspend fun connect(sessionId: String, role: UserRole)

    /** 断开连接 */
    suspend fun disconnect()

    /** 发送我的位置到中继 */
    suspend fun sendLocation(location: LocationData)

    /** 观察对端位置的实时更新 */
    fun observePeerLocation(): Flow<LocationData>

    /** 观察连接状态变化 */
    fun observeConnectionState(): Flow<ConnectionState>

    /**
     * 观察配对状态。
     *
     * 判断"对方真的来了吗"只能看这个；连接状态只说明自己的 socket 通了，
     * 对端未到时服务端回的是 WAITING。
     */
    fun observePairingStatus(): Flow<PairingStatus>
}
