package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.BacklogMarkdownHandlerResultListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ContextMarkdownExporter
    @Inject
    constructor() {
        fun exportProjectStateToMarkdown(
            project: Context?,
            backlog: List<BacklogItemContent>,
            logs: List<ContextLog>,
            listener: BacklogMarkdownHandlerResultListener,
        ) {
            if (project == null) {
                listener.showSnackbar("Не вдалося завантажити дані проекту.", null)
                return
            }

            val markdown =
                buildString {
                    appendLine("# Звіт по проекту: ${project.name}")
                    appendLine()

                    appendLine("## Поточний стан проекту")
                    appendLine("- **Статус:** ${ContextStatusValues.getDisplayName(project.contextStatus) ?: "Не визначено"}")
                    project.contextStatusText?.takeIf { it.isNotBlank() }?.let {
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

                    if (backlog.isNotEmpty()) {
                        appendLine("## Беклог проекту")
                        backlog.forEach { item ->
                            val line =
                                when (item) {
                                    is BacklogItemContent.GoalItem -> {
                                        val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                                        "$checkbox ${item.goal.text}"
                                    }
                                    is BacklogItemContent.SublistItem -> "- [С] ${item.project.name}"
                                    is BacklogItemContent.LinkItem -> {
                                        val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                                        "- [Л] [$displayName](${item.link.linkData.target})"
                                    }
                                    is BacklogItemContent.NoteItem -> "- [Н] ${item.note.title}"
                                    is BacklogItemContent.NoteDocumentItem -> "- [К] ${item.document.name}"
                                    is BacklogItemContent.ChecklistItem -> "- [Ч] ${item.checklist.name}"
                                }
                            appendLine(line)
                        }
                        appendLine()
                    }

                    if (logs.isNotEmpty()) {
                        appendLine("## Історія проекту (лог)")
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        logs.sortedBy { it.timestamp }.forEach { log ->
                            val date = dateFormat.format(Date(log.timestamp))
                            appendLine("### ${log.type} - $date")
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
