package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.romankozak.forwardappmobile.core.navigation.NavTarget

/** Consume the former sheet selection only after opening the canonical editor route. */
internal fun openDayTaskProperties(taskId: String, navigate: (NavTarget) -> Unit, clearSelection: () -> Unit) {
    navigate(NavTarget.EditTask(taskId))
    clearSelection() // Returning to Day must not reopen properties or the task action sheet.
}
