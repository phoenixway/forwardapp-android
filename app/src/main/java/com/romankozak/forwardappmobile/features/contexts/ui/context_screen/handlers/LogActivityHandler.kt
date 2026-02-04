class LogActivityHandler(
    private val contextRepository: ContextRepository,
    private val activityManager: ActivityManager,
    private val stateManager: ContextStateManager,
    private val scope: CoroutineScope
) {
    fun onEditLogEntry(log: ContextLog) = stateManager.updateState { it.copy(logEntryToEdit = log) }
    fun onDismissEditLogEntryDialog() = stateManager.updateState { it.copy(logEntryToEdit = null) }
    fun onDeleteLogEntry(log: ContextLog) = scope.launch { contextRepository.deleteLogEntry(log.id) }
    fun onUpdateLogEntry(log: ContextLog, text: String) = scope.launch {
        contextRepository.updateContextLog(log.copy(description = text))
        onDismissEditLogEntryDialog()
    }
    
    fun onStartTrackingCurrentProject(id: String) = scope.launch { activityManager.startActivity(id) }
    fun stopOngoingActivity() = scope.launch { activityManager.stopActivity() }
    fun setReminderForOngoingActivity(activity: ActivityRecord, time: Long) { /* логіка */ }
}
