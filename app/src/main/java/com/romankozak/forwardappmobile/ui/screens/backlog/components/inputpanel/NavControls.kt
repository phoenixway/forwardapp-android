// file: NavControls.kt

package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun NavControls(state: NavPanelState, actions: NavPanelActions, contentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Кнопка "Закрити пошук", видима лише в режимі пошуку
        AnimatedVisibility(visible = state.inputMode == InputMode.SearchInList) {
            val closeIconColor by animateColorAsState(contentColor.copy(alpha = 0.7f), label = "closeIconColor")
            IconButton(onClick = actions.onCloseSearch, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Close,
                    "Закрити пошук",
                    tint = closeIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Кнопка "Назад", видима, коли пошук неактивний
        AnimatedVisibility(visible = state.inputMode != InputMode.SearchInList) {
            AnimatedVisibility(visible = state.canGoBack) {
                val backIconColor by animateColorAsState(if (state.canGoBack) contentColor else contentColor.copy(alpha = 0.3f), label = "backIconColor")
                val backIconScale by animateFloatAsState(if (state.canGoBack) 1.2f else 1.0f, label = "backIconScale")
                IconButton(onClick = actions.onBackClick, enabled = state.canGoBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Назад",
                        tint = backIconColor,
                        modifier = Modifier.size(20.dp).scale(backIconScale)
                    )
                }
            }
        }

        // ❌ Кнопку "Вперед" видалено

        // Відступ для візуального розділення
        Spacer(modifier = Modifier.width(8.dp))

        // Головні кнопки, які видно завжди
        val homeIconColor by animateColorAsState(contentColor.copy(alpha = 0.7f), label = "homeIconColor")
        val homeIconScale by animateFloatAsState(1.0f, label = "homeIconScale")
        IconButton(onClick = actions.onHomeClick, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Home,
                "Дім",
                tint = homeIconColor,
                modifier = Modifier.size(20.dp).scale(homeIconScale)
            )
        }

        val revealIconColor by animateColorAsState(contentColor.copy(alpha = 0.7f), label = "revealIconColor")
        val revealIconScale by animateFloatAsState(1.0f, label = "revealIconScale")
        IconButton(onClick = actions.onRevealInExplorer, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.MyLocation,
                "Показати у списку",
                tint = revealIconColor,
                modifier = Modifier.size(20.dp).scale(revealIconScale)
            )
        }
    }
}