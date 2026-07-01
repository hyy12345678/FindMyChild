package com.findmychild.app.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmychild.app.domain.model.CompassState
import com.findmychild.app.ui.theme.CompassArrow
import com.findmychild.app.ui.theme.CompassDialBg
import com.findmychild.app.ui.theme.CompassTick
import com.findmychild.app.ui.theme.CompassTickMajor
import com.findmychild.app.ui.theme.CompassNorth
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 指南针自定义 Compose 组件。
 * 用 Canvas 绘制刻度盘、方向箭头，以及距离/状态文字。
 *
 * 视觉层次：
 * 1. 深色圆形背景
 * 2. 刻度盘（随设备朝向旋转）
 * 3. 方向箭头（指向孩子方向）
 * 4. 中心圆点
 * 5. 距离文字
 */
@Composable
fun CompassView(
    state: CompassState,
    modifier: Modifier = Modifier
) {
    // 箭头旋转动画（500ms 平滑过渡）
    val animatedArrowRotation by animateFloatAsState(
        targetValue = state.arrowRotation,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    )

    // 刻度盘旋转动画（设备朝向）
    val animatedDialRotation by animateFloatAsState(
        targetValue = -state.heading,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )

    // 距离颜色
    val arrowColor = distanceColor(state.distance, state.isTargetFound)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // === Canvas 绘制层 ===
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = min(size.width, size.height) * 0.85f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val outerRadius = canvasSize / 2f

            // 1. 深色背景圆
            drawCircle(
                color = CompassDialBg,
                radius = outerRadius + 8f,
                center = Offset(centerX, centerY)
            )

            // 2. 刻度盘（旋转以保持 N 朝真北）
            rotate(degrees = animatedDialRotation, pivot = Offset(centerX, centerY)) {
                drawDial(centerX, centerY, outerRadius)
            }

            // 3. 方向箭头
            rotate(degrees = -animatedArrowRotation, pivot = Offset(centerX, centerY)) {
                drawArrow(centerX, centerY, outerRadius, arrowColor)
            }

            // 4. 中心点
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = arrowColor,
                radius = 5f,
                center = Offset(centerX, centerY)
            )
        }

        // === 文字叠加层 ===
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            if (state.isTargetFound) {
                // 距离数字
                val distText = formatDistance(state.distance)
                Text(
                    text = distText,
                    style = MaterialTheme.typography.displayLarge,
                    color = arrowColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "米",
                    style = MaterialTheme.typography.headlineMedium,
                    color = arrowColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 方向描述
                val directionText = bearingToText(state.bearing)
                Text(
                    text = "↑ $directionText",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            } else {
                // 无数据状态
                Text(
                    text = "等待位置...",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "确保小朋友开启了位置共享",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 最后更新时间
            val timeText = if (state.lastUpdateTime > 0) {
                val secondsAgo = (System.currentTimeMillis() - state.lastUpdateTime) / 1000
                when {
                    secondsAgo < 3 -> "刚刚更新"
                    secondsAgo < 60 -> "${secondsAgo}秒前"
                    else -> "${secondsAgo / 60}分钟前"
                }
            } else {
                "等待更新"
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            // GPS 精度提示
            if (state.isTargetFound && state.locationAccuracy > 20f) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "GPS 信号较弱 (精度 ±${state.locationAccuracy.toInt()}m)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ===== Private Draw Functions =====

/**
 * 绘制刻度盘。
 */
private fun DrawScope.drawDial(cx: Float, cy: Float, radius: Float) {
    val innerRadius = radius * 0.75f

    for (deg in 0 until 360 step 5) {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val cosVal = cos(rad)
        val sinVal = sin(rad)

        val isCardinal = deg % 90 == 0
        val isMajor = deg % 30 == 0

        val startR = if (isCardinal) innerRadius - 12f
            else if (isMajor) innerRadius - 6f
            else innerRadius

        val endR = innerRadius + (if (isCardinal) 20f else if (isMajor) 12f else 6f)

        val color = if (isCardinal) CompassNorth
            else if (isMajor) CompassTickMajor
            else CompassTick

        val strokeWidth = if (isCardinal) 3.5f else if (isMajor) 2.5f else 1f

        drawLine(
            color = color,
            start = Offset(cx + startR * cosVal, cy + startR * sinVal),
            end = Offset(cx + endR * cosVal, cy + endR * sinVal),
            strokeWidth = strokeWidth * 0.7f,  // scale for canvas
            cap = StrokeCap.Round
        )
    }

    // 绘制 N/S/E/W 文字标记
    val textRadius = innerRadius - 28f
    val directions = listOf(
        Triple("N", 0f, CompassNorth),
        Triple("E", 90f, CompassTickMajor),
        Triple("S", 180f, CompassTickMajor),
        Triple("W", 270f, CompassTickMajor)
    )

    for ((label, deg, color) in directions) {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val x = cx + textRadius * sin(rad)
        val y = cy - textRadius * cos(rad)

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = color.hashCode()
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = label == "N"
                isAntiAlias = true
            }
            drawText(label, x, y + 8f, paint)
        }
    }
}

/**
 * 绘制方向箭头。
 */
private fun DrawScope.drawArrow(cx: Float, cy: Float, radius: Float, color: Color) {
    val arrowLength = radius * 0.85f
    val arrowHalfWidth = radius * 0.12f

    val path = Path().apply {
        // 箭头尖（始终朝上 = 0°）
        moveTo(cx, cy - arrowLength)
        // 箭头右翼
        lineTo(cx + arrowHalfWidth, cy + arrowLength * 0.15f)
        // 箭头底部右边
        lineTo(cx + arrowHalfWidth * 0.4f, cy + arrowLength * 0.15f)
        // 箭头底部
        lineTo(cx + arrowHalfWidth * 0.4f, cy + arrowLength * 0.45f)
        // 箭头底部左边
        lineTo(cx - arrowHalfWidth * 0.4f, cy + arrowLength * 0.45f)
        // 箭头底部右边
        lineTo(cx - arrowHalfWidth * 0.4f, cy + arrowLength * 0.15f)
        // 箭头左翼
        lineTo(cx - arrowHalfWidth, cy + arrowLength * 0.15f)
        close()
    }

    // 箭头阴影
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.25f),
        alpha = 0.5f
    )

    // 箭头主体
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Fill
    )

    // 箭头边框
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = 1.5f, join = StrokeJoin.Round)
    )
}

// ===== Utility Functions =====

/**
 * 距离 → 颜色映射。
 * 绿色很近 → 黄色 → 橙色 → 红色很远。
 */
fun distanceColor(distanceMeters: Float, isFound: Boolean): Color {
    if (!isFound) return Color.Gray
    return when {
        distanceMeters < 5f   -> Color(0xFF4CAF50)  // 绿
        distanceMeters < 20f  -> Color(0xFF8BC34A)  // 浅绿
        distanceMeters < 50f  -> Color(0xFFFFC107)  // 黄
        distanceMeters < 200f -> Color(0xFFFF9800)  // 橙
        else                  -> Color(0xFFF44336)  // 红
    }
}

/**
 * 距离格式化。
 * < 1000m → "156"
 * >= 1000m → "1.2"  并在单位处显示 "公里"
 */
fun formatDistance(meters: Float): String {
    return if (meters < 1000f) {
        meters.toInt().toString()
    } else {
        String.format("%.1f", meters / 1000f)
    }
}

/**
 * 方位角 → 中文方向描述。
 */
fun bearingToText(bearing: Float): String {
    val dirs = listOf("正北", "东北", "正东", "东南", "正南", "西南", "正西", "西北")
    val index = ((bearing + 22.5f) / 45f).toInt() % 8
    return "大约朝${dirs[index]}方向"
}
