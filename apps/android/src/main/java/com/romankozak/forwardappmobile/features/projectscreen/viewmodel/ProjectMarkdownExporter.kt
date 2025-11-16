package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import com.romankozak.forwardappmobile.shared.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectExecutionLog
import me.tatarka.inject.annotations.Inject

@Inject
class ProjectMarkdownExporter {

    fun exportProjectStateToMarkdown(
        project: Project?,
        backlog: List<ListItemContent>,
        logs: List<ProjectExecutionLog>,
        listener: BacklogMarkdownHandlerResultListener,
    ) {
        if (project == null) {
            listener.showSnackbar("Проект не завантажено.", null)
            return
        }

        val markdownBuilder = StringBuilder()
        markdownBuilder.appendLine("# ${project.name}")
        markdownBuilder.appendLine()

        if (backlog.isNotEmpty()) {
            markdownBuilder.appendLine("## Беклог")
            backlog.forEach { item ->
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
            markdownBuilder.appendLine()
        }

        if (logs.isNotEmpty()) {
            markdownBuilder.appendLine("## Логи")
            logs.forEach { log ->
                markdownBuilder.appendLine("- ${log.description}")
            }
            markdownBuilder.appendLine()
        }

        val markdownText = markdownBuilder.toString()
        listener.copyToClipboard(markdownText, "Project State Export")
        listener.showSnackbar("Стан проекту скопійовано у буфер обміну.", null)
    }
}
