package com.threesecond.reset.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold       = Color(0xFFF5C842)
val GoldDim    = Color(0xFFB8932E)
val GoldFaint  = Color(0xFF2A2310)
val BlackBg    = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF161616)
val SurfaceCard = Color(0xFF1C1C1C)
val OnSurface  = Color(0xFFCCCCCC)

private val DarkColors = darkColorScheme(
    primary          = Gold,
    onPrimary        = Color(0xFF1A1400),
    primaryContainer = GoldFaint,
    onPrimaryContainer = Gold,
    secondary        = GoldDim,
    onSecondary      = Color(0xFF1A1400),
    background       = BlackBg,
    onBackground     = Color(0xFFF0F0F0),
    surface          = SurfaceDark,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceCard,
    onSurfaceVariant = Color(0xFF999999),
    outline          = Color(0xFF333333),
)

@Composable
fun ThreeSecondResetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content
    )
}
