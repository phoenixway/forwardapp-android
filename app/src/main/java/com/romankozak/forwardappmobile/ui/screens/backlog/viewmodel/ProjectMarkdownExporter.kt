package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.ui.screens.backlog.BacklogMarkdownHandlerResultListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ProjectMarkdownExporter @Inject constructor() {

    fun exportProjectStateToMarkdown(
        project: GoalList?,
        backlog: List<ListItemContent>,
        logs: List<ProjectExecutionLog>,
        listener: BacklogMarkdownHandlerResultListener
    ) {
        if (project == null) {
            listener.showSnackbar("Не вдалося завантажити дані проекту.", null)
            return
        }

        val markdown = buildString {
            appendLine("# Звіт по проекту: ${project.name}")
            appendLine()

            // Section 1: Current Status
            appendLine("## Поточний стан проекту")
            appendLine("- **Статус:** ${project.projectStatus?.displayName ?: "Не визначено"}")
            project.projectStatusText?.takeIf { it.isNotBlank() }?.let {
                appendLine("- **Коментар до статусу:** $it")
            }
            project.totalTimeSpentMinutes?.let { minutes ->
                if (minutes > 0) {
                    val hours = minutes / 60
                    val remainingMinutes = minutes % 60
                    appendLine("- **Загальний витрачений час:** $hours год $remainingMinutes хв")
                }
            }
            appendLine()

            // Section 2: Backlog
            if (backlog.isNotEmpty()) {
                appendLine("## Беклог проекту")
                backlog.forEach { item ->
                    val line = when (item) {
                        is ListItemContent.GoalItem -> {
                            val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                            "$checkbox ${item.goal.text}"
                        }
                        is ListItemContent.SublistItem -> "- [С] ${item.sublist.name}" // С for Sublist
                        is ListItemContent.LinkItem -> {
                            val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                            "- [Л] [$displayName](${item.link.linkData.target})" // Л for Link
                        }
                    }
                    appendLine(line)
                }
                appendLine()
            }

            // Section 3: Project Log
            if (logs.isNotEmpty()) {
                appendLine("## Історія проекту (лог)")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                logs.sortedBy { it.timestamp }.forEach { log ->
                    val date = dateFormat.format(Date(log.timestamp))
                    appendLine("### ${log.type.name} - $date")
                    appendLine(log.description)
                    log.details?.let {
                        appendLine("\n> Деталі: $it\n")
                    }
                    appendLine("---")
                }
            }
        }

        listener.copyToClipboard(markdown, "Project State Export")
        listener.showSnackbar("Стан проекту скопійовано у буфер обміну.", null)
    }
}
