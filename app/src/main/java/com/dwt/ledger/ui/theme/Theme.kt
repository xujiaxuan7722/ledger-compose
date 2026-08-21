package com.dwt.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 参考原项目配色：蓝色主色、浅灰背景、白色卡片、浅蓝图标底、绿/红收支
val BrandBlue = Color(0xFF4272F4)
val BrandBlueDark = Color(0xFF2E5DDB)
val BrandNavy = Color(0xFF1A2980)
val TileBlue = Color(0xFFEAEEFC)
val PageGray = Color(0xFFF5F6FA)
val IncomeGreen = Color(0xFF00A844)
val ExpenseRed = Color(0xFFD32F2F)

/** 结余卡片的渐变（原 card_gradient：#5B86E5 → #36D1DC → #1A2980，135°） */
val HeroGradient = Brush.linearGradient(listOf(Color(0xFF5B86E5), Color(0xFF36D1DC), Color(0xFF1A2980)))

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = TileBlue,
    onPrimaryContainer = BrandNavy,
    secondary = BrandBlueDark,
    secondaryContainer = TileBlue,
    onSecondaryContainer = BrandBlueDark,
    tertiary = Color(0xFF36D1DC),
    background = PageGray,
    onBackground = Color(0xFF1B1C1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1C1F),
    surfaceVariant = Color(0xFFE9ECF3),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F3FA),
    surfaceContainerHighest = Color(0xFFE9ECF3),
    outline = Color(0xFFC5CAD6),
    outlineVariant = Color(0xFFE3E6EE),
    error = ExpenseRed,
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF8C1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB8FF),
    onPrimary = Color(0xFF0B2A7A),
    primaryContainer = Color(0xFF2A3F7A),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondaryContainer = Color(0xFF2A3550),
    onSecondaryContainer = Color(0xFFDCE4FF),
    background = Color(0xFF111318),
    surface = Color(0xFF1A1D24),
    surfaceContainerLow = Color(0xFF1A1D24),
    surfaceContainer = Color(0xFF1F232B),
    surfaceContainerHigh = Color(0xFF262B35),
    surfaceVariant = Color(0xFF2A2F3A),
    onSurfaceVariant = Color(0xFFB3B9C6),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
)

val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = LedgerShapes,
        content = content,
    )
}
