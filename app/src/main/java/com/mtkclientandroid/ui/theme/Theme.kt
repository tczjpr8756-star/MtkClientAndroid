package com.mtkclientandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtkclientandroid.engine.AppearanceSettings
import com.mtkclientandroid.engine.ColorPreset
import com.mtkclientandroid.engine.ThemeMode

private val TealLight = lightColorScheme(
    primary = Color(0xFF006A68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1EE),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6362),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E6),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607C),
    background = Color(0xFFF4FBF9),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4FBF9),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    error = Color(0xFFBA1A1A)
)

private val TealDark = darkColorScheme(
    primary = Color(0xFF80D5D2),
    onPrimary = Color(0xFF003736),
    primaryContainer = Color(0xFF00504E),
    onPrimaryContainer = Color(0xFF9CF1EE),
    secondary = Color(0xFFB0CCCA),
    onSecondary = Color(0xFF1C3534),
    secondaryContainer = Color(0xFF324B4A),
    onSecondaryContainer = Color(0xFFCCE8E6),
    tertiary = Color(0xFFB3C8E8),
    background = Color(0xFF0E1414),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0E1414),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF889391),
    error = Color(0xFFFFB4AB)
)

private val GraphiteLight = lightColorScheme(
    primary = Color(0xFF2F3033),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4D4D8),
    onPrimaryContainer = Color(0xFF1A1C1E),
    secondary = Color(0xFF5A5D62),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E3E6),
    onSurfaceVariant = Color(0xFF44474B),
    outline = Color(0xFF74777C)
)

private val GraphiteDark = darkColorScheme(
    primary = Color(0xFFC8CCD4),
    onPrimary = Color(0xFF1A1C1E),
    primaryContainer = Color(0xFF3A3D41),
    onPrimaryContainer = Color(0xFFE4E6EA),
    secondary = Color(0xFFC4C6CB),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE4E6EA),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFE4E6EA),
    surfaceVariant = Color(0xFF44474B),
    onSurfaceVariant = Color(0xFFC4C6CB),
    outline = Color(0xFF8E9196)
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF3B6A1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBF396),
    onPrimaryContainer = Color(0xFF0D2000),
    secondary = Color(0xFF56624B),
    background = Color(0xFFF7FBEA),
    onBackground = Color(0xFF191D13),
    surface = Color(0xFFF7FBEA),
    onSurface = Color(0xFF191D13),
    surfaceVariant = Color(0xFFDFE4D3),
    onSurfaceVariant = Color(0xFF43483C),
    outline = Color(0xFF74796B)
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFFA0D67C),
    onPrimary = Color(0xFF1A3700),
    primaryContainer = Color(0xFF245105),
    onPrimaryContainer = Color(0xFFBBF396),
    secondary = Color(0xFFBDCBB0),
    background = Color(0xFF11150D),
    onBackground = Color(0xFFE1E5D6),
    surface = Color(0xFF11150D),
    onSurface = Color(0xFFE1E5D6),
    surfaceVariant = Color(0xFF43483C),
    onSurfaceVariant = Color(0xFFC3C8B8),
    outline = Color(0xFF8D9283)
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MtkTheme(
    appearance: AppearanceSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val context = LocalContext.current
    var scheme: ColorScheme = when (appearance.colorPreset) {
        ColorPreset.MATERIAL_YOU -> if (Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (dark) darkColorScheme() else lightColorScheme()
        }
        ColorPreset.STOCK_ANDROID -> if (dark) darkColorScheme() else lightColorScheme()
        ColorPreset.TEAL -> if (dark) TealDark else TealLight
        ColorPreset.GRAPHITE -> if (dark) GraphiteDark else GraphiteLight
        ColorPreset.FOREST -> if (dark) ForestDark else ForestLight
    }
    if (appearance.themeMode == ThemeMode.OLED) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF1A1A1A)
        )
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = AppTypography,
        content = content
    )
}

val StatusGray = Color(0xFF9E9E9E)
val StatusAmber = Color(0xFFF9A825)
val StatusGreen = Color(0xFF2E7D32)
val StatusBlue = Color(0xFF1565C0)
val StatusRed = Color(0xFFC62828)
