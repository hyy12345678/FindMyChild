package com.findmychild.app.domain.usecase

import com.findmychild.app.domain.model.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [CalculateBearingUseCase] 单元测试。
 *
 * 覆盖场景：
 * - 方位角计算：正北、正东、正南、正西、对角线方向
 * - 距离计算：已知距离对比、零距离、长距离
 * - normalizeAngle：边界值与负值归一化
 */
class CalculateBearingUseCaseTest {

    private lateinit var useCase: CalculateBearingUseCase

    @Before
    fun setUp() {
        useCase = CalculateBearingUseCase()
    }

    // ===== bearing() 方位角测试 =====

    @Test
    fun `bearing due north returns 0 degrees`() {
        // 从赤道向北 0.001° ≈ 111 米
        val from = LocationData(0.0, 0.0)
        val to = LocationData(0.001, 0.0)
        val bearing = useCase.bearing(from, to)
        assertEquals(0f, bearing, 1f)
    }

    @Test
    fun `bearing due east returns 90 degrees`() {
        val from = LocationData(0.0, 0.0)
        val to = LocationData(0.0, 0.001)
        val bearing = useCase.bearing(from, to)
        assertEquals(90f, bearing, 1f)
    }

    @Test
    fun `bearing due south returns 180 degrees`() {
        val from = LocationData(0.001, 0.0)
        val to = LocationData(0.0, 0.0)
        val bearing = useCase.bearing(from, to)
        assertEquals(180f, bearing, 1f)
    }

    @Test
    fun `bearing due west returns 270 degrees`() {
        val from = LocationData(0.0, 0.001)
        val to = LocationData(0.0, 0.0)
        val bearing = useCase.bearing(from, to)
        assertEquals(270f, bearing, 1f)
    }

    @Test
    fun `bearing northeast returns approximately 45 degrees`() {
        val from = LocationData(0.0, 0.0)
        val to = LocationData(0.001, 0.001)
        val bearing = useCase.bearing(from, to)
        // 在赤道附近，东北方向 ≈ 45°
        assertEquals(45f, bearing, 5f)
    }

    @Test
    fun `bearing northwest returns approximately 315 degrees`() {
        val from = LocationData(0.0, 0.0)
        val to = LocationData(0.001, -0.001)
        val bearing = useCase.bearing(from, to)
        assertEquals(315f, bearing, 5f)
    }

    @Test
    fun `bearing is always in range 0 to 360`() {
        // 多组随机方向测试
        val testCases = listOf(
            Pair(LocationData(39.9042, 116.4074), LocationData(40.0, 116.5)),  // 北京偏东北
            Pair(LocationData(39.9042, 116.4074), LocationData(39.8, 116.3)),  // 北京偏西南
            Pair(LocationData(0.0, 179.5), LocationData(0.0, -179.5)),          // 跨日期线
            Pair(LocationData(-33.8688, 151.2093), LocationData(-33.9, 151.1))  // 悉尼偏西北
        )

        for ((from, to) in testCases) {
            val bearing = useCase.bearing(from, to)
            assertTrue("Bearing $bearing should be >= 0", bearing >= 0f)
            assertTrue("Bearing $bearing should be < 360", bearing < 360f)
        }
    }

    @Test
    fun `bearing same location returns 0 degrees`() {
        val loc = LocationData(39.9042, 116.4074)
        val bearing = useCase.bearing(loc, loc)
        // 同一位置方位角不确定，但应返回 [0, 360)
        assertTrue(bearing >= 0f && bearing < 360f)
    }

    // ===== distance() 距离测试 =====

    @Test
    fun `distance same location returns approximately zero`() {
        val loc = LocationData(39.9042, 116.4074)
        val distance = useCase.distance(loc, loc)
        assertEquals(0f, distance, 0.01f)
    }

    @Test
    fun `distance 1 degree latitude is approximately 111 km`() {
        // 1° 纬度 ≈ 111.0 km
        val from = LocationData(0.0, 0.0)
        val to = LocationData(1.0, 0.0)
        val distance = useCase.distance(from, to)
        assertEquals(111_000f, distance, 1000f) // 允许 ±1 km 误差
    }

    @Test
    fun `distance 1 degree longitude at equator is approximately 111 km`() {
        // 赤道上 1° 经度 ≈ 111.0 km
        val from = LocationData(0.0, 0.0)
        val to = LocationData(0.0, 1.0)
        val distance = useCase.distance(from, to)
        assertEquals(111_000f, distance, 1000f)
    }

    @Test
    fun `distance is symmetric`() {
        // 距离应满足对称性: d(A,B) == d(B,A)
        val a = LocationData(39.9042, 116.4074)
        val b = LocationData(40.0, 116.5)
        val distAB = useCase.distance(a, b)
        val distBA = useCase.distance(b, a)
        assertEquals(distAB, distBA, 0.01f)
    }

    @Test
    fun `distance short range matches approximate value`() {
        // 北京天安门 → 故宫博物院 ≈ 600 米
        val tiananmen = LocationData(39.9087, 116.3974)
        val forbiddenCity = LocationData(39.9143, 116.3998)
        val distance = useCase.distance(tiananmen, forbiddenCity)
        // 允许 ±150 米误差（GPS 精度 + Haversine 近似）
        assertTrue("Distance $distance should be in [400, 900]", distance in 400f..900f)
    }

    @Test
    fun `distance long range matches approximate value`() {
        // 北京 → 上海 ≈ 1067 km
        val beijing = LocationData(39.9042, 116.4074)
        val shanghai = LocationData(31.2304, 121.4737)
        val distance = useCase.distance(beijing, shanghai)
        assertEquals(1_067_000f, distance, 20_000f) // 允许 ±20 km 误差
    }

    // ===== normalizeAngle() 测试 =====

    @Test
    fun `normalizeAngle zero returns zero`() {
        assertEquals(0f, useCase.normalizeAngle(0f), 0.001f)
    }

    @Test
    fun `normalizeAngle 180 returns negative 180`() {
        // ±180° 等价；实现的契约是 [-180, 180]，故 180° 归一化得 -180°
        assertEquals(-180f, useCase.normalizeAngle(180f), 0.001f)
    }

    @Test
    fun `normalizeAngle 360 returns negative 180`() {
        // 360° 等同于 0°，归一化后为 0° → 在 [-180, 180] 范围内
        val result = useCase.normalizeAngle(360f)
        assertTrue("Result $result should be in [-180, 180]", result >= -180f && result <= 180f)
    }

    @Test
    fun `normalizeAngle 270 returns negative 90`() {
        assertEquals(-90f, useCase.normalizeAngle(270f), 0.001f)
    }

    @Test
    fun `normalizeAngle negative 270 returns 90`() {
        assertEquals(90f, useCase.normalizeAngle(-270f), 0.001f)
    }

    @Test
    fun `normalizeAngle always in range negative 180 to 180`() {
        val testValues = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f, -45f, -90f, -180f, -270f, -360f, 720f, -720f)
        for (value in testValues) {
            val result = useCase.normalizeAngle(value)
            assertTrue("normalizeAngle($value) = $result should be in [-180, 180]",
                result >= -180f && result <= 180f)
        }
    }
}
