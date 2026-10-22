package com.findmychild.app.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import com.findmychild.app.domain.model.CompassState
import com.findmychild.app.ui.theme.FindMyChildTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [CompassView] UI 渲染测试。
 *
 * 验证不同 [CompassState] 下指南针组件的显示内容：
 * - 目标未找到时显示"等待位置..."
 * - 目标找到时显示距离和方向
 * - GPS 信号弱时显示精度提示
 *
 * 注意：箭头指向属于 Canvas 几何，无法用 Robolectric 做像素级断言
 * （captureToImage() 需要真实渲染表面）。方向正确性由
 * `CompassViewUtilTest` 中的 screenAngleFromNorth 用例保证，
 * 它是刻度盘与箭头共用的唯一几何真值来源。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompassViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `compassView shows waiting text when target not found`() {
        val state = CompassState(
            isTargetFound = false,
            distance = 0f,
            bearing = 0f,
            heading = 0f
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        composeTestRule.onNodeWithText("等待位置...").assertIsDisplayed()
    }

    @Test
    fun `compassView shows distance when target found`() {
        val state = CompassState(
            isTargetFound = true,
            distance = 156f,
            bearing = 45f,
            heading = 0f,
            lastUpdateTime = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        composeTestRule.onNodeWithText("156").assertIsDisplayed()
        composeTestRule.onNodeWithText("米").assertIsDisplayed()
    }

    @Test
    fun `compassView shows kilometers unit beyond 1000m`() {
        val state = CompassState(
            isTargetFound = true,
            distance = 1200f,
            bearing = 180f,
            heading = 0f,
            lastUpdateTime = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        // 回归：公里距离曾经配着"米"显示成 "1.2 米"
        composeTestRule.onNodeWithText("1.2").assertIsDisplayed()
        composeTestRule.onNodeWithText("公里").assertIsDisplayed()
    }

    @Test
    fun `compassView shows direction text when target found`() {
        val state = CompassState(
            isTargetFound = true,
            distance = 30f,
            bearing = 90f,
            heading = 0f,
            lastUpdateTime = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        composeTestRule.onAllNodesWithText("正东", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun `compassView shows gps accuracy when signal weak`() {
        val state = CompassState(
            isTargetFound = true,
            distance = 50f,
            bearing = 0f,
            heading = 0f,
            lastUpdateTime = System.currentTimeMillis(),
            locationAccuracy = 35f
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        composeTestRule.onAllNodesWithText("GPS 信号较弱", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun `compassView shows recently updated when just updated`() {
        val state = CompassState(
            isTargetFound = true,
            distance = 100f,
            bearing = 0f,
            heading = 0f,
            lastUpdateTime = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        composeTestRule.onNodeWithText("刚刚更新").assertIsDisplayed()
    }

    @Test
    fun `compassView shows ensure child sharing hint when not found`() {
        val state = CompassState(
            isTargetFound = false
        )

        composeTestRule.setContent {
            FindMyChildTheme {
                CompassView(state = state)
            }
        }

        composeTestRule.onAllNodesWithText("确保小朋友", substring = true)[0].assertIsDisplayed()
    }
}
