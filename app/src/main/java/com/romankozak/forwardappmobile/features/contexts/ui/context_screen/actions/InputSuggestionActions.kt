package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

class InputSuggestionActions {
    data class SuggestionApplyResult(
        val text: String,
        val cursorPosition: Int,
    )

    fun applySuggestion(
        currentText: String,
        cursorPosition: Int,
        suggestion: String,
    ): SuggestionApplyResult? {
        val wordInfo = getCurrentWordInfo(currentText, cursorPosition) ?: return null
        val startIndex = currentText.substring(0, cursorPosition).lastIndexOf(wordInfo.prefix)
        if (startIndex < 0) return null
        val newText =
            currentText.substring(0, startIndex) +
                suggestion +
                " " +
                currentText.substring(cursorPosition)
        val newCursorPosition = startIndex + suggestion.length + 1
        return SuggestionApplyResult(
            text = newText,
            cursorPosition = newCursorPosition,
        )
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
        if (lastAt == -1 && lastHash == -1) return null

        val (startIndex, prefix) =
            if (lastAt > lastHash) {
                lastAt to "@"
            } else {
                lastHash to "#"
            }

        val word = textUpToCursor.substring(startIndex + 1)
        if (word.contains(" ")) return null
        return WordInfo(word = word, prefix = prefix)
    }
}
