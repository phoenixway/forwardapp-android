
package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Інтерфейс для зворотного зв'язку з ViewModel
 */
interface BacklogMarkdownHandlerResultListener {
    fun copyToClipboard(
        text: String,
        label: String,
    )

    fun showSnackbar(
        message: String,
        action: String?,
    )

    fun forceRefresh()
}

/**
 * Handler для експорту/імпорту Backlog в Markdown форматі
 */
class BacklogMarkdownHandler
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val goalRepository: GoalRepository,
        private val listItemRepository: ListItemRepository,
        private val scope: CoroutineScope,
        private val listener: BacklogMarkdownHandlerResultListener,
    ) {
        companion object {
            private const val TAG = "BacklogMarkdownHandler"
        }

        /**
         * Експортує backlog до Markdown формату
         */
        fun exportToMarkdown(content: List<BacklogItemContent>) {
            if (content.isEmpty()) {
                listener.showSnackbar("Backlog is empty. Nothing to export.", null)
                return
            }

            val markdownBuilder = StringBuilder()

            content.forEach { item ->
                val line =
                    when (item) {
                        is BacklogItemContent.GoalItem -> {
                            val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                            "$checkbox ${item.goal.text}"
                        }
                        is BacklogItemContent.ContextLinkItem -> {
                            "- [C] ${item.project.name}"
                        }
                        is BacklogItemContent.LinkItem -> {
                            val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                            "- [L] [$displayName](${item.link.linkData.target})"
                        }
                        is BacklogItemContent.NoteItem -> {
                            "- [N] ${item.note.title}"
                        }
                        is BacklogItemContent.NoteDocumentItem -> {
                            "- [D] ${item.document.name}"
                        }
                        is BacklogItemContent.ChecklistItem -> {
                            "- [Ch] ${item.checklist.name}"
                        }
                    }
                markdownBuilder.appendLine(line)
            }

            val markdownText = markdownBuilder.toString()
            listener.copyToClipboard(markdownText, "Backlog Export")
            listener.showSnackbar("Backlog copied to clipboard.", null)
        }

        /**
         * Імпортує backlog з Markdown формату
         *
         * Підтримувані формати:
         * - [ ] - незавершений goal
         * - [x] - завершений goal
         * - [C] - контекст (поки не імплементовано)
         * - [L] - лінк (поки не імплементовано)
         * - [N] - нотатка (поки не імплементовано)
         */
        fun importFromMarkdown(
            markdownText: String,
            contextId: String,
        ) {
            if (markdownText.isBlank()) {
                listener.showSnackbar("Nothing to import.", null)
                return
            }

            scope.launch(Dispatchers.IO) {
                val lines = markdownText.lines().filter { it.isNotBlank() }
                var importedCount = 0

                for (line in lines) {
                    try {
                        val trimmedLine = line.trim()

                        when {
                            // Незавершений goal
                            trimmedLine.startsWith("- [ ]") -> {
                                val goalText = trimmedLine.removePrefix("- [ ]").trim()
                                if (goalText.isNotEmpty()) {
                                    goalRepository.addGoalToContext(goalText, contextId, completed = false)
                                    importedCount++
                                }
                            }

                            // Завершений goal
                            trimmedLine.startsWith("- [x]") || trimmedLine.startsWith("- [X]") -> {
                                val goalText =
                                    trimmedLine
                                        .removePrefix("- [x]")
                                        .removePrefix("- [X]")
                                        .trim()
                                if (goalText.isNotEmpty()) {
                                    goalRepository.addGoalToContext(goalText, contextId, completed = true)
                                    importedCount++
                                }
                            }

                            // TODO: Додати підтримку інших типів:
                            // - [C] для контекстів
                            // - [L] для лінків
                            // - [N] для нотаток
                            // - [D] для документів
                            // - [Ch] для чеклістів
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to import line: $line", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    listener.showSnackbar("Imported $importedCount items.", null)
                    listener.forceRefresh()
                }
            }
        }

        /**
         * Експортує окремий item до Markdown рядка
         */
        fun itemToMarkdown(item: BacklogItemContent): String {
            return when (item) {
                is BacklogItemContent.GoalItem -> {
                    val checkbox = if (item.goal.completed) "[x]" else "[ ]"
                    "- $checkbox ${item.goal.text}"
                }
                is BacklogItemContent.ContextLinkItem -> {
                    "- [C] ${item.project.name}"
                }
                is BacklogItemContent.LinkItem -> {
                    val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                    "- [L] [$displayName](${item.link.linkData.target})"
                }
                is BacklogItemContent.NoteItem -> {
                    "- [N] ${item.note.title}"
                }
                is BacklogItemContent.NoteDocumentItem -> {
                    "- [D] ${item.document.name}"
                }
                is BacklogItemContent.ChecklistItem -> {
                    "- [Ch] ${item.checklist.name}"
                }
            }
        }

        /**
         * Експортує вибрані items до Markdown
         */
        fun exportSelectedToMarkdown(items: List<BacklogItemContent>) {
            if (items.isEmpty()) {
                listener.showSnackbar("No items selected.", null)
                return
            }

            val markdown = items.joinToString("\n") { itemToMarkdown(it) }
            listener.copyToClipboard(markdown, "Selected Items")
            listener.showSnackbar("${items.size} items copied to clipboard.", null)
        }
    }
