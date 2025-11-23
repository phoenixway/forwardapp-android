package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Утиліта для розрахунку позиції меню з урахуванням різних крайніх випадків
 */
object HoldMenu2Geometry {
    
    data class MenuLayout(
        val menuTopLeft: IntOffset,
        val menuWidth: Float,
        val menuHeight: Float,
        val itemHeight: Float,
    )
    
    /**
     * Розраховує оптимальну позицію меню відносно кнопки-якоря
     * 
     * @param anchor Центр кнопки в window coordinates
     * @param itemCount Кількість пунктів меню
     * @param density Density для конвертації dp в px
     * @param screenWidth Ширина екрану в px
     * @param screenHeight Висота екрану в px
     * @param menuWidth Ширина меню
     * @param itemHeight Висота одного пункту
     * @param gap Відступ від кнопки до меню
     * @param edgePadding Мінімальний відступ від країв екрану
     */
    fun calculateMenuLayout(
        anchor: Offset,
        itemCount: Int,
        density: Density,
        screenWidth: Float,
        screenHeight: Float,
        menuWidth: Dp = 220.dp,
        itemHeight: Dp = 48.dp,
        gap: Dp = 16.dp,
        edgePadding: Dp = 8.dp,
    ): MenuLayout {
        with(density) {
            val menuWidthPx = menuWidth.toPx()
            val itemHeightPx = itemHeight.toPx()
            val menuHeightPx = itemHeightPx * itemCount
            val gapPx = gap.toPx()
            val edgePx = edgePadding.toPx()
            
            // 1. Спробуємо розмістити меню зверху від кнопки (preferred)
            var menuX = anchor.x - menuWidthPx / 2f
            var menuY = anchor.y - menuHeightPx - gapPx
            
            // 2. Перевірка по вертикалі
            when {
                // Меню виходить за верх екрану - розміщуємо знизу
                menuY < edgePx -> {
                    menuY = anchor.y + gapPx
                    
                    // Якщо і знизу не вміщається - притискаємо до низу
                    if (menuY + menuHeightPx > screenHeight - edgePx) {
                        menuY = (screenHeight - menuHeightPx - edgePx).coerceAtLeast(edgePx)
                    }
                }
                // Меню виходить за низ екрану - притискаємо до низу
                menuY + menuHeightPx > screenHeight - edgePx -> {
                    menuY = (screenHeight - menuHeightPx - edgePx).coerceAtLeast(edgePx)
                }
            }
            
            // 3. Перевірка по горизонталі
            when {
                // Меню виходить за ліву межу - притискаємо до лівого краю
                menuX < edgePx -> {
                    menuX = edgePx
                }
                // Меню виходить за праву межу - притискаємо до правого краю
                menuX + menuWidthPx > screenWidth - edgePx -> {
                    menuX = (screenWidth - menuWidthPx - edgePx).coerceAtLeast(edgePx)
                }
            }
            
            return MenuLayout(
                menuTopLeft = IntOffset(menuX.toInt(), menuY.toInt()),
                menuWidth = menuWidthPx,
                menuHeight = menuHeightPx,
                itemHeight = itemHeightPx,
            )
        }
    }
    
    /**
     * Розраховує індекс пункту меню, на який вказує палець
     * 
     * @param fingerPosition Позиція пальця в window coordinates
     * @param layout Розкладка меню
     * @return Індекс пункту (0..itemCount-1) або -1 якщо палець поза меню
     */
    fun calculateHoverIndex(
        fingerPosition: Offset,
        layout: MenuLayout,
        itemCount: Int,
    ): Int {
        val relativeX = fingerPosition.x - layout.menuTopLeft.x
        val relativeY = fingerPosition.y - layout.menuTopLeft.y
        
        // Перевірка чи палець в межах меню
        if (relativeX < 0 || relativeX > layout.menuWidth ||
            relativeY < 0 || relativeY > layout.menuHeight) {
            return -1
        }
        
        // Розраховуємо індекс пункту
        val index = (relativeY / layout.itemHeight).toInt()
        return index.coerceIn(0, itemCount - 1)
    }
    
    /**
     * Перевіряє чи позиція в межах кнопки (для детекції long press)
     */
    fun isInsideButton(
        position: Offset,
        buttonCenter: Offset,
        buttonRadius: Float = 100f
    ): Boolean {
        return (position - buttonCenter).getDistance() < buttonRadius
    }
}
