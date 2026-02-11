package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.BacklogMarkdownHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.InboxMarkdownHandler

class MarkdownActions(
    private val backlogMarkdownHandler: BacklogMarkdownHandler,
    private val inboxMarkdownHandler: InboxMarkdownHandler,
    private val stateManager: ContextStateManager,
    private val copyToClipboard: (String, String) -> Unit,
    private val showSnackbar: (String, String?) -> Unit,
) {
    fun onExportBacklogToMarkdown(items: List<BacklogItemContent>) {
        backlogMarkdownHandler.exportToMarkdown(items)
    }

    fun onImportBacklogFromMarkdown(markdownText: String, contextId: String) {
        backlogMarkdownHandler.importFromMarkdown(markdownText, contextId)
    }

    fun onShowImportBacklogFromMarkdownDialog() {
        stateManager.updateState { it.copy(showImportBacklogFromMarkdownDialog = true) }
    }

    fun onDismissImportBacklogFromMarkdownDialog() {
        stateManager.updateState { it.copy(showImportBacklogFromMarkdownDialog = false) }
    }

    fun onExportInboxToMarkdown(records: List<InboxRecord>) {
        inboxMarkdownHandler.exportToMarkdown(records)
    }

    fun onImportFromMarkdownRequest() {
        stateManager.updateState { it.copy(showImportFromMarkdownDialog = true) }
    }

    fun onImportFromMarkdownDismiss() {
        stateManager.updateState { it.copy(showImportFromMarkdownDialog = false) }
    }

    fun onImportBacklogFromMarkdownConfirm(markdownText: String, contextId: String) {
        onImportBacklogFromMarkdown(markdownText, contextId)
        onDismissImportBacklogFromMarkdownDialog()
    }

    fun onImportFromMarkdownConfirm(markdownText: String, contextId: String) {
        inboxMarkdownHandler.importFromMarkdown(markdownText, contextId)
        onImportFromMarkdownDismiss()
    }

    fun onCopyBacklogToClipboardRequest(items: List<BacklogItemContent>) {
        backlogMarkdownHandler.exportToMarkdown(items)
        showSnackbar("Беклог скопійовано", null)
        stateManager.updateState { it.copy(showShareDialog = false) }
    }

    fun copyInboxRecordText(text: String) {
        copyToClipboard(text, "Inbox Record")
    }
}
