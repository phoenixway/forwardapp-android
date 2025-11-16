package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface BacklogMarkdownHandlerResultListener {
    fun copyToClipboard(text: String, label: String)
    fun showSnackbar(message: String, action: String?)
    fun forceRefresh()
}

@Inject
class BacklogMarkdownHandler(
  private val projectRepository: ProjectRepository,
  private val goalRepository: GoalRepository,
  private val scope: CoroutineScope,
  private val listener: BacklogMarkdownHandlerResultListener,
) {
  fun exportToMarkdown(content: List<ListItemContent>) {
    if (content.isEmpty()) {
      listener.showSnackbar("Беклог порожній. Нічого експортувати.", null)
      return
    }
    val markdownBuilder = StringBuilder()
    content.forEach { item ->
      val line =
        when (item) {
          is ListItemContent.GoalItem -> {
            val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
            "$checkbox ${item.goal.text}"
          }

          is ListItemContent.SublistItem -> "- [С] ${item.project.name}"
          is ListItemContent.LinkItem -> {
            val displayName = item.link.linkData.displayName ?: item.link.linkData.target
            "- [Л] [$displayName](${item.link.linkData.target})"
          }
          is ListItemContent.NoteItem -> "- [Н] ${item.note.title}"
          is ListItemContent.NoteDocumentItem -> "- [К] ${item.document.name}"
          is ListItemContent.ChecklistItem -> "- [Ч] ${item.checklist.name}"
        }
      markdownBuilder.appendLine(line)
    }
    val markdownText = markdownBuilder.toString()
    listener.copyToClipboard(markdownText, "Backlog Export")
    listener.showSnackbar("Беклог скопійовано у буфер обміну.", null)
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
          when {
            trimmedLine.startsWith("- [ ]") -> {
              val goalText = trimmedLine.removePrefix("- [ ]").trim()
              if (goalText.isNotEmpty()) {
                goalRepository.addGoalToProject(goalText, projectId, completed = false)
                importedCount++
              }
            }

            trimmedLine.startsWith("- [x]") -> {
              val goalText = trimmedLine.removePrefix("- [x]").trim()
              if (goalText.isNotEmpty()) {
                goalRepository.addGoalToProject(goalText, projectId, completed = true)
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
}
