package com.romankozak.forwardappmobile.ui.screens.goaldetail

// Цей код можна розмістити у файлі GoalDetailScreen.kt (вище функції GoalDetailScreen)
// або винести в окремий файл, наприклад, ReorderableAnimations.kt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import sh.calvin.reorderable.ReorderableLazyListState

/**
 * Кастомний модифікатор, що поєднує анімацію "підняття" елемента, що перетягується,
 * та анімацію вертикального зсуву інших елементів.
 *
 * @param state Стан від бібліотеки reorderable.
 * @param key Унікальний ключ поточного елемента.
 * @param isDragging Чи є цей елемент тим, що зараз перетягується.
 */
fun Modifier.animatedReorderableItem(
    state: ReorderableLazyListState,
    key: Any?,
    isDragging: Boolean,
): Modifier = composed {
    // Анімація для вертикального зсуву (для елементів, що "розсуваються")
    val offset = remember { Animatable(0f) }
    // Анімації для ефекту "підняття" (для елемента, що перетягується)
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val elevation = remember { Animatable(0f) }

    // Знаходимо інформацію про необхідний зсув для поточного елемента.
    // Бібліотека сама розраховує, на скільки пікселів треба зсунути кожен елемент.
    val displacement = state.displacedItems[key]

    // Запускаємо анімацію зсуву, коли змінюється `displacement`
    LaunchedEffect(displacement) {
        offset.animateTo(
            targetValue = displacement ?: 0f,
            animationSpec = tween(durationMillis = 300)
        )
    }

    // Запускаємо анімацію "підняття", коли змінюється стан `isDragging`
    LaunchedEffect(isDragging) {
        val targetScale = if (isDragging) 1.05f else 1f
        val targetRotation = if (isDragging) -3f else 0f
        val targetAlpha = if (isDragging) 0.95f else 1f
        val targetElevation = if (isDragging) 8f else 0f

        // Анімуємо одночасно всі властивості
        launch { scale.animateTo(targetScale, spring(dampingRatio = 0.6f, stiffness = 400f)) }
        launch { rotation.animateTo(targetRotation, tween(durationMillis = 300)) }
        launch { alpha.animateTo(targetAlpha, tween(durationMillis = 300)) }
        launch { elevation.animateTo(targetElevation, tween(durationMillis = 300)) }
    }

    // Повертаємо фінальний модифікатор, що застосовує всі анімовані значення
    // через graphicsLayer для максимальної продуктивності.
    this.graphicsLayer {
        translationY = offset.value
        scaleX = scale.value
        scaleY = scale.value
        rotationZ = rotation.value
        alpha = alpha.value
        shadowElevation = elevation.value
    }
}