package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusDialogMode
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusItemEditorSheet
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel

@Composable
internal fun TodayFocusDialogsHost(
    dayFocusesUiState: DayFocusesUiState,
    dayFocusesViewModel: DayFocusesViewModel,
    predictedDayDurationMinutes: Long? = null,
) {
    val totalBudgetPercent = dayFocusesUiState.items.sumOf { it.budgetPercent ?: 0 }
    when (val dialogMode = dayFocusesUiState.dialogMode) {
        is DayFocusDialogMode.Create ->
            DayFocusItemEditorSheet(
                initialType = dialogMode.type,
                availableContexts = dayFocusesUiState.availableContexts,
                availableAttachments = dayFocusesUiState.availableAttachments,
                otherBudgetPercent = totalBudgetPercent,
                predictedDayDurationMinutes = predictedDayDurationMinutes,
                onDismiss = dayFocusesViewModel::dismissDialog,
                onConfirm = dayFocusesViewModel::saveItem,
                onCreateDocumentForPicker = dayFocusesViewModel::createDocumentForPicker,
            )

        is DayFocusDialogMode.Edit ->
            DayFocusItemEditorSheet(
                existingItem = dialogMode.item,
                initialType = dialogMode.item.type,
                availableContexts = dayFocusesUiState.availableContexts,
                availableAttachments = dayFocusesUiState.availableAttachments,
                otherBudgetPercent = totalBudgetPercent - (dialogMode.item.budgetPercent ?: 0),
                predictedDayDurationMinutes = predictedDayDurationMinutes,
                onDismiss = dayFocusesViewModel::dismissDialog,
                onConfirm = dayFocusesViewModel::saveItem,
                onCreateDocumentForPicker = dayFocusesViewModel::createDocumentForPicker,
            )

        null -> Unit
    }

    dayFocusesUiState.pendingDeleteItem?.let { item ->
        val isRecurring = item.recurrenceSeriesId != null

        AlertDialog(
            onDismissRequest = dayFocusesViewModel::dismissDeleteRequest,
            title = { Text("Видалити елемент?") },
            text = {
                Text(
                    if (isRecurring) {
                        "Це повторюваний елемент. Видалити його з усіх днів чи тільки з сьогодні?"
                    } else {
                        "Видалити \"${item.title}\" зі списку фокусів дня?"
                    },
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRecurring) {
                        TextButton(onClick = dayFocusesViewModel::confirmDeleteCurrentOnly) {
                            Text("Лише сьогодні")
                        }
                    }
                    Button(
                        onClick =
                            if (isRecurring) {
                                dayFocusesViewModel::confirmDeleteEverywhere
                            } else {
                                dayFocusesViewModel::confirmDeleteCurrentOnly
                            },
                    ) {
                        Text(if (isRecurring) "З усіх днів" else "Видалити")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = dayFocusesViewModel::dismissDeleteRequest) {
                    Text("Скасувати")
                }
            },
        )
    }
}
