package com.findmychild.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SettingsDataStore] 单元测试。
 *
 * 使用 Robolectric 提供的 Android Context 运行，无需真机。
 *
 * 覆盖场景：
 * - 角色读写：保存家长/孩子角色后正确读取
 * - 配对信息读写：保存后正确读取，清除后为 null
 * - 服务器地址：默认值、自定义值、清除后恢复默认
 * - clearAll：清除所有数据后角色和配对信息均为空
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: SettingsDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = SettingsDataStore(context)
    }

    @After
    fun tearDown() = runTest {
        dataStore.clearAll()
    }

    // ===== 角色读写测试 =====

    @Test
    fun `userRole initially null`() = runTest {
        val role = dataStore.userRole.first()
        assertNull(role)
    }

    @Test
    fun `setUserRole parent then read returns parent`() = runTest {
        dataStore.setUserRole(UserRole.PARENT)
        val role = dataStore.userRole.first()
        assertEquals(UserRole.PARENT, role)
    }

    @Test
    fun `setUserRole child then read returns child`() = runTest {
        dataStore.setUserRole(UserRole.CHILD)
        val role = dataStore.userRole.first()
        assertEquals(UserRole.CHILD, role)
    }

    @Test
    fun `setUserRole overwrites previous role`() = runTest {
        dataStore.setUserRole(UserRole.PARENT)
        assertEquals(UserRole.PARENT, dataStore.userRole.first())

        dataStore.setUserRole(UserRole.CHILD)
        assertEquals(UserRole.CHILD, dataStore.userRole.first())
    }

    // ===== 配对信息读写测试 =====

    @Test
    fun `pairingInfo initially null`() = runTest {
        val info = dataStore.pairingInfo.first()
        assertNull(info)
    }

    @Test
    fun `savePairingInfo then read returns same info`() = runTest {
        val info = PairingInfo(
            sessionId = "test-session-123",
            deviceName = "My Phone",
            pairedAt = 1697894400000L,
            peerDeviceName = "Child Phone"
        )
        dataStore.savePairingInfo(info)
        val read = dataStore.pairingInfo.first()
        assertNotNull(read)
        assertEquals("test-session-123", read!!.sessionId)
        assertEquals("My Phone", read.deviceName)
        assertEquals(1697894400000L, read.pairedAt)
        assertEquals("Child Phone", read.peerDeviceName)
    }

    @Test
    fun `savePairingInfo overwrites previous`() = runTest {
        val info1 = PairingInfo("session-1", "Device A", 1000L, "Peer A")
        val info2 = PairingInfo("session-2", "Device B", 2000L, "Peer B")

        dataStore.savePairingInfo(info1)
        dataStore.savePairingInfo(info2)
        val read = dataStore.pairingInfo.first()
        assertEquals("session-2", read!!.sessionId)
        assertEquals("Device B", read.deviceName)
        assertEquals(2000L, read.pairedAt)
        assertEquals("Peer B", read.peerDeviceName)
    }

    // ===== 服务器地址测试 =====

    @Test
    fun `serverUrl initially returns default`() = runTest {
        val url = dataStore.serverUrl.first()
        assertEquals(SettingsDataStore.DEFAULT_SERVER_URL, url)
    }

    @Test
    fun `setServerUrl then read returns custom url`() = runTest {
        dataStore.setServerUrl("ws://192.168.1.100:8080/ws")
        val url = dataStore.serverUrl.first()
        assertEquals("ws://192.168.1.100:8080/ws", url)
    }

    @Test
    fun `setServerUrl overwrites previous`() = runTest {
        dataStore.setServerUrl("ws://first:8080/ws")
        dataStore.setServerUrl("ws://second:8080/ws")
        val url = dataStore.serverUrl.first()
        assertEquals("ws://second:8080/ws", url)
    }

    // ===== clearAll 测试 =====

    @Test
    fun `clearAll removes role`() = runTest {
        dataStore.setUserRole(UserRole.PARENT)
        assertNotNull(dataStore.userRole.first())

        dataStore.clearAll()
        assertNull(dataStore.userRole.first())
    }

    @Test
    fun `clearAll removes pairingInfo`() = runTest {
        dataStore.savePairingInfo(PairingInfo("s1", "d1", 1000L, "p1"))
        assertNotNull(dataStore.pairingInfo.first())

        dataStore.clearAll()
        assertNull(dataStore.pairingInfo.first())
    }

    @Test
    fun `clearAll resets serverUrl to default`() = runTest {
        dataStore.setServerUrl("ws://custom:8080/ws")
        dataStore.clearAll()
        val url = dataStore.serverUrl.first()
        assertEquals(SettingsDataStore.DEFAULT_SERVER_URL, url)
    }

    @Test
    fun `clearAll removes all data simultaneously`() = runTest {
        dataStore.setUserRole(UserRole.CHILD)
        dataStore.savePairingInfo(PairingInfo("s1", "d1", 1000L, "p1"))
        dataStore.setServerUrl("ws://custom:9090/ws")

        dataStore.clearAll()

        assertNull(dataStore.userRole.first())
        assertNull(dataStore.pairingInfo.first())
        assertEquals(SettingsDataStore.DEFAULT_SERVER_URL, dataStore.serverUrl.first())
    }
}
