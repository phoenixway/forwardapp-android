// --- File: com/romankozak/forwardappmobile/ui/components/SwipeConfiguration.kt ---
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Описує одну конкретну дію, що виконується при свайпі.
 * @param icon Іконка, що відображається на фоні.
 * @param color Колір фону.
 * @param contentDescription Опис для accessibility.
 * @param action Лямбда, яка буде виконана.
 */
data class SwipeAction(
    val icon: ImageVector,
    val color: Color,
    val contentDescription: String,
    val action: () -> Unit,
)

/**
 * Повна конфігурація свайпів для одного елемента списку.
 * Дозволяє визначити дії для свайпу вправо (startToEnd) та вліво (endToStart).
 * Якщо якась дія є null, свайп у відповідному напрямку буде вимкнено.
 */
data class SwipeConfiguration(
    val startToEnd: SwipeAction?, // Свайп вправо
    val endToStart: SwipeAction?, // Свайп вліво
)