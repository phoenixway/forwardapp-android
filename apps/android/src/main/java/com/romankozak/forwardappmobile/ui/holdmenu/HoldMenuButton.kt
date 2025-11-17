/*package com.romankozak.forwardappmobile.ui.holdmenu

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HoldMenuBuilder {
    internal val items = mutableListOf<HoldMenuItem>()

    fun item(label: String, icon: ImageVector, onSelect: () -> Unit) {
        items += HoldMenuItem(label, icon, onSelect)
    }
}
@Composable
fun HoldMenuButton(
    icon: ImageVector,
    state: MutableState<HoldMenuState>,
    modifier: Modifier = Modifier,
    holdDelayMs: Long = 150L,
    content: HoldMenuBuilder.() -> Unit,
) {
    val builder = remember { HoldMenuBuilder() }
    builder.items.clear()
    builder.content()

    Box(
        modifier = modifier
            .size(40.dp)
            .pointerInput(state.value, builder.items) {
                awaitPointerEventScope {

                    while (true) {

                        val down = awaitFirstDown()
                        var isUp = false

                        val timeoutResult = withTimeoutOrNull(holdDelayMs) {
                            while (!isUp) {
                                val e = awaitPointerEvent()
                                val ch = e.changes.first()
                                ch.consume()

                                if (ch.changedToUp() || !ch.pressed) {
                                    isUp = true
                                }
                            }
                        }

                        // long press triggered
                        if (timeoutResult == null && !isUp) {
                            state.value = HoldMenuState(
                                isOpen = true,
                                anchor = down.position,
                                items = builder.items.toList()
                            )
                        }

                        // short tap → ignore (або додамо onClick, якщо хочеш)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
*/