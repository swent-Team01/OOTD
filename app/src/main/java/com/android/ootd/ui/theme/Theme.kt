package com.android.ootd.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LightColorScheme =
    lightColorScheme(
        primary = Primary,
        secondary = Secondary,
        tertiary = Tertiary,
        background = Background,
        onBackground = Last,
        onSecondaryContainer = OnSecondaryContainer,
        tertiaryContainer = TertiaryContainer,
        onSurfaceVariant = OnSurfaceVariant,
        onPrimaryContainer = OnPrimaryContainer,
        onSurface = OnSurface)

@Composable
fun OOTDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
  // Remove dark color scheme so we do not have to define default values

  val colorScheme = LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
