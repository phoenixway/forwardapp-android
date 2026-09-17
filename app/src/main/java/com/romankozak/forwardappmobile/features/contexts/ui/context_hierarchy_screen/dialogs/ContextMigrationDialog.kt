package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationCandidate
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextMigrationChoice
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

@Composable
fun ContextMigrationDialog(
    state: DialogState.ContextMigration,
    onDismiss: () -> Unit,
    onChoice: (ContextMigrationChoice) -> Unit,
    onOrientationKind: (OrientationKind) -> Unit,
    onExistingAspect: (String) -> Unit,
    onExistingOrientation: (String) -> Unit,
    onRequestConfirmation: () -> Unit,
    onExecute: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Міграція контексту: ${state.projectName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Рекомендація: ${recommendationLabel(state.preview.suggestedOutcome.name)} (${state.preview.confidence.name})")
                state.preview.reasons.forEach { Text("• $it") }
                Text("Це лише рекомендація. Міграція не виконується автоматично.", Modifier.padding(top = 8.dp))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ContextMigrationChoice.entries.forEach { choice ->
                    ChoiceRow(choice, state.selectedChoice == choice) { onChoice(choice) }
                }
                when (state.selectedChoice) {
                    ContextMigrationChoice.NEW_ORIENTATION -> {
                        Text("Тип нового орієнтиру")
                        OrientationKind.entries.forEach { kind ->
                            ChoiceRow(kind.name, state.selectedOrientationKind == kind) { onOrientationKind(kind) }
                        }
                    }
                    ContextMigrationChoice.EXISTING_ASPECT -> CandidatePicker("Оберіть аспект", state.aspectCandidates, state.selectedExistingAspectId, onExistingAspect)
                    ContextMigrationChoice.EXISTING_ORIENTATION -> CandidatePicker("Оберіть орієнтир", state.orientationCandidates, state.selectedExistingOrientationId, onExistingOrientation)
                    else -> Unit
                }
                state.errorMessage?.let { Text(it, Modifier.padding(top = 8.dp)) }
                if (state.confirmationRequested) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Контекст буде завершено як legacy-запис. Ідентичність робочого простору та його стан збережуться. Міграція виконується лише для legacy-листа.")
                }
            }
        },
        confirmButton = {
            if (state.confirmationRequested) {
                Button(onClick = onExecute, enabled = !state.isExecuting) { Text("Підтвердити міграцію") }
            } else {
                Button(onClick = onRequestConfirmation) { Text("Продовжити") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun ChoiceRow(choice: ContextMigrationChoice, selected: Boolean, onClick: () -> Unit) =
    ChoiceRow(
        label = when (choice) {
            ContextMigrationChoice.NEW_ASPECT -> "Новий аспект"
            ContextMigrationChoice.EXISTING_ASPECT -> "Існуючий аспект"
            ContextMigrationChoice.NEW_ORIENTATION -> "Новий орієнтир"
            ContextMigrationChoice.EXISTING_ORIENTATION -> "Існуючий орієнтир"
            ContextMigrationChoice.WORKSPACE_ONLY -> "Лише робочий простір"
        },
        selected = selected,
        onClick = onClick,
    )

@Composable
private fun CandidatePicker(label: String, candidates: List<ContextMigrationCandidate>, selected: String?, onSelect: (String) -> Unit) {
    Text(label)
    candidates.forEach { candidate -> ChoiceRow(candidate.title, selected == candidate.id) { onSelect(candidate.id) } }
}

private fun recommendationLabel(outcome: String): String =
    when (outcome) {
        "ASPECT_AND_WORKSPACE" -> "Новий аспект"
        "ORIENTATION_AND_WORKSPACE" -> "Новий орієнтир"
        "WORKSPACE_ONLY" -> "Лише робочий простір"
        "WORKSPACE_WITH_RELATIONS" -> "Потрібні окремі semantic/relation рішення"
        else -> "Потрібен перегляд"
    }
