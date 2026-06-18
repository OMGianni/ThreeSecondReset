package com.threesecond.reset.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold        = Color(0xFFD4A017)
val GoldDark    = Color(0xFF9A7210)
val GoldFaint   = Color(0xFFFDF6E3)
val WhiteBg     = Color(0xFFFFFFFF)
val SurfaceWhite = Color(0xFFF7F7F7)
val SurfaceCard  = Color(0xFFFFFFFF)
val TextPrimary  = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF666666)

private val LightColors = lightColorScheme(
    primary             = Gold,
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = GoldFaint,
    onPrimaryContainer  = GoldDark,
    secondary           = GoldDark,
    onSecondary         = Color(0xFFFFFFFF),
    background          = WhiteBg,
    onBackground        = TextPrimary,
    surface             = SurfaceWhite,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceWhite,
    onSurfaceVariant    = TextSecondary,
    outline             = Color(0xFFE0E0E0),
)

@Composable
fun ThreeSecondResetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}
