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
        return applyToBlock(value) { line -> "    " + line }
    }

    fun outdentBlock(value: TextFieldValue): TextFieldValue {
        return applyToBlock(value) { line ->
            if (line.startsWith("    ")) line.substring(4) else line
        }
    }

    private fun applyToBlock(value: TextFieldValue, transform: (String) -> String): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val lines = text.lines().toMutableList()
        val currentLineNumber = text.substring(0, cursorPosition).count { it == '\n' }

        val blockLines = getBlockLines(text, currentLineNumber)
        if (blockLines.isEmpty()) return value

        val transformedBlock = blockLines.map(transform)

        val blockStartLine = currentLineNumber

        for (i in 0 until blockLines.size) {
            lines[blockStartLine + i] = transformedBlock[i]
        }

        val newText = lines.joinToString("\n")
        // This is a simplification; cursor position might not be perfectly preserved
        val newCursorPosition = cursorPosition + (transformedBlock.first().length - blockLines.first().length)

        return value.copy(text = newText, selection = TextRange(newCursorPosition.coerceIn(0, newText.length)))
    }

    fun moveBlockUp(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val lines = text.lines()
        val currentLineNumber = text.substring(0, cursorPosition).count { it == '\n' }

        val currentBlock = getBlockLines(text, currentLineNumber)
        if (currentBlock.isEmpty() || currentLineNumber == 0) return value

        val prevLineNumber = (currentLineNumber - 1).coerceAtLeast(0)
        val prevBlock = getBlockLines(text, prevLineNumber)
        if (prevBlock.isEmpty()) return value
        
        val mutableLines = lines.toMutableList()
        mutableLines.subList(prevLineNumber, currentLineNumber + currentBlock.size -1).clear()
        mutableLines.addAll(prevLineNumber, currentBlock)
        mutableLines.addAll(prevLineNumber + currentBlock.size, prevBlock)

        val newText = mutableLines.joinToString("\n")
        val newCursorPosition = cursorPosition - prevBlock.joinToString("\n").length - 1

        return value.copy(text = newText, selection = TextRange(newCursorPosition.coerceIn(0, newText.length)))
    }

    fun moveBlockDown(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val lines = text.lines()
        val currentLineNumber = text.substring(0, cursorPosition).count { it == '\n' }

        val currentBlock = getBlockLines(text, currentLineNumber)
        if (currentBlock.isEmpty() || (currentLineNumber + currentBlock.size) >= lines.size) return value

        val nextBlockStartLine = currentLineNumber + currentBlock.size
        val nextBlock = getBlockLines(text, nextBlockStartLine)
        if (nextBlock.isEmpty()) return value

        val mutableLines = lines.toMutableList()
        mutableLines.subList(currentLineNumber, nextBlockStartLine + nextBlock.size).clear()
        mutableLines.addAll(currentLineNumber, nextBlock)
        mutableLines.addAll(currentLineNumber + nextBlock.size, currentBlock)

        val newText = mutableLines.joinToString("\n")
        val newCursorPosition = cursorPosition + nextBlock.joinToString("\n").length + 1

        return value.copy(text = newText, selection = TextRange(newCursorPosition.coerceIn(0, newText.length)))
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

    fun getLineForClipboard(value: TextFieldValue): String {
        val (lineStart, lineEnd) = getCurrentLineBounds(value.text, value.selection.start)
        return value.text.substring(lineStart, lineEnd)
    }

    fun pasteLine(value: TextFieldValue, clipboardText: String): TextFieldValue {
        val text = value.text
        val cursorPosition = value.selection.start
        val (lineStart, lineEnd) = getCurrentLineBounds(text, cursorPosition)
        val currentLine = text.substring(lineStart, lineEnd)

        val trimmedClipboard = clipboardText.trim()
        val currentLineHasMarker = currentLine.trim().startsWith("- ")
        val clipboardHasMarker = trimmedClipboard.startsWith("- ")

        val textToInsert = if (currentLineHasMarker && clipboardHasMarker) {
            trimmedClipboard.replaceFirst(Regex("^\\s*- "), "")
        } else {
            clipboardText
        }

        val newText = text.replaceRange(lineStart, lineEnd, textToInsert)
        val newCursorPosition = lineStart + textToInsert.length

        return value.copy(text = newText, selection = TextRange(newCursorPosition))
    }
}
