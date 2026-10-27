package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.romankozak.forwardappmobile.R
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Presentation command: owns no domain state and cannot bypass availability. */
internal data class ContextualAction(
    val id: String,
    val label: String,
    val icon: ImageVector = Icons.Outlined.Tune,
    val selected: Boolean? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
) {
    fun invokeIfEnabled() { if (enabled) onClick() }
}

/** A separate local menu, deliberately not the global More menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContextualActionsButton(
    title: String,
    actions: List<ContextualAction>,
    extraContent: (@Composable (() -> Unit) -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    IconButton(onClick = { focus.clearFocus(); keyboard?.hide(); visible = true }, modifier = Modifier.size(48.dp)) {
        Icon(painterResource(R.drawable.ic_contextual_blocks), contentDescription = "Дії: $title", modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (visible) {
        ModalBottomSheet(onDismissRequest = { visible = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Text(title, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                extraContent?.invoke { visible = false }
                ContextualActionItems(actions) { action ->
                    visible = false
                    action.invokeIfEnabled()
                }
            }
        }
    }
}

@Composable
internal fun ContextualActionItems(actions: List<ContextualAction>, onSelected: (ContextualAction) -> Unit) {
    actions.forEach { action ->
        DropdownMenuItem(
            text = { Text(action.label) },
            leadingIcon = { Icon(action.icon, contentDescription = null) },
            trailingIcon = {
                if (action.selected == true) Icon(Icons.Outlined.Check, contentDescription = "Вибрано")
            },
            enabled = action.enabled,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                action.selected?.let { selected = it }
            },
            onClick = { if (action.enabled) onSelected(action) },
        )
    }
}

/** Light, inset-free contextual content; the shared dock host owns system insets. */
@Composable
internal fun ContextualComposerSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}
