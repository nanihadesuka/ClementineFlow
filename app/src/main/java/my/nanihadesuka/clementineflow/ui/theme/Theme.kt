package my.nanihadesuka.clementineflow.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.asLiveData
import my.nanihadesuka.clementineflow.App
import my.nanihadesuka.clementineflow.THEME_FOLLOW_SYSTEM_flow
import my.nanihadesuka.clementineflow.THEME_TYPE
import my.nanihadesuka.clementineflow.THEME_TYPE_flow


val color_accent = Color(0xFF2A59B6)

val color_08 = Color(0xFF080808)
val color_20 = Color(0xFF202020)
val color_24 = Color(0xFF242424)
val color_EE = Color(0xFFEEEEEE)
val color_30 = Color(0xFF303030)
val color_AA = Color(0xFFAAAAAA)

@SuppressLint("ConflictingOnColor")
private val DarkColorPalette = darkColors(
    primary = color_20, onPrimary = color_EE,
    secondary = color_24, onSecondary = color_EE,
    surface = color_08, onSurface = color_EE,
    background = color_20, onBackground = color_EE,
    error = color_20, onError = Color.Red,
    primaryVariant = color_30,
    secondaryVariant = color_24,
)

val color_FB = Color(0xFFFBFBFB)
val color_E0 = Color(0xFFE0E0E0)
val color_11 = Color(0xFF111111)
val color_77 = Color(0xFF777777)

@SuppressLint("ConflictingOnColor")
private val LightColorPalette = lightColors(
    primary = color_FB, onPrimary = color_11,
    secondary = color_FB, onSecondary = color_11,
    surface = color_FB, onSurface = color_11,
    background = color_FB, onBackground = color_11,
    error = color_FB, onError = Color.Red,
    primaryVariant = color_E0,
    secondaryVariant = color_FB,
)

val Colors.outline get() = if (isLight) color_77 else color_AA
val Colors.appToolbar get() = if (isLight) color_EE else color_20

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
{
    val themeFollowSystem by remember { App.preferences.THEME_FOLLOW_SYSTEM_flow().asLiveData() }.observeAsState(true)
    val themeType by remember { App.preferences.THEME_TYPE_flow().asLiveData() }.observeAsState(THEME_TYPE.BLACK)

    val colors = when (themeFollowSystem)
    {
        true -> if (darkTheme) DarkColorPalette else LightColorPalette
        false -> when (themeType)
        {
            THEME_TYPE.BLACK -> DarkColorPalette
            THEME_TYPE.LIGHT -> LightColorPalette
        }
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = { Surface { content() } }
    )
}