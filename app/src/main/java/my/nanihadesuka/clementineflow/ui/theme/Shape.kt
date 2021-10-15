package my.nanihadesuka.clementineflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

private val small = 3.dp

val Shapes = Shapes(
    small = RoundedCornerShape(small),
    medium = RoundedCornerShape(small*3),
    large = RoundedCornerShape(small*8)
)