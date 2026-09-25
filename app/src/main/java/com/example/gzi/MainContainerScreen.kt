package com.example.gzi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
fun MainContainerScreen(
    onOpenSettings: () -> Unit,
    onOpenLaboratory: () -> Unit // Прокидываем событие из навигации
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val fullHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val minHeightPx = fullHeightPx / 2f

    val chatHeightAnim = remember { Animatable(minHeightPx) }
    var isScrollingActive by remember { mutableStateOf(false) }

    val chatSpringSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    val currentHeightPx = chatHeightAnim.value
    val progress = ((currentHeightPx - minHeightPx) / (fullHeightPx - minHeightPx)).coerceIn(0f, 1f)
    val isExpanded = currentHeightPx > (fullHeightPx * 0.75f)

    BackHandler(enabled = isExpanded) {
        coroutineScope.launch {
            chatHeightAnim.animateTo(
                targetValue = minHeightPx,
                animationSpec = chatSpringSpec
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {

        // Главное меню теперь знает, куда перенаправлять при клике на Лабораторию
        MainMenuScreen(
            progress = progress,
            onOpenSettings = onOpenSettings,
            onOpenLaboratory = onOpenLaboratory
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { (currentHeightPx / density.density).dp })
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = !isScrollingActive,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val target = (chatHeightAnim.value - delta).coerceIn(minHeightPx, fullHeightPx)
                            chatHeightAnim.snapTo(target)
                        }
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val midPoint = (minHeightPx + fullHeightPx) / 2f
                            val upwardVelocity = -velocity
                            val velocityThreshold = 1000f

                            if (upwardVelocity > velocityThreshold) {
                                chatHeightAnim.animateTo(
                                    targetValue = fullHeightPx,
                                    animationSpec = chatSpringSpec,
                                    initialVelocity = upwardVelocity
                                )
                            } else if (upwardVelocity < -velocityThreshold) {
                                chatHeightAnim.animateTo(
                                    targetValue = minHeightPx,
                                    animationSpec = chatSpringSpec,
                                    initialVelocity = upwardVelocity
                                )
                            } else {
                                if (chatHeightAnim.value > midPoint) {
                                    chatHeightAnim.animateTo(fullHeightPx, chatSpringSpec)
                                } else {
                                    chatHeightAnim.animateTo(minHeightPx, chatSpringSpec)
                                }
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
                        val target = if (isExpanded) minHeightPx else fullHeightPx
                        chatHeightAnim.animateTo(target, chatSpringSpec)
                    }
                },
                onScrollStateChanged = { active -> isScrollingActive = active }
            )
        }
    }
}
