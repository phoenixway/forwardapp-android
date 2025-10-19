package com.romankozak.forwardappmobile.ui.screens.projectsettings.tabs

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.ui.reminders.components.ReminderSection

@Composable
fun RemindersTabContent(
    reminderTime: Long?,
    onSetReminder: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit,
    onClearReminder: () -> Unit,
) {
    ReminderSection(
        reminderTime = reminderTime,
        onSetReminder = onSetReminder,
        onClearReminder = onClearReminder
    )
}