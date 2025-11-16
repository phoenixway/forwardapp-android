package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import android.util.Log
import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.shared.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface InboxMarkdownHandlerResultListener {
  fun copyToClipboard(text: String, label: String)
  fun showSnackbar(message: String, action: String?)
  fun forceRefresh()
}

@Inject
class InboxMarkdownHandler(
  private val projectRepository: ProjectRepository,
  private val goalRepository: GoalRepository,
  private val scope: CoroutineScope,
  private val listener: InboxMarkdownHandlerResultListener,
) {
  fun exportToMarkdown(inboxRecords: List<InboxRecord>) {
    if (inboxRecords.isEmpty()) {
      listener.showSnackbar("Немає записів для експорту.", null)
      return
    }
    val markdownBuilder = StringBuilder()
    inboxRecords.forEach { record ->
      markdownBuilder.appendLine("- [ ] ${record.text}")
    }
    val markdownText = markdownBuilder.toString()
    listener.copyToClipboard(markdownText, "Inbox Export")
    listener.showSnackbar("Записи скопійовано у буфер обміну.", null)
  }

  fun importFromMarkdown(markdownText: String, projectId: String) {
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
          if (trimmedLine.startsWith("- [ ]") || trimmedLine.startsWith("- [x]")) {
            val goalText =
              trimmedLine.removePrefix("- [ ]").removePrefix("- [x]").trim()
            if (goalText.isNotEmpty()) {
              projectRepository.addGoalToProject(goalText, projectId)
              importedCount++
            }
          }
        } catch (e: Exception) {
          Log.e("InboxMarkdownHandler", "Failed to import line: $line", e)
        }
      }
      withContext(Dispatchers.Main) {
        listener.showSnackbar("Імпортовано $importedCount елементів.", null)
        listener.forceRefresh()
      }
    }
  }
}
