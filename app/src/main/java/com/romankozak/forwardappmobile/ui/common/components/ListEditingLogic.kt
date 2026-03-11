package com.romankozak.forwardappmobile.ui.common.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// Returns Pair<start, end> where end is exclusive
private fun getCurrentLineBounds(
    text: String,
    cursorPosition: Int,
): Pair<Int, Int> {
    val start =
        text.lastIndexOf('\n', startIndex = (cursorPosition - 1).coerceAtLeast(0))
            .let { if (it == -1) 0 else it + 1 }
    val end = text.indexOf('\n', startIndex = cursorPosition).let { if (it == -1) text.length else it }
    return start to end
}

private fun getLineIndentation(line: String): Int {
    return line.takeWhile { it.isWhitespace() }.length
}

private fun getBlockLines(
    text: String,
    startLineIndex: Int,
): List<String> {
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

object ListEditingLogic {
    private const val INDENT_SIZE = 4
    private const val INDENT = "    "
    private const val LIST_MARKER = "- "

    fun toggleList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        val newText: String
        val newCursorPosition: Int

        if (currentLine.trim().startsWith(LIST_MARKER)) {
            val lineWithoutMarker = currentLine.replaceFirst(Regex("^\\s*- "), "")
            newText = text.replaceRange(lineStart, lineEnd, lineWithoutMarker)
            newCursorPosition = (cursorPosition - LIST_MARKER.length).coerceAtLeast(lineStart)
        } else {
            val newLine = LIST_MARKER + currentLine
            newText = text.replaceRange(lineStart, lineEnd, newLine)
            newCursorPosition = cursorPosition + LIST_MARKER.length
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

        val indentedLine = INDENT + currentLine
        val newText = text.replaceRange(lineStart, lineEnd, indentedLine)
        val newCursorPosition = cursorPosition + INDENT_SIZE

        return value.copy(text = newText, selection = TextRange(newCursorPosition))
    }

    fun outdent(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        if (currentLine.startsWith(INDENT)) {
            val outdentedLine = currentLine.substring(INDENT_SIZE)
            val newText = text.replaceRange(lineStart, lineEnd, outdentedLine)
            val newCursorPosition = (cursorPosition - INDENT_SIZE).coerceAtLeast(lineStart)
            return value.copy(text = newText, selection = TextRange(newCursorPosition))
        }

        return value
    }

    fun moveBlockUp(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val lines = text.lines()
        val currentLineNumber = text.substring(0, cursorPosition).count { it == '\n' }

        val currentBlock = getBlockLines(text, currentLineNumber)
        val prevLineNumber = (currentLineNumber - 1).coerceAtLeast(0)
        val prevBlock = getBlockLines(text, prevLineNumber)
        val canMove = currentBlock.isNotEmpty() && currentLineNumber != 0 && prevBlock.isNotEmpty()

        return if (!canMove) {
            value
        } else {
            val mutableLines = lines.toMutableList()
            mutableLines.subList(prevLineNumber, currentLineNumber + currentBlock.size - 1).clear()
            mutableLines.addAll(prevLineNumber, currentBlock)
            mutableLines.addAll(prevLineNumber + currentBlock.size, prevBlock)

            val newText = mutableLines.joinToString("\n")
            val newCursorPosition = cursorPosition - prevBlock.joinToString("\n").length - 1
            value.copy(text = newText, selection = TextRange(newCursorPosition.coerceIn(0, newText.length)))
        }
    }

    fun moveBlockDown(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val lines = text.lines()
        val currentLineNumber = text.substring(0, cursorPosition).count { it == '\n' }

        val currentBlock = getBlockLines(text, currentLineNumber)
        val nextBlockStartLine = currentLineNumber + currentBlock.size
        val nextBlock = getBlockLines(text, nextBlockStartLine)
        val canMove =
            currentBlock.isNotEmpty() &&
                (currentLineNumber + currentBlock.size) < lines.size &&
                nextBlock.isNotEmpty()

        return if (!canMove) {
            value
        } else {
            val mutableLines = lines.toMutableList()
            mutableLines.subList(currentLineNumber, nextBlockStartLine + nextBlock.size).clear()
            mutableLines.addAll(currentLineNumber, nextBlock)
            mutableLines.addAll(currentLineNumber + nextBlock.size, currentBlock)

            val newText = mutableLines.joinToString("\n")
            val newCursorPosition = cursorPosition + nextBlock.joinToString("\n").length + 1
            value.copy(text = newText, selection = TextRange(newCursorPosition.coerceIn(0, newText.length)))
        }
    }

    // Clipboard operations

    fun deleteLine(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val newText = text.removeRange(lineStart, if (lineEnd < text.length) lineEnd + 1 else lineEnd)
        val newCursorPosition = lineStart.coerceAtMost(newText.length)
        return value.copy(text = newText, selection = TextRange(newCursorPosition))
    }

    fun pasteLine(
        value: TextFieldValue,
        clipboardText: String,
    ): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        val trimmedClipboard = clipboardText.trim()
        val currentLineHasMarker = currentLine.trim().startsWith("- ")
        val clipboardHasMarker = trimmedClipboard.startsWith("- ")

        val textToInsert =
            if (currentLineHasMarker && clipboardHasMarker) {
                trimmedClipboard.replaceFirst(Regex("^\\s*- "), "")
            } else {
                clipboardText
            }

        val newText = text.replaceRange(lineStart, lineEnd, textToInsert)
        val newCursorPosition = lineStart + textToInsert.length

        return value.copy(text = newText, selection = TextRange(newCursorPosition))
    }
}
