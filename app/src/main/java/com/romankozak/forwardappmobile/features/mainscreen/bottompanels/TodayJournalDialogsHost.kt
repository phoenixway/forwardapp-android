package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.activitytracker.QuickCompletedActionDialog
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay

@Composable
internal fun TodayJournalDialogsHost(
    showClearJournalConfirmDialog: Boolean,
    quickDonePresetText: String?,
    showHoldMenuOverlay: Boolean,
    holdMenuController: HoldMenu2Controller,
    onDismissClearJournal: () -> Unit,
    onConfirmClearJournal: () -> Unit,
    onDismissQuickDone: () -> Unit,
    onConfirmQuickDone: (String, Int?, Int?) -> Unit,
) {
    if (showClearJournalConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearJournal,
            title = { Text("Очистити лог?") },
            text = { Text("Ви впевнені, що хочете видалити всі записи? Цю дію неможливо буде скасувати.") },
            confirmButton = {
                Button(onClick = onConfirmClearJournal) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearJournal) {
                    Text("Скасувати")
                }
            },
        )
    }

    quickDonePresetText?.let { presetText ->
        QuickCompletedActionDialog(
            initialText = presetText,
            onDismiss = onDismissQuickDone,
            onConfirm = onConfirmQuickDone,
        )
    }

    if (showHoldMenuOverlay) {
        HoldMenu2Overlay(controller = holdMenuController)
    }
}
