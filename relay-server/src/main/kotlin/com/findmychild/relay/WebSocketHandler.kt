package com.findmychild.relay

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun Route.webSocketHandler(sessionManager: SessionManager) {
    webSocket("/ws") {
        var sessionId: String? = null

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val raw = frame.readText()
                val obj = json.parseToJsonElement(raw).jsonObject
                val type = obj["type"]?.jsonPrimitive?.content ?: continue

                when (type) {
                    "REGISTER" -> {
                        val msg = json.decodeFromJsonElement<RegisterMessage>(obj)
                        sessionId = msg.sessionId

                        val result = sessionManager.register(msg.sessionId, msg.role, this)

                        when (result) {
                            is RegisterResult.Waiting -> {
                                // 通知注册者正在等待
                                send(Frame.Text(
                                    json.encodeToString(
                                        PairingStatusMessage.serializer(),
                                        PairingStatusMessage(status = "WAITING")
                                    )
                                ))
                            }
                            is RegisterResult.Paired -> {
                                // 通知双方配对成功
                                val myNotification = if (msg.role == "PARENT") {
                                    PairingStatusMessage(status = "PAIRED", peerDeviceName = "孩子设备")
                                } else {
                                    PairingStatusMessage(status = "PAIRED", peerDeviceName = "家长设备")
                                }

                                send(Frame.Text(
                                    json.encodeToString(PairingStatusMessage.serializer(), myNotification)
                                ))
                                result.peerSession.send(Frame.Text(
                                    json.encodeToString(PairingStatusMessage.serializer(), result.peerNotification)
                                ))
                            }
                            is RegisterResult.Error -> {
                                send(Frame.Text(
                                    json.encodeToString(ErrorMessage.serializer(), ErrorMessage(message = result.message))
                                ))
                            }
                        }
                    }

                    "LOCATION_UPDATE" -> {
                        val msg = json.decodeFromJsonElement<LocationUpdateMessage>(obj)
                        val sid = msg.sessionId

                        val peerSession = sessionManager.relayLocation(
                            sessionId = sid,
                            fromSession = this,
                            latitude = msg.latitude,
                            longitude = msg.longitude,
                            accuracy = msg.accuracy,
                            timestamp = msg.timestamp
                        )

                        peerSession?.send(Frame.Text(
                            json.encodeToString(
                                PeerLocationMessage.serializer(),
                                PeerLocationMessage(
                                    latitude = msg.latitude,
                                    longitude = msg.longitude,
                                    accuracy = msg.accuracy,
                                    timestamp = msg.timestamp
                                )
                            )
                        ))
                    }
                }
            }
        } finally {
            // 断开处理
            val id = sessionId
            if (id != null) {
                val peer = sessionManager.disconnect(id, this)
                peer?.send(Frame.Text(
                    json.encodeToString(
                        PairingStatusMessage.serializer(),
                        PairingStatusMessage(status = "DISCONNECTED")
                    )
                ))
            }
        }
    }
}
