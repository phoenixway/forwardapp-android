package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.planning.PlanningEntityPickerDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun TodayPriorityCreationHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: suspend (CanonicalEntitySummary, DayFocusType) -> Unit,
) {
    if (!visible) return

    var source by remember { mutableStateOf<CanonicalEntitySummary?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (source == null) {
        PlanningEntityPickerDialog(
            onDismiss = { if (source == null) onDismiss() },
            onSelected = { picked -> source = picked },
            allowedClasses = setOf(CanonicalEntityClass.ORIENTATION),
        )
    }

    source?.let { selected ->
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text("Тип пріоритету") },
            text = { Text("${selected.title} — фокус чи зона відповідальності?") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayFocusType.entries.forEach { type ->
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        onCreate(selected, type)
                                        onDismiss()
                                    } catch (cancel: CancellationException) {
                                        throw cancel
                                    } catch (failure: Exception) {
                                        error = failure.message ?: "Не вдалося створити пріоритет"
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        ) { Text(if (type == DayFocusType.FOCUS) "Фокус" else "Відповідальність") }
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Скасувати") }
            },
        )
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Не вдалося створити пріоритет") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Гаразд") } },
        )
    }
}
