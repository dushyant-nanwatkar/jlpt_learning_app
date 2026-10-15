package com.nihongo.app.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PitchBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0A0A0A)
val SurfaceContainer = Color(0xFF121212)
val SurfaceContainerHigh = Color(0xFF1E1E1E)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFE),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0D1B2A),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF1E2530),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFD7B8EE),
    onTertiary = Color(0xFF3B254B),
    background = PitchBlack,
    onBackground = Color(0xFFFFFFFF),
    surface = PitchBlack,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Color(0xFFCCCCCC),
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerLowest = PitchBlack,
    surfaceContainerLow = DarkSurface
)

@Composable
fun NihongoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = AmoledDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AndroidColor.BLACK
            window.navigationBarColor = AndroidColor.BLACK
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
