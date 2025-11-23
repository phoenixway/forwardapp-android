package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.compareTo
import androidx.compose.ui.unit.dp


/**
 * Кнопка з long-press меню та drag-to-select функціональністю
 *
 * @param items Список пунктів меню з іконками
 * @param onSelect Callback при виборі пункту (передається індекс)
 * @param modifier Modifier для кнопки
 * @param controller Опціональний контролер (створюється автоматично якщо не передано)
 * @param longPressDuration Тривалість утримання для відкриття меню (мс)
 * @param onTap Callback для одинарного тапу (опціонально)
 * @param iconPosition Позиція іконки (START/END)
 * @param menuAlignment Вирівнювання контенту в меню
 * @param content Вміст кнопки
 */
@Composable
fun HoldMenu2Button(
    items: List<HoldMenuItem>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    controller: HoldMenu2Controller = rememberHoldMenu2(),
    longPressDuration: Long = 400,
    onTap: (() -> Unit)? = null,
    iconPosition: IconPosition = IconPosition.START,
    menuAlignment: MenuAlignment = MenuAlignment.START,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    var buttonAnchor by remember { mutableStateOf(Offset.Zero) }

    // Встановлюємо розміри екрану в контролер
    LaunchedEffect(screenWidth, screenHeight) {
        controller.setScreenDimensions(screenWidth, screenHeight, density)
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                buttonAnchor = Offset(
                    pos.x + size.width / 2f,
                    pos.y + size.height / 2f
                )
            }
            .pointerInput(items, onSelect, onTap) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Перевіряємо чи натиснули на кнопку
                    if (!HoldMenu2Geometry.isInsideButton(down.position, Offset(size.width / 2f, size.height / 2f))) {
                        return@awaitEachGesture
                    }

                    Log.e("HOLDMENU2", "👇 Finger on button")

                    // Чекаємо long press
                    val longPress = withTimeoutOrNull(longPressDuration) {
                        awaitPointerEvent(PointerEventPass.Main)
                        null
                    }

                    if (longPress == null) {
                        // Long press виконався - відкриваємо меню
                        Log.e("HOLDMENU2", "🔥 Opening menu")

                        // Використовуємо глобальні координати для anchor
                        val globalAnchor = buttonAnchor
                        val globalTouch = Offset(
                            buttonAnchor.x + down.position.x - size.width / 2f,
                            buttonAnchor.y + down.position.y - size.height / 2f
                        )

                        controller.open(
                            anchor = globalAnchor,
                            touch = globalTouch,
                            items = items,
                            onSelect = onSelect,
                            iconPosition = iconPosition,
                            menuAlignment = menuAlignment,
                        )

                        // Обробляємо drag
                        var currentPos = globalTouch

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break

                            // Конвертуємо локальну позицію в глобальну
                            currentPos = Offset(
                                buttonAnchor.x + change.position.x - size.width / 2f,
                                buttonAnchor.y + change.position.y - size.height / 2f
                            )

                            // Оновлюємо hover
                            controller.updateHover(currentPos)

                            // Відпустили - виконуємо action
                            if (!change.pressed) {
                                val hover = controller.state.hoverIndex
                                Log.e("HOLDMENU2", "✅ Released on: $hover")
                                if (hover >= 0) {
                                    onSelect(hover)
                                }
                                controller.close()
                                break
                            }

                            change.consume()
                        }
                    } else {
                        // Не long press - перевіряємо чи це простий тап
                        var wasDrag = false
                        val initialPos = down.position

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break

                            // Якщо палець рухався більше ніж на 10px - це не тап
                            if ((change.position - initialPos).getDistance() > 10f) {
                                wasDrag = true
                            }

                            if (!change.pressed) {
                                if (!wasDrag) {
                                    // Це був простий тап
                                    Log.e("HOLDMENU2", "👆 Single tap")
                                    onTap?.invoke()
                                }
                                break
                            }

                            change.consume()
                        }
                    }
                }
            }
    ) {
        content()
    }
}