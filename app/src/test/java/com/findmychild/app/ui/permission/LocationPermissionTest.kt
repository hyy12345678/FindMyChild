package com.findmychild.app.ui.permission

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.findmychild.app.ui.theme.FindMyChildTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 定位权限判据与权限门渲染测试。
 *
 * 权限缺失时 [com.findmychild.app.data.sensor.LocationProvider] 只能静默关闭 Flow，
 * 界面若无区分，用户看到的永远是"等待位置..."，所以这道门是定位链路可见性的一部分。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationPermissionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun grant(vararg permissions: String) {
        shadowOf(context() as Application).grantPermissions(*permissions)
    }

    // ===== hasLocationPermission() =====

    @Test
    fun `hasLocationPermission is false with no grant`() {
        assertFalse(context().hasLocationPermission())
    }

    @Test
    fun `hasLocationPermission is true after granting fine location`() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        assertTrue(context().hasLocationPermission())
    }

    @Test
    fun `hasLocationPermission is true with coarse location only`() {
        // 用户可以选择"大致位置"，此时方位角精度下降但仍应放行
        grant(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(context().hasLocationPermission())
    }

    @Test
    fun `hasLocationPermission is false for an unrelated permission`() {
        grant(Manifest.permission.CAMERA)
        assertFalse(context().hasLocationPermission())
    }

    // ===== LocationPermissionGate() =====

    @Test
    fun `gate renders content when permission is granted`() {
        composeTestRule.setContent {
            FindMyChildTheme {
                LocationPermissionGate(LocationPermissionState().apply { granted = true }, "需要定位") {
                    Text("COMPASS_CONTENT")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("COMPASS_CONTENT").assertIsDisplayed()
        composeTestRule.onNodeWithText("需要定位权限").assertDoesNotExist()
    }

    @Test
    fun `gate explains the requirement when permission is missing`() {
        composeTestRule.setContent {
            FindMyChildTheme {
                LocationPermissionGate(LocationPermissionState(), "需要定位") {
                    Text("COMPASS_CONTENT")
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("需要定位权限").assertIsDisplayed()
        composeTestRule.onNodeWithText("授予定位权限").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPASS_CONTENT").assertDoesNotExist()
    }

    @Test
    fun `gate offers system settings only after a denial`() {
        // 拒绝过一次之后系统可能不再弹窗，必须给一个去系统设置的出口
        val denied = LocationPermissionState().apply {
            granted = false
            requested = true
        }

        composeTestRule.setContent {
            FindMyChildTheme {
                LocationPermissionGate(denied, "需要定位") { Text("COMPASS_CONTENT") }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("再次请求权限").assertIsDisplayed()
        composeTestRule.onNodeWithText("前往系统设置开启").assertIsDisplayed()
    }
}
