package com.findmychild.relay

import io.ktor.websocket.WebSocketSession
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SessionManager] 单元测试。
 *
 * 覆盖场景：
 * - 注册：家长先注册 → 等待，孩子后注册 → 配对成功
 * - 注册：孩子先注册 → 等待，家长后注册 → 配对成功
 * - 重复注册：同一角色重复注册 → Error
 * - 无效角色 → Error
 * - 位置转发：正确识别发送者并返回对端
 * - 断开连接：单端断开返回对端，双端断开返回 null
 */
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        sessionManager = SessionManager()
    }

    private fun mockSession(): WebSocketSession = mockk(relaxed = true)

    @Test
    fun `parent registers first returns waiting`() = runTest {
        val parentSession = mockSession()
        val result = sessionManager.register("session-1", "PARENT", parentSession)
        assertTrue("Expected Waiting but got $result", result is RegisterResult.Waiting)
    }

    @Test
    fun `child registers first returns waiting`() = runTest {
        val childSession = mockSession()
        val result = sessionManager.register("session-2", "CHILD", childSession)
        assertTrue("Expected Waiting but got $result", result is RegisterResult.Waiting)
    }

    @Test
    fun `parent registers then child registers returns paired`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()

        // 家长先注册
        val parentResult = sessionManager.register("session-3", "PARENT", parentSession)
        assertTrue(parentResult is RegisterResult.Waiting)

        // 孩子后注册 → 应配对成功
        val childResult = sessionManager.register("session-3", "CHILD", childSession)
        assertTrue("Expected Paired but got $childResult", childResult is RegisterResult.Paired)

        val paired = childResult as RegisterResult.Paired
        // 孩子注册后，peerSession 应该是家长
        assertEquals(parentSession, paired.peerSession)
        assertEquals("PAIRED", paired.peerNotification.status)
    }

    @Test
    fun `child registers then parent registers returns paired`() = runTest {
        val childSession = mockSession()
        val parentSession = mockSession()

        // 孩子先注册
        val childResult = sessionManager.register("session-4", "CHILD", childSession)
        assertTrue(childResult is RegisterResult.Waiting)

        // 家长后注册 → 应配对成功
        val parentResult = sessionManager.register("session-4", "PARENT", parentSession)
        assertTrue("Expected Paired but got $parentResult", parentResult is RegisterResult.Paired)

        val paired = parentResult as RegisterResult.Paired
        // 家长注册后，peerSession 应该是孩子
        assertEquals(childSession, paired.peerSession)
        assertEquals("PAIRED", paired.peerNotification.status)
    }

    @Test
    fun `duplicate parent registration returns error`() = runTest {
        val session1 = mockSession()
        val session2 = mockSession()

        sessionManager.register("session-5", "PARENT", session1)
        val result = sessionManager.register("session-5", "PARENT", session2)

        assertTrue("Expected Error but got $result", result is RegisterResult.Error)
        val error = result as RegisterResult.Error
        assertTrue(error.message.contains("家长"))
    }

    @Test
    fun `duplicate child registration returns error`() = runTest {
        val session1 = mockSession()
        val session2 = mockSession()

        sessionManager.register("session-6", "CHILD", session1)
        val result = sessionManager.register("session-6", "CHILD", session2)

        assertTrue("Expected Error but got $result", result is RegisterResult.Error)
        val error = result as RegisterResult.Error
        assertTrue(error.message.contains("孩子"))
    }

    @Test
    fun `invalid role returns error`() = runTest {
        val session = mockSession()
        val result = sessionManager.register("session-7", "TEACHER", session)

        assertTrue("Expected Error but got $result", result is RegisterResult.Error)
        val error = result as RegisterResult.Error
        assertTrue(error.message.contains("无效"))
    }

    @Test
    fun `relayLocation from parent returns child session`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()

        sessionManager.register("session-8", "PARENT", parentSession)
        sessionManager.register("session-8", "CHILD", childSession)

        val peer = sessionManager.relayLocation(
            sessionId = "session-8",
            fromSession = parentSession,
            latitude = 39.9042,
            longitude = 116.4074,
            accuracy = 5f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(childSession, peer)
    }

    @Test
    fun `relayLocation from child returns parent session`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()

        sessionManager.register("session-9", "PARENT", parentSession)
        sessionManager.register("session-9", "CHILD", childSession)

        val peer = sessionManager.relayLocation(
            sessionId = "session-9",
            fromSession = childSession,
            latitude = 31.2304,
            longitude = 121.4737,
            accuracy = 10f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(parentSession, peer)
    }

    @Test
    fun `relayLocation unknown session returns null`() = runTest {
        val session = mockSession()
        val peer = sessionManager.relayLocation(
            sessionId = "unknown",
            fromSession = session,
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 0f,
            timestamp = 0
        )
        assertNull(peer)
    }

    @Test
    fun `relayLocation from unknown session returns null`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()
        val unknownSession = mockSession()

        sessionManager.register("session-10", "PARENT", parentSession)
        sessionManager.register("session-10", "CHILD", childSession)

        val peer = sessionManager.relayLocation(
            sessionId = "session-10",
            fromSession = unknownSession,
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 0f,
            timestamp = 0
        )
        assertNull(peer)
    }

    @Test
    fun `disconnect parent returns child session`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()

        sessionManager.register("session-11", "PARENT", parentSession)
        sessionManager.register("session-11", "CHILD", childSession)

        val peer = sessionManager.disconnect("session-11", parentSession)
        assertEquals(childSession, peer)
    }

    @Test
    fun `disconnect child returns parent session`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()

        sessionManager.register("session-12", "PARENT", parentSession)
        sessionManager.register("session-12", "CHILD", childSession)

        val peer = sessionManager.disconnect("session-12", childSession)
        assertEquals(parentSession, peer)
    }

    @Test
    fun `disconnect both parties removes session`() = runTest {
        val parentSession = mockSession()
        val childSession = mockSession()

        sessionManager.register("session-13", "PARENT", parentSession)
        sessionManager.register("session-13", "CHILD", childSession)

        // 家长断开 → 返回孩子
        val peer1 = sessionManager.disconnect("session-13", parentSession)
        assertEquals(childSession, peer1)

        // 孩子断开 → 双方都断开，session 应被移除，返回 null
        val peer2 = sessionManager.disconnect("session-13", childSession)
        assertNull(peer2)

        // 再次 relayLocation 应返回 null（session 已移除）
        val peer3 = sessionManager.relayLocation(
            sessionId = "session-13",
            fromSession = parentSession,
            latitude = 0.0, longitude = 0.0, accuracy = 0f, timestamp = 0
        )
        assertNull(peer3)
    }

    @Test
    fun `disconnect unknown session returns null`() = runTest {
        val session = mockSession()
        val peer = sessionManager.disconnect("unknown-session", session)
        assertNull(peer)
    }

    @Test
    fun `disconnect unknown ws session returns null`() = runTest {
        val parentSession = mockSession()
        val unknownSession = mockSession()

        sessionManager.register("session-14", "PARENT", parentSession)

        val peer = sessionManager.disconnect("session-14", unknownSession)
        assertNull(peer)
    }

    @Test
    fun `multiple independent sessions do not interfere`() = runTest {
        val parent1 = mockSession()
        val child1 = mockSession()
        val parent2 = mockSession()
        val child2 = mockSession()

        // Session A
        sessionManager.register("A", "PARENT", parent1)
        sessionManager.register("A", "CHILD", child1)

        // Session B
        sessionManager.register("B", "PARENT", parent2)
        sessionManager.register("B", "CHILD", child2)

        // A 的位置转发不应影响 B
        val peerA = sessionManager.relayLocation("A", parent1, 0.0, 0.0, 0f, 0)
        assertEquals(child1, peerA)

        val peerB = sessionManager.relayLocation("B", child2, 0.0, 0.0, 0f, 0)
        assertEquals(parent2, peerB)

        // 断开 A 不影响 B
        sessionManager.disconnect("A", parent1)
        sessionManager.disconnect("A", child1)

        val peerBStill = sessionManager.relayLocation("B", parent2, 0.0, 0.0, 0f, 0)
        assertEquals(child2, peerBStill)
    }
}
