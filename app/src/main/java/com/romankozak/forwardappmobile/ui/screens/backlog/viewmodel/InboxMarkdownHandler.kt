package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxMarkdownHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listener: ResultListener
) {
    interface ResultListener {
        fun showSnackbar(message: String, action: String? = null)
        fun copyToClipboard(text: String, label: String)
        fun forceRefresh()
    }

    fun importFromMarkdown(markdownText: String, listId: String) {
        if (listId.isEmpty()) {
            listener.showSnackbar("Помилка: не вдалося визначити поточний список.", null)
            return
        }

        scope.launch(Dispatchers.IO) {
            val lines = markdownText.lines()
                .map { it.trim() }
                .filter { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("+ ") }
                .map { it.substring(2).trim() }
                .filter { it.isNotEmpty() }

            if (lines.isEmpty()) {
                withContext(Dispatchers.Main) {
                    listener.showSnackbar("Не знайдено жодного запису для імпорту.", null)
                }
                return@launch
            }

            lines.forEach { text ->
                goalRepository.addInboxRecord(text, listId)
            }

            withContext(Dispatchers.Main) {
                listener.showSnackbar("Імпортовано ${lines.size} записів.", null)
                listener.forceRefresh()
            }
        }
    }

    fun exportToMarkdown(records: List<InboxRecord>) {
        if (records.isEmpty()) {
            listener.showSnackbar("Інбокс порожній. Немає що експортувати.", null)
            return
        }

        val markdownText = records.joinToString("\n") { record ->
            "- ${record.text}"
        }

        listener.copyToClipboard(markdownText, "Inbox Export")
        listener.showSnackbar("Записи інбоксу скопійовано у буфер обміну.", null)
    }
}