package my.nanihadesuka.clementineflow.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun Modifier.doIf(run: Boolean, call: Modifier.() -> Modifier): Modifier = if (run) call(this) else this


fun Color.blend(fraction : Float, second : Color) : Color
{
    val iFraction = 1f - fraction
    return Color(
        red = (red*fraction + second.red*iFraction) ,
        green = (green*fraction + second.green*iFraction),
        blue = (blue*fraction + second.blue*iFraction),
        alpha = (alpha*fraction + second.alpha*iFraction)
    )
}


