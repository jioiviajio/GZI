package com.example.gzi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MainContainerScreen(onOpenSettings: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val fullHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val minHeightPx = fullHeightPx / 2f // Исходное состояние — чат на 50% экрана

    val chatHeightAnim = remember { Animatable(minHeightPx) }
    var isScrollingActive by remember { mutableStateOf(false) }

    // Расчет прогресса и стейта на основе анимированного значения
    val currentHeightPx = chatHeightAnim.value
    val progress = ((currentHeightPx - minHeightPx) / (fullHeightPx - minHeightPx)).coerceIn(0f, 1f)
    val isExpanded = currentHeightPx > (fullHeightPx * 0.75f)

    // ИСПРАВЛЕНО: Перехватываем жест "Назад" на телефоне, чтобы он сворачивал чат, а не закрывал приложение
    BackHandler(enabled = isExpanded) {
        coroutineScope.launch {
            chatHeightAnim.animateTo(minHeightPx)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {

        // 1. НАШЕ ГЛАВНОЕ МЕНЮ
        MainMenuScreen(
            progress = progress,
            onOpenSettings = onOpenSettings
        )

        // 2. ВЫДВИЖНАЯ ШТОРКА ЧАТА С АВТОДОВОДЧИКОМ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { (currentHeightPx / density.density).dp })
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = !isScrollingActive,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val target =
                                (chatHeightAnim.value - delta).coerceIn(minHeightPx, fullHeightPx)
                            chatHeightAnim.snapTo(target)
                        }
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val midPoint = (minHeightPx + fullHeightPx) / 2f

                            // Если толкнули вверх с силой ИЛИ просто бросили в верхней половине — раскрываем на 100%
                            if (velocity < -500f || chatHeightAnim.value > midPoint) {
                                chatHeightAnim.animateTo(fullHeightPx)
                            } else {
                                // Иначе плавно возвращаем на исходные 50%
                                chatHeightAnim.animateTo(minHeightPx)
                            }
                        }
                    }
                )
        ) {
            ChatScreen(
                progress = progress,
                isExpanded = isExpanded,
                onBackToMenu = {
                    coroutineScope.launch {
                        if (isExpanded) {
                            chatHeightAnim.animateTo(minHeightPx)
                        } else {
                            chatHeightAnim.animateTo(fullHeightPx)
                        }
                    }
                },
                onScrollStateChanged = { active -> isScrollingActive = active }
            )
        }
    }
}
