package com.findmychild.relay

import kotlinx.serialization.Serializable

// ===== 客户端 → 服务器 =====

@Serializable
data class RegisterMessage(
    val type: String = "REGISTER",
    val sessionId: String,
    val role: String          // "PARENT" | "CHILD"
)

@Serializable
data class LocationUpdateMessage(
    val type: String = "LOCATION_UPDATE",
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long
)

// ===== 服务器 → 客户端 =====

@Serializable
data class PairingStatusMessage(
    val type: String = "PAIRING_STATUS",
    val status: String,       // "WAITING" | "PAIRED" | "DISCONNECTED"
    val peerDeviceName: String = ""
)

@Serializable
data class PeerLocationMessage(
    val type: String = "PEER_LOCATION",
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long
)

@Serializable
data class ErrorMessage(
    val type: String = "ERROR",
    val message: String
)
