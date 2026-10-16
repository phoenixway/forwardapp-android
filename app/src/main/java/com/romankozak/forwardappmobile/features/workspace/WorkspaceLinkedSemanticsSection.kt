package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel

/**
 * Small read-only entry point shared by operational and standalone Workspace.
 * Relation mutations remain in Workspace Properties / Semantic Editor.
 */
@Composable
fun WorkspaceLinkedSemanticsSection(
    onOpen: (NavTarget.SemanticEntityEditor) -> Unit,
    viewModel: WorkspaceLinkedSemanticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    val label =
        when (val current = state) {
            is WorkspaceLinkedSemanticsUiState.Ready ->
                "Семантичні зв’язки (${current.links.size})"
            else -> "Семантичні зв’язки"
        }

    TextButton(onClick = { expanded = true }) {
        Text(label)
    }

    if (!expanded) return

    AlertDialog(
        onDismissRequest = { expanded = false },
        title = { Text("Пов’язані семантичні сутності") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when (val current = state) {
                    WorkspaceLinkedSemanticsUiState.Loading ->
                        Text("Завантаження зв’язків…")

                    is WorkspaceLinkedSemanticsUiState.Unavailable ->
                        Text(current.message)

                    is WorkspaceLinkedSemanticsUiState.Ready -> {
                        if (current.links.isEmpty()) {
                            Text("У цьому Workspace немає семантичних зв’язків.")
                        }
                        current.links.forEach { link ->
                            val target = link.navigationTarget()
                            TextButton(
                                enabled = target != null,
                                onClick = {
                                    if (target != null) {
                                        expanded = false
                                        onOpen(target)
                                    }
                                },
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(link.title)
                                    Text(
                                        text = buildString {
                                            append(
                                                link.ref?.entityClass?.displayLabel()
                                                    ?: "Недоступна сутність",
                                            )
                                            append(" · ")
                                            append(link.bindingTypes.joinToString(", "))
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { expanded = false }) {
                Text("Закрити")
            }
        },
    )
}
