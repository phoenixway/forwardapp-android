package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Старий конфіг для простих випадків (залишений для сумісності).
 */
data class FAHeaderConfig(
    val left: (@Composable () -> Unit)? = null,
    val center: (@Composable () -> Unit)? = null,
    val right: (@Composable () -> Unit)? = null,
    val backgroundStyle: FAHeaderBackground = FAHeaderBackground.Default
)

/**
 * Тип фону хедера.
 */
enum class FAHeaderBackground {
    Default,
    Transparent,
    Elevated,
    Gradient,
    CommandDeck
}

/**
 * Базовий інтерфейс для будь-якого layout-у хедера.
 */
interface HeaderLayout {
    @Composable
    fun Content()
}

/**
 * Проста одно-рядкова схема:
 * LEFT | CENTER | RIGHT
 */
class LeftCenterCombinedHeaderLayout(
    private val left: (@Composable () -> Unit)? = null,
    private val center: (@Composable () -> Unit)? = null,
    private val right: (@Composable () -> Unit)? = null,
    private val onClick: (() -> Unit)? = null
) : HeaderLayout {

    @Composable
    override fun Content() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onClick?.invoke()
                        }
                    )
                }
        ) {
            left?.let {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    it()
                }
            }
            center?.let {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    it()
                }
            }
            right?.let {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    it()
                }
            }
        }
    }
}

/**
 * Гнучка дво-рядкова схема:
 *
 *  TOP:    topLeft      topCenter      topRight
 *  BOTTOM: bottomLeft   bottomCenter   bottomRight
 *
 * Будь-який слот може бути null.
 */
class FreeFormHeaderLayout(
    private val topLeft: (@Composable () -> Unit)? = null,
    private val topCenter: (@Composable () -> Unit)? = null,
    private val topRight: (@Composable () -> Unit)? = null,
    private val bottomLeft: (@Composable () -> Unit)? = null,
    private val bottomCenter: (@Composable () -> Unit)? = null,
    private val bottomRight: (@Composable () -> Unit)? = null,
) : HeaderLayout {

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (topLeft != null || topCenter != null || topRight != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    topLeft?.let {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) { it() }
                    }
                    topCenter?.let {
                        Box(modifier = Modifier.align(Alignment.Center)) { it() }
                    }
                    topRight?.let {
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) { it() }
                    }
                }
            }

            if (bottomLeft != null || bottomCenter != null || bottomRight != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    bottomLeft?.let {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) { it() }
                    }
                    bottomCenter?.let {
                        Box(modifier = Modifier.align(Alignment.Center)) { it() }
                    }
                    bottomRight?.let {
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) { it() }
                    }
                }
            }
        }
    }
}
