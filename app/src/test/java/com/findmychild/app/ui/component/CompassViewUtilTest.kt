package com.findmychild.app.ui.component

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [shortestAngle] 及指南针工具函数单元测试。
 *
 * 覆盖场景：
 * - shortestAngle: 正常区间、跨越 0°/360°、负角度
 * - distanceColor: 距离颜色映射边界
 * - formatDistance: 格式化逻辑
 * - bearingToText: 方向描述映射
 */
class CompassViewUtilTest {

    // ===== screenAngleFromNorth() 测试 =====
    //
    // 这是刻度盘与箭头共用的唯一几何真值来源，返回值即 Compose Canvas
    // rotate(degrees) 的入参（正值 = 顺时针 = 屏幕右方）。

    @Test
    fun `screenAngleFromNorth device faces north keeps the angle unchanged`() {
        // 设备朝正北 -> 屏幕正上方就是北，右方是东
        assertEquals(0f, screenAngleFromNorth(0f, 0f), 0.001f)     // 目标正北 -> 正上方
        assertEquals(90f, screenAngleFromNorth(90f, 0f), 0.001f)   // 目标正东 -> 正右方
        assertEquals(180f, screenAngleFromNorth(180f, 0f), 0.001f) // 目标正南 -> 正下方
        assertEquals(-90f, screenAngleFromNorth(270f, 0f), 0.001f) // 目标正西 -> 正左方
    }

    @Test
    fun `screenAngleFromNorth device faces east puts north on the left`() {
        // 设备朝正东 -> 屏幕正上方是东，正北应该在左侧
        assertEquals(-90f, screenAngleFromNorth(0f, 90f), 0.001f)   // 正北 -> 左
        assertEquals(0f, screenAngleFromNorth(90f, 90f), 0.001f)    // 正东 -> 正上方
        assertEquals(90f, screenAngleFromNorth(180f, 90f), 0.001f)  // 正南 -> 右
    }

    @Test
    fun `screenAngleFromNorth device faces west puts north on the right`() {
        // 设备朝正西 -> 正北在右侧
        assertEquals(90f, screenAngleFromNorth(0f, 270f), 0.001f)
        assertEquals(-90f, screenAngleFromNorth(180f, 270f), 0.001f)
    }

    @Test
    fun `screenAngleFromNorth takes the shortest path across 360`() {
        // 350° 的目标在设备朝北时几乎就在正前方偏左一点，而不是绕行 350°
        assertEquals(-10f, screenAngleFromNorth(350f, 0f), 0.001f)
        // 10° 的目标在设备朝西(270°)时偏右 100°
        assertEquals(100f, screenAngleFromNorth(10f, 270f), 0.001f)
    }

    @Test
    fun `screenAngleFromNorth result stays within the open interval`() {
        for (angle in 0 until 360 step 7) {
            for (heading in 0 until 360 step 11) {
                val r = screenAngleFromNorth(angle.toFloat(), heading.toFloat())
                assertTrue("($angle, $heading) -> $r", r > -180f && r <= 180f)
            }
        }
    }

    @Test
    fun `screenAngleFromNorth east and west are opposite on screen`() {
        // 东西在屏幕上恒差 180°（注意：这不是"关于 0 对称"）
        for (heading in 0 until 360 step 15) {
            val h = heading.toFloat()
            val diff = screenAngleFromNorth(270f, h) - screenAngleFromNorth(90f, h)
            assertEquals("heading=$h", 180f, abs(diff), 0.001f)
        }
    }

    @Test
    fun `screenAngleFromNorth is a lossless change of basis`() {
        // screenAngle + heading 必须还原出罗盘方位角。这条才是符号翻转的守门测试：
        // 一旦把旋转方向取反，往返就对不上了（上面的"东西差 180°"反而照样成立）。
        for (angle in 0 until 360 step 13) {
            for (heading in 0 until 360 step 17) {
                val a = angle.toFloat()
                val h = heading.toFloat()
                val restored = (screenAngleFromNorth(a, h) + h) % 360f
                val normalised = if (restored < 0f) restored + 360f else restored
                assertEquals("angle=$a heading=$h", a, normalised, 0.001f)
            }
        }
    }

    // ===== shortestAngle() 测试 =====

    @Test
    fun `shortestAngle same angle returns same value`() {
        assertEquals(90f, shortestAngle(90f, 90f), 0.001f)
    }

    @Test
    fun `shortestAngle small clockwise difference`() {
        // 10° → 30° 应返回 30°（顺时针 20°）
        assertEquals(30f, shortestAngle(10f, 30f), 0.001f)
    }

    @Test
    fun `shortestAngle small counterclockwise difference`() {
        // 30° → 10° 应返回 10°（逆时针 20°，但结果值就是 10）
        assertEquals(10f, shortestAngle(30f, 10f), 0.001f)
    }

    @Test
    fun `shortestAngle crossing 360 to 0 clockwise`() {
        // 350° → 10° 应返回 370°（顺时针 20°，而非逆时针 340°）
        // 返回值可能 > 360，表示动画方向
        val result = shortestAngle(350f, 10f)
        // 差值应为 +20°（顺时针）
        assertEquals(20f, result - 350f, 0.001f)
    }

    @Test
    fun `shortestAngle crossing 0 to 360 counterclockwise`() {
        // 10° → 350° 应返回 -10°（逆时针 20°）
        val result = shortestAngle(10f, 350f)
        // 差值应为 -20°（逆时针）
        assertEquals(-20f, result - 10f, 0.001f)
    }

    @Test
    fun `shortestAngle exactly 180 degrees clockwise`() {
        // 0° → 180°，两种路径均可，代码选择顺时针
        val result = shortestAngle(0f, 180f)
        // diff = (180 - 0 + 360) % 360 = 180，不 > 180 所以不减
        assertEquals(180f, result, 0.001f)
    }

    @Test
    fun `shortestAngle exactly 180 degrees counterclockwise`() {
        // 180° → 0°，diff = (0 - 180 + 360) % 360 = 180，不 > 180 所以不减
        val result = shortestAngle(180f, 0f)
        // 恰好 180° 时两个方向等价，实现取顺时针，因此返回 180 + 180 = 360
        assertEquals(360f, result, 0.001f)
    }

    @Test
    fun `shortestAngle large clockwise wrap`() {
        // 359° → 1° 应该顺时针 2°
        val result = shortestAngle(359f, 1f)
        assertEquals(361f, result, 0.001f) // 359 + 2 = 361
    }

    // ===== distanceColor() 测试 =====

    @Test
    fun `distanceColor not found returns gray`() {
        assertEquals(Color.Gray, distanceColor(100f, isFound = false))
    }

    @Test
    fun `distanceColor very close returns green`() {
        val expected = Color(0xFF4CAF50)
        assertEquals(expected, distanceColor(3f, isFound = true))
    }

    @Test
    fun `distanceColor boundary 5m returns light green`() {
        // 5m 不满足 < 5f，应返回浅绿
        val expected = Color(0xFF8BC34A)
        assertEquals(expected, distanceColor(5f, isFound = true))
    }

    @Test
    fun `distanceColor 15m returns light green`() {
        val expected = Color(0xFF8BC34A)
        assertEquals(expected, distanceColor(15f, isFound = true))
    }

    @Test
    fun `distanceColor boundary 20m returns yellow`() {
        val expected = Color(0xFFFFC107)
        assertEquals(expected, distanceColor(20f, isFound = true))
    }

    @Test
    fun `distanceColor 50m returns orange`() {
        val expected = Color(0xFFFF9800)
        assertEquals(expected, distanceColor(50f, isFound = true))
    }

    @Test
    fun `distanceColor 200m returns red`() {
        val expected = Color(0xFFF44336)
        assertEquals(expected, distanceColor(200f, isFound = true))
    }

    @Test
    fun `distanceColor very far returns red`() {
        val expected = Color(0xFFF44336)
        assertEquals(expected, distanceColor(10000f, isFound = true))
    }

    // ===== formatDistance() 测试 =====

    @Test
    fun `formatDistance under 1000 returns meters as integer`() {
        assertEquals("156", formatDistance(156f))
    }

    @Test
    fun `formatDistance zero returns zero`() {
        assertEquals("0", formatDistance(0f))
    }

    @Test
    fun `formatDistance exactly 1000 returns 1 point 0`() {
        assertEquals("1.0", formatDistance(1000f))
    }

    @Test
    fun `formatDistance 1200 returns 1 point 2`() {
        assertEquals("1.2", formatDistance(1200f))
    }

    @Test
    fun `formatDistance 15600 returns 15 point 6`() {
        assertEquals("15.6", formatDistance(15600f))
    }

    // ===== distanceUnit() 测试 =====

    @Test
    fun `distanceUnit under 1000 returns meters`() {
        assertEquals("米", distanceUnit(0f))
        assertEquals("米", distanceUnit(156f))
        assertEquals("米", distanceUnit(999f))
    }

    @Test
    fun `distanceUnit at and over 1000 returns kilometers`() {
        assertEquals("公里", distanceUnit(1000f))
        assertEquals("公里", distanceUnit(1200f))
        assertEquals("公里", distanceUnit(15600f))
    }

    @Test
    fun `distanceUnit agrees with formatDistance threshold`() {
        // 单位切换必须和 formatDistance 的数值切换在同一个阈值上，
        // 否则会出现 "1.2 米" 或 "156 公里" 这种错配
        for (m in listOf(0f, 5f, 156f, 999f, 999.5f, 1000f, 1000.5f, 1200f)) {
            val value = formatDistance(m)
            val unit = distanceUnit(m)
            val isKm = unit == "公里"
            assertEquals("formatDistance($m)=$value vs unit=$unit", isKm, value.contains("."))
            if (isKm) assertTrue("$m should be >= 1000m", m >= 1000f) else assertTrue("$m should be < 1000m", m < 1000f)
        }
    }

    // ===== bearingToText() 测试 =====

    @Test
    fun `bearingToText 0 degrees returns north`() {
        assertTrue(bearingToText(0f).contains("正北"))
    }

    @Test
    fun `bearingToText 90 degrees returns east`() {
        assertTrue(bearingToText(90f).contains("正东"))
    }

    @Test
    fun `bearingToText 180 degrees returns south`() {
        assertTrue(bearingToText(180f).contains("正南"))
    }

    @Test
    fun `bearingToText 270 degrees returns west`() {
        assertTrue(bearingToText(270f).contains("正西"))
    }

    @Test
    fun `bearingToText 45 degrees returns northeast`() {
        assertTrue(bearingToText(45f).contains("东北"))
    }

    @Test
    fun `bearingToText 225 degrees returns southwest`() {
        assertTrue(bearingToText(225f).contains("西南"))
    }

    @Test
    fun `bearingToText 359 degrees returns north`() {
        // 359° 接近正北
        assertTrue(bearingToText(359f).contains("正北"))
    }

    @Test
    fun `bearingToText 360 wraps to north`() {
        // 360° 应映射到正北
        val result = bearingToText(360f)
        assertTrue("Expected 正北 but got $result", result.contains("正北"))
    }
}
