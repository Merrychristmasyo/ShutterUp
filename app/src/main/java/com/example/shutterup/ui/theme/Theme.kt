package com.example.shutterup.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// 라이트 테마 - 인스타그램 스타일
private val LightColorScheme = lightColorScheme(
    primary = ShutterUpPrimary,
    onPrimary = ShutterUpWhite,
    primaryContainer = ShutterUpPrimaryLight,
    onPrimaryContainer = ShutterUpBlack,
    
    secondary = ShutterUpGray600,
    onSecondary = ShutterUpWhite,
    secondaryContainer = ShutterUpGray200,
    onSecondaryContainer = ShutterUpGray900,
    
    tertiary = ShutterUpGray500,
    onTertiary = ShutterUpWhite,
    tertiaryContainer = ShutterUpGray100,
    onTertiaryContainer = ShutterUpGray900,
    
    background = ShutterUpWhite,
    onBackground = ShutterUpBlack,
    surface = ShutterUpWhite,
    onSurface = ShutterUpBlack,
    surfaceVariant = ShutterUpGray50,
    onSurfaceVariant = ShutterUpGray700,
    
    error = ShutterUpError,
    onError = ShutterUpWhite,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = ShutterUpError,
    
    outline = ShutterUpGray300,
    outlineVariant = ShutterUpGray200,
    
    scrim = Color(0x52000000),
    surfaceTint = ShutterUpPrimary
)

// 다크 테마 - 핀터레스트 스타일
private val DarkColorScheme = darkColorScheme(
    primary = ShutterUpPrimaryLight,
    onPrimary = ShutterUpBlack,
    primaryContainer = ShutterUpPrimary,
    onPrimaryContainer = ShutterUpWhite,
    
    secondary = ShutterUpGray400,
    onSecondary = ShutterUpBlack,
    secondaryContainer = ShutterUpGray700,
    onSecondaryContainer = ShutterUpGray100,
    
    tertiary = ShutterUpGray300,
    onTertiary = ShutterUpBlack,
    tertiaryContainer = ShutterUpGray600,
    onTertiaryContainer = ShutterUpGray100,
    
    background = ShutterUpDarkBackground,
    onBackground = ShutterUpWhite,
    surface = ShutterUpDarkSurface,
    onSurface = ShutterUpDarkOnSurface,
    surfaceVariant = ShutterUpGray800,
    onSurfaceVariant = ShutterUpGray300,
    
    error = Color(0xFFFF6B6B),
    onError = ShutterUpBlack,
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFFCDD2),
    
    outline = ShutterUpGray600,
    outlineVariant = ShutterUpGray700,
    
    scrim = Color(0x52000000),
    surfaceTint = ShutterUpPrimaryLight
)

@Composable
fun ShutterUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 브랜드 일관성을 위해 비활성화
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}