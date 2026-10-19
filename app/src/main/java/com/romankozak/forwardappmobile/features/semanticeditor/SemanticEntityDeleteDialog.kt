package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.data.orientation.SemanticEntityDeletionTarget
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType

internal val SemanticEntityDeletionTarget.classLabel: String get() =
    if (subjectType == ManagedSubjectType.ASPECT) "аспект" else "орієнтир"

@Composable
internal fun SemanticEntityDeleteDialog(
    state: SemanticEntityDeleteState, onConfirm: () -> Unit, onCancel: () -> Unit,
) {
    val target = state.target?.takeIf { state.confirming } ?: return
    AlertDialog(
        onDismissRequest = { if (!state.busy) onCancel() },
        title = { Text("Видалити ${target.classLabel} «${target.title}»?") },
        text = {
            Text(state.error ?: "Буде видалено саму семантичну сутність та всі її появи в Big Picture. Пов’язані робочі простори залишаться. Незбережені зміни буде відкинуто.")
        },
        confirmButton = { TextButton(enabled = !state.busy, onClick = onConfirm) {
            if (state.busy) CircularProgressIndicator() else Text("Видалити", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = onCancel) { Text("Скасувати") } },
    )
}
