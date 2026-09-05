package io.github.fkoscreen.ui.miuix

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MiuixColors {
    val PrimaryLight = Color(0xFF0D84FF)
    val PrimaryDark = Color(0xFF388AF6)

    val BackgroundLight = Color(0xFFF7F7F7)
    val BackgroundDark = Color(0xFF000000)

    val CardBackgroundLight = Color(0xFFFFFFFF)
    val CardBackgroundDark = Color(0xFF1C1C1E)

    val TextPrimaryLight = Color(0xFF000000)
    val TextPrimaryDark = Color(0xFFFFFFFF)

    val TextSecondaryLight = Color(0x99000000)
    val TextSecondaryDark = Color(0x99FFFFFF)

    val TrackUncheckedLight = Color(0xFFE5E5EA)
    val TrackUncheckedDark = Color(0xFF38383A)

    val DividerLight = Color(0x1F000000)
    val DividerDark = Color(0x1FFFFFFF)

    val Thumb = Color(0xFFFFFFFF)
}

@Composable
fun MiuixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = MiuixColors.PrimaryDark,
            background = MiuixColors.BackgroundDark,
            surface = MiuixColors.CardBackgroundDark,
            onBackground = MiuixColors.TextPrimaryDark,
            onSurface = MiuixColors.TextPrimaryDark
        )
    } else {
        lightColorScheme(
            primary = MiuixColors.PrimaryLight,
            background = MiuixColors.BackgroundLight,
            surface = MiuixColors.CardBackgroundLight,
            onBackground = MiuixColors.TextPrimaryLight,
            onSurface = MiuixColors.TextPrimaryLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
