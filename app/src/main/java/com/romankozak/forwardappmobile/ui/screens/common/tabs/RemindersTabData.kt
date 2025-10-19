package com.romankozak.forwardappmobile.ui.screens.common.tabs

interface RemindersTabActions {
    fun onSetReminder(year: Int, month: Int, day: Int, hour: Int, minute: Int)
    fun onClearReminder()
}
