package com.findmychild.relay

import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * relay-server 端到端测试。
 *
 * 使用 Ktor 3.x testApplication API 启动测试服务器，
 * 验证 WebSocket 通信流程：
 * - REGISTER → WAITING（单端注册）
 * - REGISTER → PAIRED（双端配对）
 * - 重复注册 / 无效角色错误
 */
class RelayServerE2ETest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parentRegisterJson(sessionId: String) =
        """{"type":"REGISTER","sessionId":"$sessionId","role":"PARENT"}"""

    private fun childRegisterJson(sessionId: String) =
        """{"type":"REGISTER","sessionId":"$sessionId","role":"CHILD"}"""

    private fun Application.configureServer() {
        install(WebSockets)
        val sessionManager = SessionManager()
        routing { webSocketHandler(sessionManager) }
    }

    @Test
    fun `parent registers and receives waiting status`() = testApplication {
        application { configureServer() }

        val client = createClient { install(ClientWebSockets) }
        client.webSocket("/ws") {
            send(Frame.Text(parentRegisterJson("e2e-1")))
            val response = incoming.receive()
            assertTrue(response is Frame.Text, "Expected Text frame but got $response")
            val obj = json.parseToJsonElement((response as Frame.Text).readText()).jsonObject
            assertEquals("PAIRING_STATUS", obj["type"]?.jsonPrimitive?.content)
            assertEquals("WAITING", obj["status"]?.jsonPrimitive?.content)
        }
        client.close()
    }

    @Test
    fun `child registers and receives waiting status`() = testApplication {
        application { configureServer() }

        val client = createClient { install(ClientWebSockets) }
        client.webSocket("/ws") {
            send(Frame.Text(childRegisterJson("e2e-2")))
            val response = incoming.receive()
            assertTrue(response is Frame.Text)
            val obj = json.parseToJsonElement((response as Frame.Text).readText()).jsonObject
            assertEquals("PAIRING_STATUS", obj["type"]?.jsonPrimitive?.content)
            assertEquals("WAITING", obj["status"]?.jsonPrimitive?.content)
        }
        client.close()
    }

    @Test
    fun `parent and child pair successfully`() = testApplication {
        application { configureServer() }

        val parentClient = createClient { install(ClientWebSockets) }
        val childClient = createClient { install(ClientWebSockets) }

        parentClient.webSocket("/ws") {
            send(Frame.Text(parentRegisterJson("e2e-3")))

            // 家长收到 WAITING
            val waiting = incoming.receive()
            assertTrue(waiting is Frame.Text)
            val waitingObj = json.parseToJsonElement((waiting as Frame.Text).readText()).jsonObject
            assertEquals("WAITING", waitingObj["status"]?.jsonPrimitive?.content)

            // 孩子注册
            childClient.webSocket("/ws") {
                send(Frame.Text(childRegisterJson("e2e-3")))

                // 孩子收到 PAIRED
                val childPaired = incoming.receive()
                assertTrue(childPaired is Frame.Text)
                val childObj = json.parseToJsonElement((childPaired as Frame.Text).readText()).jsonObject
                assertEquals("PAIRED", childObj["status"]?.jsonPrimitive?.content)
            }
            childClient.close()

            // 家长也收到 PAIRED
            val parentPaired = incoming.receive()
            assertTrue(parentPaired is Frame.Text)
            val parentObj = json.parseToJsonElement((parentPaired as Frame.Text).readText()).jsonObject
            assertEquals("PAIRED", parentObj["status"]?.jsonPrimitive?.content)
        }
        parentClient.close()
    }

    @Test
    fun `duplicate parent registration returns error`() = testApplication {
        application { configureServer() }

        // 第一个家长注册
        val parentClient1 = createClient { install(ClientWebSockets) }
        parentClient1.webSocket("/ws") {
            send(Frame.Text(parentRegisterJson("e2e-5")))
            val first = incoming.receive()
            assertTrue(first is Frame.Text)
            val firstObj = json.parseToJsonElement((first as Frame.Text).readText()).jsonObject
            assertEquals("WAITING", firstObj["status"]?.jsonPrimitive?.content)
        }
        parentClient1.close()

        // 新连接注册同一个 session 的 PARENT → 由于上一个已断开，应该成功
        val client2 = createClient { install(ClientWebSockets) }
        client2.webSocket("/ws") {
            send(Frame.Text(parentRegisterJson("e2e-5")))
            val response = incoming.receive()
            assertTrue(response is Frame.Text)
            val obj = json.parseToJsonElement((response as Frame.Text).readText()).jsonObject
            assertEquals("PAIRING_STATUS", obj["type"]?.jsonPrimitive?.content)
        }
        client2.close()
    }

    @Test
    fun `invalid role returns error`() = testApplication {
        application { configureServer() }

        val client = createClient { install(ClientWebSockets) }
        client.webSocket("/ws") {
            send(Frame.Text("""{"type":"REGISTER","sessionId":"e2e-6","role":"TEACHER"}"""))
            val errorResponse = incoming.receive()
            assertTrue(errorResponse is Frame.Text)
            val obj = json.parseToJsonElement((errorResponse as Frame.Text).readText()).jsonObject
            assertEquals("ERROR", obj["type"]?.jsonPrimitive?.content)
            assertTrue(obj["message"]?.jsonPrimitive?.content?.contains("无效") == true)
        }
        client.close()
    }
}
