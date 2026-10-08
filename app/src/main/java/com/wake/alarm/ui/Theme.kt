package com.wake.alarm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Black, white, grays. Blue is only for things you can interact with or that are switched on. */
object WakeColors {
    val AccentLight = Color(0xFF0B3DDB)
    val AccentDark = Color(0xFF6C8CFF)

    // Restrained green / red, used only for SAVE / DELETE and permission status.
    val GoodLight = Color(0xFF1E7B34)
    val GoodDark = Color(0xFF5CBF7A)
    val BadLight = Color(0xFFB3261E)
    val BadDark = Color(0xFFFF7B72)
}

@Composable
fun goodColor(): Color = if (isSystemInDarkTheme()) WakeColors.GoodDark else WakeColors.GoodLight

@Composable
fun badColor(): Color = if (isSystemInDarkTheme()) WakeColors.BadDark else WakeColors.BadLight

private val Mono = FontFamily.Monospace

// Shapes() takes CornerBasedShape, so RectangleShape won't type-check here.
// A rounded shape with a 0dp radius is exactly square.
private val square = RoundedCornerShape(0.dp)

private val shapes = Shapes(
    extraSmall = square, small = square, medium = square,
    large = square, extraLarge = square
)

private fun typography(): Typography {
    val b = Typography()
    // Headings and labels are monospaced (old terminal / utility feel); body text stays plain.
    return b.copy(
        displayLarge = b.displayLarge.copy(fontFamily = Mono),
        displayMedium = b.displayMedium.copy(fontFamily = Mono),
        displaySmall = b.displaySmall.copy(fontFamily = Mono),
        headlineLarge = b.headlineLarge.copy(fontFamily = Mono),
        headlineMedium = b.headlineMedium.copy(fontFamily = Mono),
        headlineSmall = b.headlineSmall.copy(fontFamily = Mono),
        titleLarge = b.titleLarge.copy(fontFamily = Mono),
        titleMedium = b.titleMedium.copy(fontFamily = Mono),
        titleSmall = b.titleSmall.copy(fontFamily = Mono),
        labelLarge = b.labelLarge.copy(fontFamily = Mono),
        labelMedium = b.labelMedium.copy(fontFamily = Mono),
        labelSmall = b.labelSmall.copy(fontFamily = Mono)
    )
}

@Composable
fun WakeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = WakeColors.AccentDark, onPrimary = Color.Black,
            primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = Color.White,
            secondary = Color.White, onSecondary = Color.Black,
            secondaryContainer = Color(0xFF2A2A2A), onSecondaryContainer = Color.White,
            tertiaryContainer = Color(0xFF2A2A2A), onTertiaryContainer = Color.White,
            background = Color.Black, onBackground = Color.White,
            surface = Color.Black, onSurface = Color.White,
            surfaceVariant = Color(0xFF1E1E1E), onSurfaceVariant = Color(0xFFBBBBBB),
            surfaceContainerHighest = Color(0xFF2A2A2A),
            outline = Color(0xFF888888), outlineVariant = Color(0xFF444444)
        )
    } else {
        lightColorScheme(
            primary = WakeColors.AccentLight, onPrimary = Color.White,
            primaryContainer = Color(0xFFE6E6E6), onPrimaryContainer = Color.Black,
            secondary = Color.Black, onSecondary = Color.White,
            secondaryContainer = Color(0xFFE6E6E6), onSecondaryContainer = Color.Black,
            tertiaryContainer = Color(0xFFE6E6E6), onTertiaryContainer = Color.Black,
            background = Color.White, onBackground = Color.Black,
            surface = Color.White, onSurface = Color.Black,
            surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF444444),
            surfaceContainerHighest = Color(0xFFE6E6E6),
            outline = Color(0xFF666666), outlineVariant = Color(0xFFCCCCCC)
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography(), shapes = shapes) {
        // Plain Text takes its colour from LocalContentColor, which defaults to BLACK unless the text sits
        // inside a Material Surface. Without this, dark mode draws black text on the black background:
        // the question, labels and messages exist but can't be seen. Set it once for the whole app.
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
