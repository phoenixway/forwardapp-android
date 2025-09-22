package com.romankozak.forwardappmobile.ui.common.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object ListEditingLogic {

    // Returns Pair<start, end> where end is exclusive
    private fun getCurrentLineBounds(text: String, cursorPosition: Int): Pair<Int, Int> {
        val start = text.lastIndexOf('\n', startIndex = (cursorPosition - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val end = text.indexOf('\n', startIndex = cursorPosition).let { if (it == -1) text.length else it }
        return start to end
    }

    private fun getLineIndentation(line: String): Int {
        return line.takeWhile { it.isWhitespace() }.length
    }

    private fun getBlockLines(text: String, startLineIndex: Int): List<String> {
        val lines = text.lines()
        if (startLineIndex >= lines.size) return emptyList()

        val block = mutableListOf<String>()
        val startLine = lines[startLineIndex]
        val startIndent = getLineIndentation(startLine)
        block.add(startLine)

        for (i in startLineIndex + 1 until lines.size) {
            val nextLine = lines[i]
            if (nextLine.isBlank() || getLineIndentation(nextLine) > startIndent) {
                block.add(nextLine)
            } else {
                break
            }
        }
        return block
    }

    fun toggleList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        val listMarker = "- "
        val newText: String
        val newCursorPosition: Int

        if (currentLine.trim().startsWith("- ")) {
            val lineWithoutMarker = currentLine.replaceFirst(Regex("^\\s*- "), "")
            newText = text.replaceRange(lineStart, lineEnd, lineWithoutMarker)
            newCursorPosition = (cursorPosition - listMarker.length).coerceAtLeast(lineStart)
        } else {
            val newLine = listMarker + currentLine
            newText = text.replaceRange(lineStart, lineEnd, newLine)
            newCursorPosition = cursorPosition + listMarker.length
        }

        return value.copy(text = newText, selection = TextRange(newCursorPosition))
    }

    fun moveLineUp(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (currentLineStart, currentLineEnd) = getCurrentLineBounds(text, cursorPosition)
        if (currentLineStart == 0) return value

        val (prevLineStart, prevLineEnd) = getCurrentLineBounds(text, currentLineStart - 1)
        val currentLine = text.substring(currentLineStart, currentLineEnd)
        val previousLine = text.substring(prevLineStart, prevLineEnd)

        val builder = StringBuilder(text)
        builder.replace(prevLineStart, currentLineEnd, currentLine + "\n" + previousLine)
        val newCursorPosition = cursorPosition - previousLine.length - 1

        return value.copy(text = builder.toString(), selection = TextRange(newCursorPosition))
    }

    fun moveLineDown(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (currentLineStart, currentLineEnd) = getCurrentLineBounds(text, cursorPosition)
        if (currentLineEnd == text.length) return value

        val (nextLineStart, nextLineEnd) = getCurrentLineBounds(text, currentLineEnd + 1)
        val currentLine = text.substring(currentLineStart, currentLineEnd)
        val nextLine = text.substring(nextLineStart, nextLineEnd)

        val builder = StringBuilder(text)
        builder.replace(currentLineStart, nextLineEnd, nextLine + "\n" + currentLine)
        val newCursorPosition = cursorPosition + nextLine.length + 1

        return value.copy(text = builder.toString(), selection = TextRange(newCursorPosition))
    }

    fun indent(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        val indentedLine = "    " + currentLine
        val newText = text.replaceRange(lineStart, lineEnd, indentedLine)
        val newCursorPosition = cursorPosition + 4

        return value.copy(text = newText, selection = TextRange(newCursorPosition))
    }

    fun outdent(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        if (currentLine.startsWith("    ")) {
            val outdentedLine = currentLine.substring(4)
            val newText = text.replaceRange(lineStart, lineEnd, outdentedLine)
            val newCursorPosition = (cursorPosition - 4).coerceAtLeast(lineStart)
            return value.copy(text = newText, selection = TextRange(newCursorPosition))
        }

        return value
    }

    // Block-level operations

    fun indentBlock(value: TextFieldValue): TextFieldValue {
        // TODO: Implement
        return value
    }

    fun outdentBlock(value: TextFieldValue): TextFieldValue {
        // TODO: Implement
        return value
    }

    fun moveBlockUp(value: TextFieldValue): TextFieldValue {
        // TODO: Implement
        return value
    }

    fun moveBlockDown(value: TextFieldValue): TextFieldValue {
        // TODO: Implement
        return value
    }
}
