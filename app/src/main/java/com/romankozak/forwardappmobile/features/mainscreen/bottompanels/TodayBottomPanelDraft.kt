package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

internal data class TodayQuickTaskDraft(
    val title: String,
    val description: String,
)

private const val TODAY_QUICK_TASK_TITLE_LIMIT = 100

internal fun buildTodayQuickTaskDraft(rawInput: String): TodayQuickTaskDraft? {
    val normalizedInput =
        rawInput
            .replace("\r\n", "\n")
            .trim()
    val lines = normalizedInput.takeIf { it.isNotBlank() }?.lines().orEmpty()
    val firstLine = lines.firstOrNull()?.trim().orEmpty()
    val remainingText =
        lines
            .drop(1)
            .joinToString("\n")
            .trim()

    return firstLine.takeIf { it.isNotBlank() }?.let { title ->
        if (title.length <= TODAY_QUICK_TASK_TITLE_LIMIT) {
            TodayQuickTaskDraft(
                title = title,
                description = remainingText,
            )
        } else {
            TodayQuickTaskDraft(
                title = title.take(TODAY_QUICK_TASK_TITLE_LIMIT).trimEnd(),
                description = listOf(title.drop(TODAY_QUICK_TASK_TITLE_LIMIT).trim(), remainingText)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            )
        }
    }
}
