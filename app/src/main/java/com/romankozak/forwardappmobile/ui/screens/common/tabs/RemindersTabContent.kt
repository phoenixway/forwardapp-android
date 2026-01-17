package com.romankozak.forwardappmobile.ui.screens.common.tabs

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.reminders.components.ReminderSection

@Composable
fun RemindersTabContent(
    reminderTime: Long?,
    onViewModelAction: RemindersTabActions,
) {
    ReminderSection(
        reminderTime = reminderTime,
        onSetReminder = onViewModelAction::onSetReminder,
        onClearReminder = onViewModelAction::onClearReminder
    )
}