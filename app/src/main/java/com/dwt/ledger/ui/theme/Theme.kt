package com.dwt.ledger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val IncomeGreen = Color(0xFF2E7D32)
val ExpenseRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    secondary = Color(0xFF4F6F64),
    tertiary = Color(0xFF3C6472),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D6BF),
    secondary = Color(0xFFB3CCC2),
    tertiary = Color(0xFFA2CDDD),
)

@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
