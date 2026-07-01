package com.findmychild.app.domain.model

data class PairingInfo(
    val sessionId: String,
    val deviceName: String,
    val pairedAt: Long,
    val peerDeviceName: String = ""
)
