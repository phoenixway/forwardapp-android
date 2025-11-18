package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Пункт меню з іконкою та текстом
 */
data class HoldMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val id: String = label,
)

/**
 * Позиція іконки відносно тексту
 */
enum class IconPosition {
    START,  // Іконка зліва, текст справа
    END,    // Текст зліва, іконка справа
}

/**
 * Режим вирівнювання контенту в пункті меню
 */
enum class MenuAlignment {
    START,   // Контент притиснутий до лівого краю
    END,     // Контент притиснутий до правого краю
    CENTER,  // Контент по центру
}