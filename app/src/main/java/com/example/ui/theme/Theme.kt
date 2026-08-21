package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Color(0xFF331D16),
    primaryContainer = TerracottaDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = AmberSunrise,
    onSecondary = Color(0xFF3B1003),
    secondaryContainer = Color(0xFF4A281E),
    onSecondaryContainer = TerracottaContainer,
    tertiary = ForestGreenPrimaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = Color(0xFF4A3831)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = ForestGreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = ForestGreenContainer,
    onSecondaryContainer = OnForestGreenContainer,
    tertiary = AmberSunrise,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    background = SandBackground,
    surface = CardSurfaceSand,
    surfaceVariant = SurfaceVariantSand,
    onBackground = TextDark,
    onSurface = TextCharcoal,
    onSurfaceVariant = TextMuted,
    outline = OutlineSubtle
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography) {
    // Every screen sits on a Surface that paints the theme background.
    //
    // Without this, a screen that does not draw its own background (onboarding, for
    // instance) shows the Android *window* background instead — which comes from a
    // different resource and disagreed with the Compose colour scheme, producing dark
    // text on a dark window.
    Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
      content()
    }
  }
}

