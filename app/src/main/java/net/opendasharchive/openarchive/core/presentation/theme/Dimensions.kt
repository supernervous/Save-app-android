package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Elevations(val card: Dp = 12.dp)

@Immutable
data class Icons(
    val small: Dp = 24.dp,
    val medium: Dp = 48.dp,
    val large: Dp = 72.dp
)

@Immutable
data class Padding(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 32.dp,
)

@Immutable
data class DimensionsTheme(
    val touchable: Dp = 48.dp,
    val padding: Padding = Padding(),
    val elevations: Elevations = Elevations(),
    val icons: Icons = Icons(),
    val bubbleArrow: Dp = 24.dp,
    val buttonCorner: Dp = 4.dp
)

private val DimensionsLight = DimensionsTheme()

private val DimensionsDark = DimensionsTheme(elevations = Elevations(card = 0.dp))

fun getThemeDimensions(isDarkTheme: Boolean) =
    if (isDarkTheme) DimensionsDark else DimensionsLight

val LocalDimensions = staticCompositionLocalOf { DimensionsLight }