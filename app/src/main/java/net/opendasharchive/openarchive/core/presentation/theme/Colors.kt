package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val c23_nav_drawer_night = Color(0xff101010)
private val c23_darker_grey = Color(0xff212021)
private val c23_dark_grey = Color(0xff333333)
private val c23_medium_grey = Color(0xff696666)
private val c23_grey = Color(0xff9f9f9f)
private val c23_light_grey = Color(0xffe3e3e4)
private val c23_teal_100 = Color(0xff00ffeb) // h=175,3 s=100 v=100 -->
private val c23_teal_90 = Color(0xff00e7d5) // v=90.6 -->
private val c23_teal_80 = Color(0xff00cebe) // v=80.6 -->
private val c23_teal = Color(0xff00b4a6) // v=70.6 -->
private val c23_teal_60 = Color(0xff009b8f) // v=60.6 -->
private val c23_teal_50 = Color(0xff008177) // v=50.6 -->
private val c23_teal_40 = Color(0xff00685f) // v=40.6 -->
private val c23_teal_30 = Color(0xff004e48) // v=30.6 -->
private val c23_teal_20 = Color(0xff003530) // v=20.6 -->
private val c23_teal_10 = Color(0xff001b19) // v=10.6 -->
private val c23_powder_blue = Color(0xffaae6e1)

@Immutable
data class ColorTheme(
    val material: ColorScheme,
    val primaryDark: Color = c23_teal_40,
    val primaryBright: Color = c23_powder_blue,

    val colorBottomNavbar: Color = material.primary,

    val colorOnBottomNavbar: Color = material.onBackground,

    val colorAddButton: Color = material.background,
    val colorOnAddButton: Color = material.onBackground,
    val colorNavigationDrawerBackground: Color = material.background,
    val colorOnboarding23GetStarted: Color = material.onBackground,
    val colorSpaceSetupProgressOn: Color = Color.Black,
    val colorSpaceSetupProgressOff: Color = c23_grey,
    val colorBackgroundSpaceIcon: Color = c23_light_grey,


    val colorPill: Color = Color(0xFFE3E3E4),
    val colorMediaOverlayIcon: Color = Color.White,
    val colorDanger: Color = material.error,
    val colorDivider: Color = Color.LightGray,
    val colorImageBackground: Color = Color.Black,
    val colorFloatIconBackground: Color = Color.Transparent,
    val colorSectionHeaderText: Color = Color.Gray,
    val colorMediaTitleText: Color = Color.LightGray,
    val colorWaveformIndicator: Color = Color(0xffaa0000),
    val colorWaveform: Color = Color(0xFF999999)

)

private val LightColorScheme = ColorTheme(
    material = lightColorScheme(

        primary = c23_teal,
        onPrimary = Color.Black,
        primaryContainer = c23_teal_90,
        onPrimaryContainer = Color.Black,

        secondary = c23_teal,
        onSecondary = Color.Black,
        secondaryContainer = c23_teal_90,
        onSecondaryContainer = Color.Black,

        tertiary = c23_powder_blue,
        onTertiary = Color.Black,
        tertiaryContainer = c23_powder_blue,
        onTertiaryContainer = Color.Black,

        error = Color.Red,
        onError = Color.White,
        errorContainer = Color.Red,
        onErrorContainer = Color.White,

        background = Color.Black,
        onBackground = Color.White,

        surface = c23_light_grey,
        onSurface = Color.Black,
        surfaceVariant = c23_grey,
        onSurfaceVariant = Color.Black,

        outline = Color.Black,
        inverseOnSurface = Color.White,
        inverseSurface = Color.Black,
        inversePrimary = Color.Black,
        surfaceTint = c23_teal
    ),
)

private val DarkColorScheme = ColorTheme(
    material = darkColorScheme(
        primary = c23_teal,
        onPrimary = Color.White,
        primaryContainer = c23_teal_20,
        onPrimaryContainer = Color.White,

        secondary = c23_teal,
        onSecondary = Color.White,
        secondaryContainer = c23_teal_20,
        onSecondaryContainer = Color.White,

        tertiary = c23_powder_blue,
        onTertiary = Color.Black,
        tertiaryContainer = c23_powder_blue,
        onTertiaryContainer = Color.Black,

        error = Color.Red,
        onError = Color.White,
        errorContainer = Color.Red,
        onErrorContainer = Color.White,

        background = Color.Black,
        onBackground = Color.White,

        surface = c23_darker_grey,
        onSurface = Color.White,
        surfaceVariant = c23_dark_grey,
        onSurfaceVariant = Color.White,

        outline = Color.White,
        inverseOnSurface = Color.Black,
        inverseSurface = Color.White,
        inversePrimary = Color.White,
        surfaceTint = c23_teal
    ),
)

fun getThemeColors(isDarkTheme: Boolean) = if (isDarkTheme) DarkColorScheme else LightColorScheme

val LocalColors = staticCompositionLocalOf { LightColorScheme }

@Composable
fun textFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = ThemeColors.material.primary,
    unfocusedIndicatorColor = ThemeColors.material.primary,
    focusedLabelColor = ThemeColors.material.primary,
    unfocusedLabelColor = ThemeColors.material.primary,
    cursorColor = ThemeColors.material.primary,
    focusedContainerColor = ThemeColors.material.surface,
    unfocusedContainerColor = ThemeColors.material.surface,
    disabledContainerColor = ThemeColors.material.surface,
    unfocusedTextColor = ThemeColors.material.onSurface,
    focusedTextColor = ThemeColors.material.onSurface,
    disabledTextColor = ThemeColors.material.onSurface
)