package com.romankozak.forwardappmobile.ui.common.editor

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.ui.screens.customlist.components.ListToolbarState
import kotlin.math.min
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UniversalEditorEvent {
  data class ShowError(val message: String) : UniversalEditorEvent()

  data class ShareContent(val content: String) : UniversalEditorEvent()

  data class ShowLocation(val projectId: String) : UniversalEditorEvent() // НОВИЙ EVENT
}

data class UniversalEditorUiState(
  val content: TextFieldValue = TextFieldValue(""),
  val toolbarState: ListToolbarState = ListToolbarState(),
  val isLoading: Boolean = false,
  val projectId: String? = null,
)

class UniversalEditorViewModel(private val application: Application) : ViewModel() {

  companion object {
    private const val INDENT_STRING = "      " // 6 spaces
    private const val INDENT_LENGTH = 6
  }

  private val _uiState = MutableStateFlow(UniversalEditorUiState())
  val uiState: StateFlow<UniversalEditorUiState> = _uiState.asStateFlow()

  private val _events = Channel<UniversalEditorEvent>()
  val events = _events.receiveAsFlow()

  private val undoStack = ArrayDeque<TextFieldValue>()
  private val redoStack = ArrayDeque<TextFieldValue>()

  private var clipboard: String = ""

  fun setProjectId(projectId: String?) {
    _uiState.update { it.copy(projectId = projectId) }
  }

  fun setInitialContent(content: String) {
    val textFieldValue = TextFieldValue(content)
    _uiState.update {
      it.copy(content = textFieldValue, toolbarState = computeToolbarState(textFieldValue, true))
    }
    pushUndo(textFieldValue)
  }

  fun onContentChange(newValue: TextFieldValue) {
    val oldValue = _uiState.value.content
    val oldText = oldValue.text
    val newText = newValue.text
    val oldSelection = oldValue.selection

    if (newText.length > oldText.length && oldSelection.collapsed) {
      val inserted =
        newText.substring(
          oldSelection.start,
          oldSelection.start + (newText.length - oldText.length),
        )
      if (inserted == "\n") {
        onEnter(newValue)
        return
      }
    }

    pushUndo(oldValue)
    clearRedo()
    _uiState.update {
      it.copy(content = newValue, toolbarState = computeToolbarState(newValue, true))
    }
  }

  fun onEnter(newValue: TextFieldValue) {
    val oldContent = _uiState.value.content
    pushUndo(oldContent)
    clearRedo()

    val text = newValue.text
    val selectionStart = newValue.selection.start

    val prevLineEnd = text.lastIndexOf('\n', startIndex = selectionStart - 2)
    val prevLineStart = if (prevLineEnd == -1) 0 else prevLineEnd + 1
    val prevLine = text.substring(prevLineStart, selectionStart - 1)

    val marker = detectListMarker(prevLine)
    val indent = prevLine.takeWhile { it.isWhitespace() }
    val insert =
      if (marker.isNotEmpty()) {
        indent + marker + " "
      } else {
        indent
      }

    if (insert.isNotEmpty()) {
      val newText = text.substring(0, selectionStart) + insert + text.substring(selectionStart)
      val newSelection = TextRange(selectionStart + insert.length)
      _uiState.update {
        it.copy(
          content = TextFieldValue(newText, newSelection),
          toolbarState = computeToolbarState(TextFieldValue(newText, newSelection), true),
        )
      }
    } else {
      _uiState.update {
        it.copy(content = newValue, toolbarState = computeToolbarState(newValue, true))
      }
    }
  }

  fun onIndentLine() {
    val cur = _uiState.value.content
    val (lineStart, _) = findLineStartAndIndex(cur.text, cur.selection.start)
    val newText = cur.text.substring(0, lineStart) + INDENT_STRING + cur.text.substring(lineStart)
    val newSelection = shiftSelectionAfterInsert(cur, lineStart, INDENT_LENGTH)
    onContentChange(TextFieldValue(newText, selection = newSelection))
  }

  fun onDeIndentLine() {
    val cur = _uiState.value.content
    val (lineStart, _) = findLineStartAndIndex(cur.text, cur.selection.start)
    val removeCount =
      if (cur.text.substring(lineStart).startsWith(INDENT_STRING)) INDENT_LENGTH else 0
    if (removeCount > 0) {
      val newText = cur.text.removeRange(lineStart, lineStart + removeCount)
      val newSelection = shiftSelectionAfterRemove(cur, lineStart, removeCount)
      onContentChange(TextFieldValue(newText, selection = newSelection))
    }
  }

  fun onIndentBlock() {
    onIndentLine()
  }

  fun onDeIndentBlock() {
    onDeIndentLine()
  }

  fun onMoveLineUp() {
    val cur = _uiState.value.content
    val (_, lineIndex) = findLineStartAndIndex(cur.text, cur.selection.start)
    if (lineIndex <= 0) return
    val lines = cur.text.lines().toMutableList()
    val removed = lines.removeAt(lineIndex)
    lines.add(lineIndex - 1, removed)
    val newText = lines.joinToString("\n")
    val newSel = computeSelectionForMovedLine(lines, lineIndex - 1)
    onContentChange(TextFieldValue(newText, selection = newSel))
  }

  fun onMoveLineDown() {
    val cur = _uiState.value.content
    val linesList = cur.text.lines().toMutableList()
    val (_, lineIndex) = findLineStartAndIndex(cur.text, cur.selection.start)
    if (lineIndex >= linesList.size - 1) return
    val removed = linesList.removeAt(lineIndex)
    linesList.add(lineIndex + 1, removed)
    val newText = linesList.joinToString("\n")
    val newSel = computeSelectionForMovedLine(linesList, lineIndex + 1)
    onContentChange(TextFieldValue(newText, selection = newSel))
  }

  fun onMoveBlockUp() {
    onMoveLineUp()
  }

  fun onMoveBlockDown() {
    onMoveLineDown()
  }

  fun onDeleteLine() {
    val cur = _uiState.value.content
    val (lineStart, lineIndex) = findLineStartAndIndex(cur.text, cur.selection.start)
    val lines = cur.text.lines().toMutableList()
    if (lines.isEmpty() || lineIndex >= lines.size) return
    lines.removeAt(lineIndex)
    val newText = lines.joinToString("\n")
    val sel = TextRange(min(lineStart, newText.length))
    onContentChange(TextFieldValue(newText, selection = sel))
  }

  fun onCopyLine() {
    val cur = _uiState.value.content
    val (_, lineIndex) = findLineStartAndIndex(cur.text, cur.selection.start)
    val lines = cur.text.lines()
    if (lineIndex < lines.size) {
      val lineToCopy = lines[lineIndex]
      clipboard = lineToCopy
      val clipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("line", lineToCopy)
      clipboardManager.setPrimaryClip(clip)
    }
  }

  fun onCutLine() {
    onCopyLine()
    onDeleteLine()
  }

  fun onCopyAll() {
    val textToCopy = _uiState.value.content.text
    if (textToCopy.isNotEmpty()) {
      val clipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("editor_content", textToCopy)
      clipboardManager.setPrimaryClip(clip)
    }
  }

  fun onShare() {
    val textToShare = _uiState.value.content.text
    if (textToShare.isNotEmpty()) {
      viewModelScope.launch { _events.send(UniversalEditorEvent.ShareContent(textToShare)) }
    }
  }

  fun onPasteLine() {
    val clipboardManager =
      application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboardManager.primaryClip
    val pasteText =
      if (clipData != null && clipData.itemCount > 0) {
        clipData.getItemAt(0)?.text?.toString()
      } else {
        null
      }

    val textToPaste = pasteText ?: clipboard // Fallback to internal clipboard
    if (textToPaste.isEmpty()) return

    val cur = _uiState.value.content
    val selectionStart = cur.selection.start
    val selectionEnd = cur.selection.end
    val newText = cur.text.replaceRange(selectionStart, selectionEnd, textToPaste)
    val newSel = TextRange(selectionStart + textToPaste.length)

    onContentChange(TextFieldValue(newText, selection = newSel))
  }

  fun onToggleBullet() {
    val cur = _uiState.value.content
    val (lineStart, lineIndex) = findLineStartAndIndex(cur.text, cur.selection.start)
    val lines = cur.text.lines().toMutableList()
    if (lineIndex >= lines.size) return

    val line = lines[lineIndex]
    val originalIndent = line.takeWhile { it.isWhitespace() }
    val trimmedLine = line.trimStart()

    // Check if the line is already a list item
    val markerInfo = getMarkerInfo(trimmedLine)
    if (markerInfo != null) {
      // Already a list item, so remove the marker
      val (marker, content) = markerInfo
      val newText = originalIndent + content
      lines[lineIndex] = newText
      val selectionOffset = -(marker.length + 1) // marker + space
      val newSelectionStart =
        (cur.selection.start + selectionOffset).coerceAtLeast(lineStart + originalIndent.length)
      val newSelectionEnd =
        (cur.selection.end + selectionOffset).coerceAtLeast(lineStart + originalIndent.length)
      onContentChange(
        TextFieldValue(lines.joinToString("\n"), TextRange(newSelectionStart, newSelectionEnd))
      )
      return
    }

    // Not a list item, so add a marker based on previous line
    var indent = originalIndent
    var marker = "-"

    if (lineIndex > 0) {
      val prevLine = lines[lineIndex - 1]
      val prevLineIndent = prevLine.takeWhile { it.isWhitespace() }
      val prevLineTrimmed = prevLine.trimStart()
      val prevMarkerInfo = getMarkerInfo(prevLineTrimmed)
      if (prevMarkerInfo != null) {
        indent = prevLineIndent
        val prevMarker = prevMarkerInfo.first
        val numRegex = Regex("^(\\d+)\\.")
        val match = numRegex.find(prevMarker)
        if (match != null) {
          val num = match.groupValues[1].toInt()
          marker = "${num + 1}."
        } else {
          marker = prevMarker
        }
      }
    }

    val newText = indent + "$marker " + trimmedLine
    lines[lineIndex] = newText
    val selectionOffset = (indent.length + marker.length + 1) - originalIndent.length
    val newSelectionStart =
      (cur.selection.start + selectionOffset).coerceAtLeast(lineStart + indent.length)
    val newSelectionEnd =
      (cur.selection.end + selectionOffset).coerceAtLeast(lineStart + indent.length)
    onContentChange(
      TextFieldValue(lines.joinToString("\n"), TextRange(newSelectionStart, newSelectionEnd))
    )
  }

  // Helper function to get marker info
  private fun getMarkerInfo(line: String): Pair<String, String>? {
    val patterns =
      listOf(
        Regex("^(- )(.*)"),
        Regex("^(\\d+\\. )(.*)"),
        Regex("^([•] )(.*)"),
        Regex("^([☐] )(.*)"),
        Regex("^([☑] )(.*)"),
      )
    for (pattern in patterns) {
      val match = pattern.find(line)
      if (match != null) {
        return Pair(match.groupValues[1].trim(), match.groupValues[2])
      }
    }
    return null
  }

  fun onUndo() {
    if (undoStack.isEmpty()) return
    val prev = undoStack.removeLast()
    redoStack.addLast(_uiState.value.content)
    _uiState.update { it.copy(content = prev, toolbarState = computeToolbarState(prev, true)) }
  }

  fun onRedo() {
    if (redoStack.isEmpty()) return
    val next = redoStack.removeLast()
    undoStack.addLast(_uiState.value.content)
    _uiState.update { it.copy(content = next, toolbarState = computeToolbarState(next, true)) }
  }

  private fun pushUndo(value: TextFieldValue) {
    if (undoStack.lastOrNull() == value) return
    undoStack.addLast(value)
    if (undoStack.size > 100) undoStack.removeFirst()
  }

  private fun clearRedo() {
    redoStack.clear()
  }

  private fun computeToolbarState(content: TextFieldValue, isEditing: Boolean): ListToolbarState {
    val text = content.text
    val cursorPosition = content.selection.start

    val (canIndent, canDeIndent, canMoveUp, canMoveDown) =
      calculateCapabilities(text, cursorPosition)

    return ListToolbarState(
      totalItems = text.lines().count { it.isNotBlank() },
      isEditing = isEditing, // Тепер ми беремо значення з параметра функції
      hasSelection = content.selection.length > 0,
      canIndent = canIndent,
      canDeIndent = canDeIndent,
      canMoveUp = canMoveUp,
      canMoveDown = canMoveDown,
      canUndo = undoStack.isNotEmpty(),
      canRedo = redoStack.isNotEmpty(),
    )
  }

  private fun findLineStartAndIndex(
    fullText: String,
    cursorPos: Int,
    preferPrevious: Boolean = false,
  ): Pair<Int, Int> {
    val corrected = cursorPos.coerceIn(0, fullText.length)
    var before = fullText.substring(0, corrected)
    if (preferPrevious && before.endsWith("\n")) {
      before = before.dropLast(1)
    }
    val linesBefore = before.lines()
    val lineIndex = (linesBefore.size - 1).coerceAtLeast(0)
    val lineStart =
      linesBefore.dropLast(1).joinToString("\n").let { if (it.isEmpty()) 0 else it.length + 1 }
    return Pair(lineStart, lineIndex)
  }

  private fun detectListMarker(linePrefix: String): String {
    val trimmed = linePrefix.trimStart()
    val numberRegex = Regex("^(\\d+)\\. ")

    return when {
      trimmed.startsWith("-") -> "-"
      trimmed.startsWith("•") -> "•"
      trimmed.startsWith("☐") -> "☐"
      trimmed.startsWith("☑") -> "☑"
      numberRegex.containsMatchIn(trimmed) -> {
        val match = numberRegex.find(trimmed)
        val num = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        "${num + 1}."
      }
      else -> ""
    }
  }

  private fun shiftSelectionAfterInsert(
    old: TextFieldValue,
    insertPos: Int,
    insertLen: Int,
  ): TextRange {
    val oldSel = old.selection
    val newStart = if (oldSel.start >= insertPos) oldSel.start + insertLen else oldSel.start
    val newEnd = if (oldSel.end >= insertPos) oldSel.end + insertLen else oldSel.end
    return TextRange(newStart, newEnd)
  }

  private fun shiftSelectionAfterRemove(
    old: TextFieldValue,
    removePos: Int,
    removeLen: Int,
  ): TextRange {
    val oldSel = old.selection
    val newStart =
      when {
        oldSel.start <= removePos -> oldSel.start
        oldSel.start in (removePos + 1)..(removePos + removeLen) -> removePos
        else -> oldSel.start - removeLen
      }
    val newEnd =
      when {
        oldSel.end <= removePos -> oldSel.end
        oldSel.end in (removePos + 1)..(removePos + removeLen) -> removePos
        else -> oldSel.end - removeLen
      }
    return TextRange(newStart.coerceAtLeast(0), newEnd.coerceAtLeast(0))
  }

  private fun computeSelectionForMovedLine(lines: List<String>, targetIndex: Int): TextRange {
    val prefixLen = lines.take(targetIndex).sumOf { it.length + 1 }
    return TextRange(prefixLen.coerceAtMost(lines.joinToString("\n").length))
  }

  private fun calculateCapabilities(text: String, cursorPosition: Int): List<Boolean> {
    val (_, lineIndex) = findLineStartAndIndex(text, cursorPosition)
    val lines = text.lines()

    val canIndent = true
    val canDeIndent = lines.getOrNull(lineIndex)?.startsWith(INDENT_STRING) ?: false
    val canMoveUp = lineIndex > 0
    val canMoveDown = lineIndex < lines.size - 1

    return listOf(canIndent, canDeIndent, canMoveUp, canMoveDown)
  }

  fun onShowLocation() {
    val projectId = _uiState.value.projectId
    if (projectId != null) {
      viewModelScope.launch { _events.send(UniversalEditorEvent.ShowLocation(projectId)) }
    }
  }
}
