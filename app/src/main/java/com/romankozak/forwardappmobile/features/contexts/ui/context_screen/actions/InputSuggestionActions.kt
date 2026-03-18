package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

class InputSuggestionActions {
    data class SuggestionApplyResult(
        val text: String,
        val cursorPosition: Int,
    )

    fun buildSuggestions(
        currentText: String,
        cursorPosition: Int,
        contextMarkerNames: List<String>,
        tags: List<String>,
        limit: Int = 8,
    ): List<String> {
        val wordInfo = getCurrentWordInfo(currentText, cursorPosition) ?: return emptyList()
        val query = wordInfo.word.trim()

        return when (wordInfo.prefix) {
            "@" ->
                contextMarkerNames
                    .asSequence()
                    .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
                    .map { markerName ->
                        if (markerName.startsWith("@")) markerName else "@$markerName"
                    }.distinct()
                    .take(limit)
                    .toList()

            "#" ->
                tags
                    .asSequence()
                    .map { tag ->
                        if (tag.startsWith("#")) tag else "#$tag"
                    }.filter { query.isBlank() || it.contains("#$query", ignoreCase = true) }
                    .distinct()
                    .take(limit)
                    .toList()

            else -> emptyList()
        }
    }

    fun applySuggestion(
        currentText: String,
        cursorPosition: Int,
        suggestion: String,
    ): SuggestionApplyResult? {
        val wordInfo = getCurrentWordInfo(currentText, cursorPosition)
        val startIndex = wordInfo?.let {
            currentText.substring(0, cursorPosition).lastIndexOf(it.prefix)
        } ?: -1

        return if (startIndex >= 0) {
            val newText =
                currentText.substring(0, startIndex) +
                    suggestion +
                    " " +
                    currentText.substring(cursorPosition)
            val newCursorPosition = startIndex + suggestion.length + 1
            SuggestionApplyResult(
                text = newText,
                cursorPosition = newCursorPosition,
            )
        } else {
            null
        }
    }

    private data class WordInfo(
        val word: String,
        val prefix: String,
    )

    private fun getCurrentWordInfo(
        text: String,
        cursorPosition: Int,
    ): WordInfo? {
        val textUpToCursor = text.substring(0, cursorPosition)
        val lastAt = textUpToCursor.lastIndexOf('@')
        val lastHash = textUpToCursor.lastIndexOf('#')
        val markerInfo =
            if (lastAt == -1 && lastHash == -1) {
                null
            } else if (lastAt > lastHash) {
                lastAt to "@"
            } else {
                lastHash to "#"
            }

        return markerInfo?.let { (startIndex, prefix) ->
            val word = textUpToCursor.substring(startIndex + 1)
            if (word.contains(" ")) {
                null
            } else {
                WordInfo(word = word, prefix = prefix)
            }
        }
    }
}
