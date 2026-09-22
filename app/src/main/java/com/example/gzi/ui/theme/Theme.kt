package com.example.gzi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun GZITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    buttonColor: androidx.compose.ui.graphics.Color,
    chatFontSize: Float, // ПРИНИМАЕМ ЗНАЧЕНИЕ ПОЛЗУНКА ИЗ НАСТРОЕК
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current

    // Рассчитываем масштаб: берем базовый размер 16f за единицу (1.0f)
    val customFontScale = chatFontSize / 16f

    val fixedFontDensity = remember(currentDensity, customFontScale) {
        Density(
            density = currentDensity.density,
            fontScale = customFontScale // БЛОКИРУЕМ СИСТЕМУ И СТАВИМ ТОЛЬКО НАШ МАСШТАБ
        )
    }

    val colors = if (darkTheme) {
        darkColorScheme(primary = buttonColor, primaryContainer = buttonColor.copy(alpha = 0.3f))
    } else {
        lightColorScheme(primary = buttonColor, primaryContainer = buttonColor.copy(alpha = 0.15f))
    }

    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        MaterialTheme(
            colorScheme = colors,
            content = content
        )
    }
}
