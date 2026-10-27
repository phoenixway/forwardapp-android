package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.semantic.displayLabel
import com.romankozak.forwardappmobile.shared.core.models.orientation.SUBJECT_CONVERSION_TARGET_KINDS

@Composable
internal fun SemanticConversionDialog(state: SemanticConversionState, vm: SemanticConversionViewModel) {
    if (!state.open) return
    AlertDialog(onDismissRequest = vm::cancel,
        title = { Text("Перетворити аспект на орієнтир") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.plan?.let { plan ->
                    Text("«${plan.title}» залишиться тією самою сутністю з новим типом.")
                    Text("Буде завершено зв’язків аспекту: ${plan.memberships}. Дочірніх аспектів у семантичній таксономії буде піднято до попереднього батька або root: ${plan.childIds.size}.")
                    Text("Назва, опис, теги (${plan.tags}), Workspace bindings і всі появи в Navigation збережуться. Актуальні Planning references будуть переприв’язані без створення нових участей; історія збережеться. Знайдено references: ${plan.planningReferences}.")
                    Text("Оберіть тип орієнтира:", style = MaterialTheme.typography.titleSmall)
                    SUBJECT_CONVERSION_TARGET_KINDS.forEach { kind ->
                        Row(Modifier.fillMaxWidth().selectable(selected = state.kind == kind, enabled = !state.busy, role = Role.RadioButton,
                            onClick = { vm.select(kind) }).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.kind == kind, onClick = null, enabled = !state.busy)
                            Text(kind.displayLabel(), Modifier.padding(start = 8.dp))
                        }
                    }
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = vm::confirm, enabled = !state.busy && state.plan != null && state.kind != null) { Text("Перетворити") } },
        dismissButton = { TextButton(onClick = vm::cancel, enabled = !state.busy) { Text("Скасувати") } })
}
