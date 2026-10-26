package com.findmychild.app.data.repository

import com.findmychild.app.data.remote.RelayWebSocketManager
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val webSocketManager: RelayWebSocketManager
) : SyncRepository {

    private var sessionId: String = ""

    override suspend fun connect(sessionId: String, role: UserRole) {
        this.sessionId = sessionId
        webSocketManager.connect(sessionId, role)
    }

    override suspend fun disconnect() {
        webSocketManager.disconnect()
    }

    override suspend fun sendLocation(location: LocationData) {
        webSocketManager.sendLocation(location, sessionId)
    }

    override fun observePeerLocation(): Flow<LocationData> =
        webSocketManager.peerLocation

    override fun observeConnectionState(): Flow<ConnectionState> =
        webSocketManager.connectionState

    override fun observePairingStatus(): Flow<PairingStatus> =
        webSocketManager.pairingStatus
}
