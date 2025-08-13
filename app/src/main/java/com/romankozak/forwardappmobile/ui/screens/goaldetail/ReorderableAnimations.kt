package com.romankozak.forwardappmobile.ui.screens.goaldetail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    // Анімації для ефекту "підняття" (для елемента, що перетягується)
    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val elevationAnim = remember { Animatable(0f) }

    // ВИПРАВЛЕНО: Отримуємо зсув для елемента за його ключем.
    // Це State<Float>, який автоматично оновлюється бібліотекою.
//    val displacement = state.offsetByKey(key)
    val displacement = null//state.getDisplacement(key)


    // Запускаємо анімацію "підняття", коли змінюється стан `isDragging`
    LaunchedEffect(isDragging) {
        val targetScale = if (isDragging) 1.05f else 1f
        val targetRotation = if (isDragging) -3f else 0f
        val targetAlpha = if (isDragging) 0.95f else 1f
        val targetElevation = if (isDragging) 8f else 0f

        // ВИПРАВЛЕНО: Використовуємо coroutineScope для паралельного запуску анімацій
        coroutineScope {
            launch { scaleAnim.animateTo(targetScale, spring(dampingRatio = 0.6f, stiffness = 400f)) }
            launch { rotationAnim.animateTo(targetRotation, tween(durationMillis = 300)) }
            launch { alphaAnim.animateTo(targetAlpha, tween(durationMillis = 300)) }
            launch { elevationAnim.animateTo(targetElevation, tween(durationMillis = 300)) }
        }
    }

    // Повертаємо фінальний модифікатор, що застосовує всі анімовані значення
    this.graphicsLayer {
        // Застосовуємо зсув безпосередньо зі стану, який надає бібліотека
        //translationY = displacement.value

        // ВИПРАВЛЕНО: Використовуємо перейменовані змінні Animatable
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
        rotationZ = rotationAnim.value
        alpha = alphaAnim.value
        shadowElevation = elevationAnim.value
    }
}
