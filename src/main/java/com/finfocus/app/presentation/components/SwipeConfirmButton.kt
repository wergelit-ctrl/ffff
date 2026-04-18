package com.finfocus.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Кнопка подтверждения/отмены через свайп.
 *
 * - Свайп вправо (> 60% ширины) → [onConfirm]
 * - Свайп влево  (> 60% ширины) → [onCancel]
 * - Отпустить до порога → возврат в центр
 *
 * Задача 2.5: визуальная анимация, цветовые переходы.
 * Задача 2.7: semantics contentDescription.
 */
@Composable
fun SwipeConfirmButton(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    labelConfirm: String = "Подтвердить",
    labelCancel: String  = "Отменить",
    modifier: Modifier = Modifier,
) {
    val trackWidth = 320.dp
    val thumbSize  = 56.dp
    val density    = LocalDensity.current

    val trackPx = with(density) { trackWidth.toPx() }
    val thumbPx = with(density) { thumbSize.toPx() }
    val maxOffset = (trackPx - thumbPx) / 2f
    val threshold = maxOffset * 0.6f

    var offsetX by remember { mutableFloatStateOf(0f) }

    // Нормированное смещение [-1..1]
    val normalized = (offsetX / maxOffset).coerceIn(-1f, 1f)

    val bgColor by animateColorAsState(
        targetValue = when {
            normalized >  0.3f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f + normalized * 0.3f)
            normalized < -0.3f -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f + abs(normalized) * 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(80),
        label = "swipe-bg",
    )

    val thumbColor by animateColorAsState(
        targetValue = when {
            normalized >  0.5f -> MaterialTheme.colorScheme.primary
            normalized < -0.5f -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(80),
        label = "swipe-thumb",
    )

    val thumbIcon = when {
        normalized >  0.5f -> Icons.Default.Check
        normalized < -0.5f -> Icons.Default.Close
        else -> Icons.Default.ArrowForward
    }

    // Метка по центру — затухает при смещении
    val labelAlpha by animateFloatAsState(
        targetValue = 1f - abs(normalized) * 1.5f,
        label = "label-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bgColor)
            .semantics {
                contentDescription = "Потяните вправо чтобы $labelConfirm, влево чтобы $labelCancel"
            },
        contentAlignment = Alignment.Center,
    ) {
        // Центральный текст
        Text(
            text = when {
                normalized >  0.3f -> "→ $labelConfirm"
                normalized < -0.3f -> "← $labelCancel"
                else -> "← $labelCancel  |  $labelConfirm →"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(labelAlpha.coerceAtLeast(0f)),
        )

        // Ползунок
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-maxOffset, maxOffset)
                    },
                    onDragStopped = {
                        when {
                            offsetX >  threshold -> { offsetX = 0f; onConfirm() }
                            offsetX < -threshold -> { offsetX = 0f; onCancel() }
                            else -> offsetX = 0f
                        }
                    },
                )
                .semantics { contentDescription = "$labelConfirm / $labelCancel" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = thumbIcon,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(26.dp),
            )
        }
    }
}
