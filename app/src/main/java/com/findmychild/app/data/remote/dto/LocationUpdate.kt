package com.findmychild.app.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterMessage(
    val type: String = "REGISTER",
    val sessionId: String,
    val role: String           // "PARENT" | "CHILD"
)

@JsonClass(generateAdapter = true)
data class LocationUpdateDto(
    val type: String = "LOCATION_UPDATE",
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class PairingStatusDto(
    val type: String = "PAIRING_STATUS",
    val status: String,        // "WAITING" | "PAIRED" | "DISCONNECTED"
    val peerDeviceName: String = ""
)

@JsonClass(generateAdapter = true)
data class PeerLocationDto(
    val type: String = "PEER_LOCATION",
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class ErrorDto(
    val type: String = "ERROR",
    val message: String
)
