package com.romankozak.forwardappmobile.features.projectscreen

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.MinimalInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.holdmenu2.*
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    projectId: String?,
) {
    val appComponent = LocalAppComponent.current

    val viewModel: ProjectScreenViewModel = viewModel(
        factory = appComponent.viewModelFactory
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val holdMenu = rememberHoldMenu2()
    val density = LocalDensity.current

    var buttonAnchor by remember { mutableStateOf(Offset.Zero) }

    val onHoldMenuSelect: (Int) -> Unit = { index ->
        Log.e("HOLDMENU2", "🎉 Menu item $index selected")
        when (index) {
            0 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Backlog))
            1 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Advanced))
            2 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Inbox))
            3 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Attachments))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Перевіряємо чи натиснули на кнопку (64dp box)
                    val isOnButton = buttonAnchor != Offset.Zero &&
                            (down.position - buttonAnchor).getDistance() < 100f

                    if (!isOnButton) return@awaitEachGesture

                    Log.e("HOLDMENU2", "👇 Root: Finger on button")

                    // Чекаємо long press
                    val longPress = withTimeoutOrNull(400) {
                        awaitPointerEvent(PointerEventPass.Main)
                        null
                    }

                    if (longPress == null) {
                        // Long press!
                        Log.e("HOLDMENU2", "🔥 Root: Opening menu")
                        holdMenu.open(
                            anchor = buttonAnchor,
                            touch = down.position,
                            items = listOf("Backlog", "Advanced", "Inbox", "Attachments"),
                            onSelect = onHoldMenuSelect
                        )

                        // Обробляємо drag - використовуємо ту саму логіку, що й в Popup
                        val itemH = with(density) { 48.dp.toPx() }
                        val menuWidthPx = with(density) { 220.dp.toPx() }
                        val menuHeightPx = itemH * 4

                        val desiredX = buttonAnchor.x - menuWidthPx / 2f
                        val desiredY = buttonAnchor.y - menuHeightPx - 16f

                        val menuTop = desiredY.coerceAtLeast(8f)

                        Log.e("HOLDMENU2", "📐 Menu calc: itemH=$itemH, menuHeight=$menuHeightPx, menuTop=$menuTop, buttonY=${buttonAnchor.y}")

                        // Початкова позиція
                        var currentPos = down.position
                        Log.e("HOLDMENU2", "👆 Initial pos=$currentPos")

                        // Обчислюємо початковий hover
                        val initialRelativeY = currentPos.y - menuTop
                        val initialHover = if (initialRelativeY >= 0 && initialRelativeY <= menuHeightPx) {
                            (initialRelativeY / itemH).toInt().coerceIn(0, 3)
                        } else {
                            -1
                        }
                        holdMenu.setHover(initialHover)
                        Log.e("HOLDMENU2", "🎯 Initial hover: $initialHover")

                        while (true) {
                            // Чекаємо наступну подію
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break

                            // Оновлюємо позицію ДО перевірки pressed
                            currentPos = change.position

                            // Обчислюємо hover для поточної позиції
                            val relativeY = currentPos.y - menuTop
                            val hover = if (relativeY >= 0 && relativeY <= menuHeightPx) {
                                (relativeY / itemH).toInt().coerceIn(0, 3)
                            } else {
                                -1
                            }

                            // Оновлюємо hover якщо змінився
                            if (hover != holdMenu.state.hoverIndex) {
                                Log.e("HOLDMENU2", "🎯 Hover: $hover (pos=$currentPos, relativeY=$relativeY)")
                                holdMenu.setHover(hover)
                            }

                            // Відпустили - виконуємо селект
                            if (!change.pressed) {
                                Log.e("HOLDMENU2", "✅ Released on: $hover")
                                if (hover >= 0) {
                                    onHoldMenuSelect(hover)
                                }
                                holdMenu.close()
                                break
                            }

                            change.consume()
                        }
                    }
                }
            }
    ) {
        MinimalInputPanel(
            inputMode = state.inputMode,
            onInputModeSelected = {
                viewModel.onEvent(
                    ProjectScreenViewModel.Event.SwitchInputMode(it)
                )
            },
            onButtonAnchorChanged = { buttonAnchor = it },
            modifier = Modifier.zIndex(1f)
        )

        // Overlay тільки для візуалізації
        HoldMenu2Overlay(
            controller = holdMenu,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f)
        )
    }
}