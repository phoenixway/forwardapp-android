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
import com.romankozak.forwardappmobile.shared.core.domain.orientation.axisApplicability
import com.romankozak.forwardappmobile.shared.core.models.orientation.*

@Composable
internal fun OrientationKindDialog(state: OrientationKindState, vm: OrientationKindViewModel) {
    if (!state.open) return
    AlertDialog(onDismissRequest = vm::cancel, title = { Text("Змінити тип орієнтира") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.plan?.let { plan ->
                Text("«${plan.title}» · ${plan.kind.displayLabel()}")
                Text("Збережуться та сама сутність, lifecycle, теги, зв’язки, Workspace і всі появи в Navigation. Planning references та participation (${plan.planningCount}), recurrence (${plan.recurrenceCount}) не змінюються.")
                Text("Оберіть новий тип:", style = MaterialTheme.typography.titleSmall)
                SUBJECT_CONVERSION_TARGET_KINDS.filter { it != plan.kind }.forEach { kind ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = state.target == kind, enabled = !state.busy,
                        role = Role.RadioButton, onClick = { vm.select(kind) }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.target == kind, onClick = null, enabled = !state.busy)
                        Text(kind.displayLabel(), Modifier.padding(start = 8.dp))
                    }
                }
                state.target?.let { target ->
                    if (OrientationAxis.entries.any { axisApplicability(plan.kind, it) != axisApplicability(target, it) })
                        Text("Expected span і Target window отримають стандартні значення нового типу. Попередні оцінки залишаться в історії; решта оцінок і scoring settings збережуться.")
                }
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(onClick = vm::confirm, enabled = state.plan != null && state.target != null && !state.busy) { Text("Змінити тип") }
    }, dismissButton = { TextButton(onClick = vm::cancel, enabled = !state.busy) { Text("Скасувати") } })
}
