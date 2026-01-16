package com.romankozak.forwardappmobile.ui.screens.contextcreen.viewmodel

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxMarkdownHandler(
    private val projectRepository: ProjectRepository,
    private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
    private val scope: CoroutineScope,
    private val listener: ResultListener,
) {
    interface ResultListener {
        fun showSnackbar(
            message: String,
            action: String? = null,
        )

        fun copyToClipboard(
            text: String,
            label: String,
        )

        fun forceRefresh()
    }

    fun importFromMarkdown(
        markdownText: String,
        projectId: String,
    ) {
        if (markdownText.isBlank()) {
            listener.showSnackbar("Нічого імпортувати.", null)
            return
        }
        scope.launch(Dispatchers.IO) {
            val lines = markdownText.lines().filter { it.isNotBlank() }
            var importedCount = 0
            for (line in lines) {
                try {
                    val trimmedLine = line.trim()
                    when {
                        trimmedLine.startsWith("- [x]") -> {
                            val goalText = trimmedLine.removePrefix("- [x]").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToProject(goalText, projectId, completed = true)
                                importedCount++
                            }
                        }
                        trimmedLine.startsWith("- [ ]") -> {
                            val goalText = trimmedLine.removePrefix("- [ ]").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToProject(goalText, projectId, completed = false)
                                importedCount++
                            }
                        }
                        trimmedLine.startsWith("- ") -> {
                            val goalText = trimmedLine.removePrefix("- ").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToProject(goalText, projectId, completed = false)
                                importedCount++
                            }
                        }
                        trimmedLine.startsWith("* ") -> {
                            val goalText = trimmedLine.removePrefix("* ").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToProject(goalText, projectId, completed = false)
                                importedCount++
                            }
                        }
                        trimmedLine.startsWith("+ ") -> {
                            val goalText = trimmedLine.removePrefix("+ ").trim()
                            if (goalText.isNotEmpty()) {
                                goalRepository.addGoalToProject(goalText, projectId, completed = false)
                                importedCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BacklogMarkdownHandler", "Failed to import line: $line", e)
                }
            }
            withContext(Dispatchers.Main) {
                listener.showSnackbar("Імпортовано $importedCount елементів.", null)
                listener.forceRefresh()
            }
        }
    }

    fun exportToMarkdown(records: List<InboxRecord>) {
        if (records.isEmpty()) {
            listener.showSnackbar("Інбокс порожній. Немає що експортувати.", null)
            return
        }

        val markdownText =
            records.joinToString("\n") { record ->
                "- ${record.text}"
            }

        listener.copyToClipboard(markdownText, "Inbox Export")
        listener.showSnackbar("Записи інбоксу скопійовано у буфер обміну.", null)
    }
}
