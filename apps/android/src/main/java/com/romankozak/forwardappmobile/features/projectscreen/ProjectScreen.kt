package com.romankozak.forwardappmobile.features.projectscreen

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.MinimalInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuOverlay
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuState
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
    val holdMenuState = remember { mutableStateOf(HoldMenuState()) }

    // Зберігаємо позиції елементів меню
    val itemPositions = remember { mutableStateMapOf<Int, Pair<Offset, IntSize>>() }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var buttonCenter by remember { mutableStateOf(Offset.Zero) }

    val onHoldMenuSelect: (Int) -> Unit = { index ->
        Log.e("HOLDMENU", "🎉 Menu item selected: $index")
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
                    Log.e("HOLDMENU", "🌍 Root: Finger down at ${down.position}")

                    // Перевіряємо чи натиснули на кнопку
                    val isOnButton =
                        buttonCenter != Offset.Zero &&
                                (down.position - buttonCenter).getDistance() < 100f

                    if (!isOnButton) {
                        return@awaitEachGesture
                    }

                    Log.e("HOLDMENU", "🎯 Touch on button!")

                    // Чекаємо 500ms для long press
                    val longPress = withTimeoutOrNull(500) {
                        awaitPointerEvent(PointerEventPass.Main)
                        null
                    }

                    if (longPress == null) {
                        // Long press спрацював!
                        Log.e("HOLDMENU", "🔥 Long press detected, opening menu")
                        holdMenuState.value = HoldMenuState(
                            isOpen = true,
                            anchor = buttonCenter,
                            items = listOf("Backlog", "Advanced", "Inbox", "Attachments"),
                            onItemSelected = onHoldMenuSelect
                        )

                        selectedIndex = null

                        // Тепер обробляємо рухи пальцем
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            val position = change.position

                            Log.e("HOLDMENU", "👆 Dragging at $position")

                            // Шукаємо елемент під пальцем
                            val hoveredIndex = itemPositions.entries.firstOrNull { (_, posSize) ->
                                val (topLeft, size) = posSize
                                position.x >= topLeft.x &&
                                        position.x <= topLeft.x + size.width &&
                                        position.y >= topLeft.y &&
                                        position.y <= topLeft.y + size.height
                            }?.key

                            if (hoveredIndex != selectedIndex) {
                                selectedIndex = hoveredIndex
                                Log.e("HOLDMENU", "🎯 Selected: $hoveredIndex")
                            }

                            // Відпустили палець
                            if (!change.pressed) {
                                Log.e("HOLDMENU", "✅ Released on item: $selectedIndex")
                                selectedIndex?.let { index ->
                                    onHoldMenuSelect(index)
                                }
                                holdMenuState.value = holdMenuState.value.copy(isOpen = false)
                                selectedIndex = null
                                break
                            }

                            change.consume()
                        }
                    }
                }
            }
    ) {
        // Main content
        MinimalInputPanel(
            inputMode = state.inputMode,
            onInputModeSelected = {
                viewModel.onEvent(ProjectScreenViewModel.Event.SwitchInputMode(it))
            },
            holdMenuState = holdMenuState,
            onHoldMenuSelect = onHoldMenuSelect,
            onButtonCenterChanged = { buttonCenter = it },
            modifier = Modifier.zIndex(1f)
        )

        // Overlay для меню
        if (holdMenuState.value.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(999f)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                HoldMenuOverlay(
                    state = holdMenuState.value,
                    selectedIndex = selectedIndex,
                    onItemPositioned = { index, offset, size ->
                        itemPositions[index] = offset to size
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}