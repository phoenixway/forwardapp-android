package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.customlist.components.ListFormatMode
import com.romankozak.forwardappmobile.ui.screens.customlist.components.ListToolbarState
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
)

@HiltViewModel
class UnifiedCustomListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

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
            _uiState.update { it.copy(isNewList = true, content = TextFieldValue("• ", selection = TextRange(2)))
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, isNewList = false) }
                projectRepository.getCustomListById(listId)?.let { list ->
                    val content = TextFieldValue(list.content ?: "")
                    _uiState.update {
                        it.copy(
                            title = list.name,
                            content = content,
                            isLoading = false,
                            toolbarState = computeToolbarState(content)
                        )
                    }
                    pushUndo(content)
                } ?: _events.send(UnifiedCustomListEvent.NavigateBack)
            }
        }
    }

    fun onToggleEditMode(isEditing: Boolean) {
        _uiState.update { it.copy(isEditing = isEditing) }
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
                _events.send(UnifiedCustomListEvent.ShowError(e.message ?: "An unknown error occurred"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChange(newValue: TextFieldValue) {
        pushUndo(_uiState.value.content)
        clearRedo()
        _uiState.update { it.copy(content = newValue, toolbarState = computeToolbarState(newValue)) }
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
            val newText = text.substring(0, selectionStart) + insert + text.substring(selectionStart)
            val newSelection = TextRange(selectionStart + insert.length)
            _uiState.update { it.copy(content = TextFieldValue(newText, newSelection), toolbarState = computeToolbarState(TextFieldValue(newText, newSelection))) }
        } else {
            _uiState.update { it.copy(content = newValue, toolbarState = computeToolbarState(newValue)) }
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
        val newText = cur.text.substring(0, lineStart) + "    " + cur.text.substring(lineStart)
        val newSelection = shiftSelectionAfterInsert(cur, lineStart, 4)
        onContentChange(TextFieldValue(newText, selection = newSelection))
    }

    fun onDeIndentLine() {
        val cur = _uiState.value.content
        val (lineStart, _) = findLineStartAndIndex(cur.text, cur.selection.start)
        val removeCount = if (cur.text.substring(lineStart).startsWith("    ")) 4 else 0
        if (removeCount > 0) {
            val newText = cur.text.removeRange(lineStart, lineStart + removeCount)
            val newSelection = shiftSelectionAfterRemove(cur, lineStart, removeCount)
            onContentChange(TextFieldValue(newText, selection = newSelection))
        }
    }

    fun onIndentBlock() { onIndentLine() }
    fun onDeIndentBlock() { onDeIndentLine() }

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

    fun onMoveBlockUp() { onMoveLineUp() }
    fun onMoveBlockDown() { onMoveLineDown() }

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
        if (lineIndex < lines.size) clipboard = lines[lineIndex]
    }

    fun onCutLine() {
        onCopyLine()
        onDeleteLine()
    }

    fun onPasteLine() {
        if (clipboard.isEmpty()) return
        val cur = _uiState.value.content
        val (lineStart, _) = findLineStartAndIndex(cur.text, cur.selection.start)
        val insertion = clipboard
        val newText = cur.text.substring(0, lineStart) + insertion + "\n" + cur.text.substring(lineStart)
        val newSel = TextRange(lineStart + insertion.length + 1)
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

    fun onToggleNumbered() { /* ... */ }
    fun onToggleChecklist() { /* ... */ }

    fun onUndo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(_uiState.value.content)
        _uiState.update { it.copy(content = prev, toolbarState = computeToolbarState(prev)) }
    }

    fun onRedo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_uiState.value.content)
        _uiState.update { it.copy(content = next, toolbarState = computeToolbarState(next)) }
    }

    private fun pushUndo(value: TextFieldValue) {
        if (undoStack.lastOrNull() == value) return
        undoStack.addLast(value)
        if (undoStack.size > 100) undoStack.removeFirst()
    }

    private fun clearRedo() {
        redoStack.clear()
    }

    private fun computeToolbarState(content: TextFieldValue): ListToolbarState {
        val text = content.text
        val cursorPosition = content.selection.start
        val (canIndent, canDeIndent, canMoveUp, canMoveDown) = calculateCapabilities(text, cursorPosition)
        return ListToolbarState(
            totalItems = text.lines().count { it.isNotBlank() },
            isEditing = uiState.value.isEditing,
            formatMode = detectFormatMode(text.lines()),
            hasSelection = content.selection.length > 0,
            canIndent = canIndent,
            canDeIndent = canDeIndent,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown
        )
    }

    private fun findLineStartAndIndex(fullText: String, cursorPos: Int, preferPrevious: Boolean = false): Pair<Int, Int> {
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
    val numberRegex = Regex("^(\\d+)\\.")

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


    private fun shiftSelectionAfterInsert(old: TextFieldValue, insertPos: Int, insertLen: Int): TextRange {
        val oldSel = old.selection
        val newStart = if (oldSel.start >= insertPos) oldSel.start + insertLen else oldSel.start
        val newEnd = if (oldSel.end >= insertPos) oldSel.end + insertLen else oldSel.end
        return TextRange(newStart, newEnd)
    }

    private fun shiftSelectionAfterRemove(old: TextFieldValue, removePos: Int, removeLen: Int): TextRange {
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
        val canDeIndent = lines.getOrNull(lineIndex)?.startsWith("    ") ?: false
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
        trimmed.matches(Regex("^\\d+\\.\\s.*"))
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