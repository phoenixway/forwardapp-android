package com.romankozak.forwardappmobile.ui.screens.customlist

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.components.ListFormatMode
import com.romankozak.forwardappmobile.ui.common.editor.components.ListToolbarState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

sealed class UnifiedCustomListEvent {
    object NavigateBack : UnifiedCustomListEvent()
    data class ShowError(val message: String) : UnifiedCustomListEvent()
    data class ShowSuccess(val message: String = "") : UnifiedCustomListEvent()
    object AutoSaved : UnifiedCustomListEvent()
}

data class UnifiedCustomListUiState(
    val title: String = "",
    val content: TextFieldValue = TextFieldValue(""),
    val collapsedLines: Set<Int> = emptySet(),
    val toolbarState: ListToolbarState = ListToolbarState(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isNewList: Boolean = true,
    val currentLine: Int? = null,
)

@HiltViewModel
class UnifiedCustomListViewModel @Inject constructor(
    private val application: Application,
    private val projectRepository: ProjectRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val INDENT_STRING = "      " // 6 spaces
        private const val INDENT_LENGTH = 6
    }

    private val _uiState = MutableStateFlow(UnifiedCustomListUiState())
    val uiState: StateFlow<UnifiedCustomListUiState> = _uiState.asStateFlow()

    private val _events = Channel<UnifiedCustomListEvent>()
    val events = _events.receiveAsFlow()

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()

    private var clipboard: String = ""

    val isNewList: Boolean get() = _uiState.value.isNewList

    init {
        val listId: String? = savedStateHandle["listId"]
        if (listId == null) {
            _uiState.update {
                it.copy(isNewList = true, content = TextFieldValue("• ", selection = TextRange(2)))
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, isNewList = false) }
                projectRepository.getCustomListById(listId)?.let { list ->
                    projectRepository.updateCustomList(list.copy(updatedAt = System.currentTimeMillis()))
                    val content = TextFieldValue(list.content ?: "")
                    _uiState.update {
                        it.copy(
                            title = list.name,
                            content = content,
                            isLoading = false,
                            toolbarState = computeToolbarState(content, false)
                        )
                    }
                    pushUndo(content)
                } ?: _events.send(UnifiedCustomListEvent.NavigateBack)
            }
        }
    }

    fun onToggleEditMode(isEditing: Boolean) {
        _uiState.update {
            it.copy(
                isEditing = isEditing,
                // Явно передаємо нове значення 'isEditing' в функцію
                toolbarState = computeToolbarState(it.content, isEditing = isEditing)
            )
        }
    }

    fun onSave() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentState = _uiState.value
            val listId: String? = savedStateHandle["listId"]
            val projectId: String? = savedStateHandle["projectId"]

            try {
                if (listId == null) {
                    projectId?.let {
                        projectRepository.createCustomList(
                            name = currentState.title.ifBlank { "New List" },
                            content = currentState.content.text,
                            projectId = it
                        )
                        _events.send(UnifiedCustomListEvent.NavigateBack)
                    } ?: _events.send(UnifiedCustomListEvent.ShowError("Project ID is missing"))
                } else {
                    projectRepository.getCustomListById(listId)?.let { existingList ->
                        val updatedList = existingList.copy(
                            name = currentState.title,
                            content = currentState.content.text
                        )
                        projectRepository.updateCustomList(updatedList)
                        _events.send(UnifiedCustomListEvent.ShowSuccess("Saved"))
                    } ?: _events.send(UnifiedCustomListEvent.ShowError("List not found"))
                }
            } catch (e: Exception) {
                _events.send(
                    UnifiedCustomListEvent.ShowError(
                        e.message ?: "An unknown error occurred"
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

// CustomListViewModel.kt

    fun onContentChange(newValue: TextFieldValue) {
        pushUndo(_uiState.value.content)
        clearRedo()
        _uiState.update {
            it.copy(
                content = newValue,
                toolbarState = computeToolbarState(newValue, isEditing = it.isEditing)
            )
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
        val insert = if (marker.isNotEmpty()) "$marker " else indent

        if (insert.isNotEmpty()) {
            val newText =
                text.substring(0, selectionStart) + insert + text.substring(selectionStart)
            val newSelection = TextRange(selectionStart + insert.length)
            _uiState.update {
                it.copy(
                    content = TextFieldValue(newText, newSelection),
                    toolbarState = computeToolbarState(
                        TextFieldValue(newText, newSelection),
                        it.isEditing
                    )
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    content = newValue,
                    toolbarState = computeToolbarState(newValue, it.isEditing)
                )
            }
        }
    }

    fun onToggleFold(lineIndex: Int) {
        _uiState.update {
            val set = it.collapsedLines.toMutableSet()
            if (set.contains(lineIndex)) set.remove(lineIndex) else set.add(lineIndex)
            it.copy(collapsedLines = set)
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
        val removeCount = if (cur.text.substring(lineStart).startsWith(INDENT_STRING)) INDENT_LENGTH else 0
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
            val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("line", lineToCopy)
            clipboardManager.setPrimaryClip(clip)
        }
    }

    fun onCutLine() {
        onCopyLine()
        onDeleteLine()
    }

    fun onPasteLine() {
        val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip
        val pasteText = if (clipData != null && clipData.itemCount > 0) {
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
        val indent = line.takeWhile { it.isWhitespace() }
        val trimmedLine = line.trimStart()

        val newText: String
        if (trimmedLine.startsWith("• ")) {
            newText = indent + trimmedLine.removePrefix("• ")
        } else {
            newText = indent + "• " + trimmedLine
        }
        lines[lineIndex] = newText

        val newContent = lines.joinToString("\n")
        onContentChange(TextFieldValue(newContent, cur.selection))
    }

    fun onToggleNumbered() { /* ... */
    }

    fun onToggleChecklist() { /* ... */
    }

    fun onUndo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(_uiState.value.content)
        _uiState.update {
            it.copy(
                content = prev,
                toolbarState = computeToolbarState(prev, it.isEditing)
            )
        }
    }

    fun onRedo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_uiState.value.content)
        _uiState.update {
            it.copy(
                content = next,
                toolbarState = computeToolbarState(next, it.isEditing)
            )
        }
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
        val (_, lineIndex) = findLineStartAndIndex(text, cursorPosition)
        _uiState.update { it.copy(currentLine = lineIndex) }

        val (canIndent, canDeIndent, canMoveUp, canMoveDown) = calculateCapabilities(
            text,
            cursorPosition
        )

        return ListToolbarState(
            totalItems = text.lines().count { it.isNotBlank() },
            isEditing = isEditing, // Тепер ми беремо значення з параметра функції
            formatMode = detectFormatMode(text.lines()),
            hasSelection = content.selection.length > 0,
            canIndent = canIndent,
            canDeIndent = canDeIndent,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }


    private fun findLineStartAndIndex(
        fullText: String,
        cursorPos: Int,
        preferPrevious: Boolean = false
    ): Pair<Int, Int> {
        val corrected = cursorPos.coerceIn(0, fullText.length)
        var before = fullText.substring(0, corrected)
        if (preferPrevious && before.endsWith("\n")) {
            before = before.dropLast(1)
        }
        val linesBefore = before.lines()
        val lineIndex = (linesBefore.size - 1).coerceAtLeast(0)
        val lineStart = linesBefore.dropLast(1).joinToString("\n").let {
            if (it.isEmpty()) 0 else it.length + 1
        }
        return Pair(lineStart, lineIndex)
    }

   private fun detectListMarker(linePrefix: String): String {
    val trimmed = linePrefix.trimStart()
    val numberRegex = Regex("""^(\d+)\.""")

    return when {
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
        insertLen: Int
    ): TextRange {
        val oldSel = old.selection
        val newStart = if (oldSel.start >= insertPos) oldSel.start + insertLen else oldSel.start
        val newEnd = if (oldSel.end >= insertPos) oldSel.end + insertLen else oldSel.end
        return TextRange(newStart, newEnd)
    }

    private fun shiftSelectionAfterRemove(
        old: TextFieldValue,
        removePos: Int,
        removeLen: Int
    ): TextRange {
        val oldSel = old.selection
        val newStart = when {
            oldSel.start <= removePos -> oldSel.start
            oldSel.start in (removePos + 1)..(removePos + removeLen) -> removePos
            else -> oldSel.start - removeLen
        }
        val newEnd = when {
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

    private fun scheduleAutoSave() {}

    private fun calculateCapabilities(text: String, cursorPosition: Int): List<Boolean> {
        val (_, lineIndex) = findLineStartAndIndex(text, cursorPosition)
        val lines = text.lines()

        val canIndent = true
        val canDeIndent = lines.getOrNull(lineIndex)?.startsWith(INDENT_STRING) ?: false
        val canMoveUp = lineIndex > 0
        val canMoveDown = lineIndex < lines.size - 1

        return listOf(canIndent, canDeIndent, canMoveUp, canMoveDown)
    }

private fun detectFormatMode(lines: List<String>): ListFormatMode {
    val nonEmptyLines = lines.filter { it.isNotBlank() }
    if (nonEmptyLines.isEmpty()) return ListFormatMode.BULLET

    val bulletCount = nonEmptyLines.count { it.trimStart().startsWith("•") }
    val numberedCount = nonEmptyLines.count { line ->
        val trimmed = line.trimStart()
        trimmed.matches(Regex("""^\d+\.\s.*"""))
    }
    val checklistCount = nonEmptyLines.count { line ->
        val trimmed = line.trimStart()
        trimmed.startsWith("☐") || trimmed.startsWith("☑")
    }

    return when {
        checklistCount > nonEmptyLines.size / 2 -> ListFormatMode.CHECKLIST
        numberedCount > nonEmptyLines.size / 2 -> ListFormatMode.NUMBERED
        bulletCount > nonEmptyLines.size / 2 -> ListFormatMode.BULLET
        else -> ListFormatMode.PLAIN
    }
}


}
