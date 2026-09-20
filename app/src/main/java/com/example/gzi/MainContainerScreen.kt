package com.example.gzi

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun MainContainerScreen(onOpenSettings: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val fullHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val minHeightPx = fullHeightPx * 0.5f

    var chatHeightPx by remember { mutableStateOf(minHeightPx) }

    val progress = ((chatHeightPx - minHeightPx) / (fullHeightPx - minHeightPx)).coerceIn(0f, 1f)
    val isExpanded = progress > 0.8f

    var isScrollingActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        MainMenuScreen(
            progress = progress,
            onOpenSettings = onOpenSettings
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { (chatHeightPx / density.density).dp })
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = !isScrollingActive,
                    state = rememberDraggableState { delta ->
                        val newHeight = (chatHeightPx - delta).coerceIn(minHeightPx, fullHeightPx)
                        chatHeightPx = newHeight
                    },
                    onDragStopped = { velocity ->
                        chatHeightPx = if (velocity < -500f || chatHeightPx > (minHeightPx + (fullHeightPx - minHeightPx) * 0.4f)) {
                            fullHeightPx
                        } else {
                            minHeightPx
                        }
                    }
                )
        ) {
            ChatScreen(
                progress = progress,
                isExpanded = isExpanded,
                // ИСПРАВЛЕНО: Клик на свернутой шторке раскрывает её, а клик на стрелке Назад в открытом чате — сворачивает обратно
                onBackToMenu = {
                    chatHeightPx = if (chatHeightPx == minHeightPx) fullHeightPx else minHeightPx
                },
                onScrollStateChanged = { isScrolling -> isScrollingActive = isScrolling }
            )
        }
    }
}
