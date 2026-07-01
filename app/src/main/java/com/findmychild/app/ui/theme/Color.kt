package com.findmychild.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary palette — warm, reassuring tones for anxious parents
val Orange = Color(0xFFFF6D00)
val OrangeLight = Color(0xFFFF9E40)
val OrangeDark = Color(0xFFE65100)

val Blue = Color(0xFF1565C0)
val BlueLight = Color(0xFF42A5F5)
val BlueDark = Color(0xFF0D47A1)

// Compass direction colors (distance-based)
val CompassGreen = Color(0xFF4CAF50)       // < 10m — very close
val CompassTeal = Color(0xFF26A69A)        // 10-50m — close
val CompassOrange = Color(0xFFFF9800)      // 50-200m — medium
val CompassRed = Color(0xFFF44336)         // > 200m — far
val CompassGray = Color(0xFF9E9E9E)        // no data

// Light theme
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnSurface = Color(0xFF1C1B1F)

// Dark theme
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkOnSurface = Color(0xFFE6E1E5)

// Compass specific
val CompassDialBg = Color(0xFF263238)
val CompassArrow = Color(0xFFFF5722)
val CompassNorth = Color(0xFFEF5350)
val CompassTick = Color(0xFFB0BEC5)
val CompassTickMajor = Color(0xFFECEFF1)
