package com.findmychild.app.domain.model

/**
 * 指南针的状态快照。
 * - bearing: 从我的位置到目标位置的真北方位角 (0-360°)
 * - heading: 设备当前物理朝向 (0-360°)
 * - distance: 与目标的距离 (米)
 *
 * 箭头的屏幕旋转角不在此处存储，由 UI 用 screenAngleFromNorth(bearing, heading) 推导，
 * 以保证只有一处决定旋转符号。
 */
data class CompassState(
    val bearing: Float = 0f,
    val heading: Float = 0f,
    val distance: Float = 0f,
    val lastUpdateTime: Long = 0L,
    val isTargetFound: Boolean = false,
    val locationAccuracy: Float = 0f
)
