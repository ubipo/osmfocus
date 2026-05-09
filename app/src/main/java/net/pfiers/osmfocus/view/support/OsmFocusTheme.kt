package net.pfiers.osmfocus.view.support

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mirrors the app's XML color definitions (res/values/colors.xml + styles.xml)
private val Primary       = Color(0xFFFFC107) // #FFC107 amber/yellow
private val PrimaryDark   = Color(0xFFF57F17) // #F57F17
private val PrimaryLight  = Color(0xFFFFE082) // #FFE082
private val Secondary     = Color(0xFF29335C) // #29335C
private val OnSecondary   = Color(0xFFFFFFFF)
private val Accent        = Color(0xFF028090) // #028090 teal
private val ErrorColor    = Color(0xFFC62828) // #c62828

private val OsmFocusColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = Color.Black,
    primaryContainer   = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary          = Secondary,
    onSecondary        = OnSecondary,
    tertiary           = Accent,
    onTertiary         = Color.White,
    error              = ErrorColor,
    onError            = Color.White,
)

/**
 * App-wide Compose theme that mirrors the XML AppTheme colours.
 * Use this instead of bare `MaterialTheme { }` in every Compose entry-point.
 */
@Composable
fun OsmFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OsmFocusColorScheme,
        content = content,
    )
}

