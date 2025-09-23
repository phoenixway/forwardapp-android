package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.CustomListEntity
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.customlist.components.ListFormatMode
import com.romankozak.forwardappmobile.ui.screens.customlist.components.ListToolbarState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class UnifiedCustomListUiState(
  val title: String = "",
  val content: TextFieldValue = TextFieldValue(""),
  val isExistingList: Boolean = false,
  val isLoading: Boolean = false,
  val error: String? = null,
  val isSaveEnabled: Boolean = false,
  val list: CustomListEntity? = null,
  val toolbarState: ListToolbarState = ListToolbarState(),
  val collapsedLines: Set<Int> = emptySet(),
  val undoStack: List<TextFieldValue> = emptyList(),
  val redoStack: List<TextFieldValue> = emptyList(),
  val autoSaveEnabled: Boolean = true,
)

sealed class UnifiedCustomListEvent {
  data class NavigateBack(val message: String? = null) : UnifiedCustomListEvent()

  data class ShowError(val message: String) : UnifiedCustomListEvent()

  data class ShowSuccess(val message: String) : UnifiedCustomListEvent()

  object AutoSaved : UnifiedCustomListEvent()
}

@HiltViewModel
class UnifiedCustomListViewModel
@Inject
constructor(private val projectRepository: ProjectRepository, savedStateHandle: SavedStateHandle) :
  ViewModel() {

  private val _uiState = MutableStateFlow(UnifiedCustomListUiState())
  val uiState = _uiState.asStateFlow()

  private val _events = Channel<UnifiedCustomListEvent>()
  val events = _events.receiveAsFlow()

  val listId: String? = savedStateHandle["listId"]
  val isNewList: Boolean = listId == null
  private val projectId: String? = savedStateHandle["projectId"]

  private var autoSaveJob: kotlinx.coroutines.Job? = null

  init {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        if (listId != null) {
          val list = projectRepository.getCustomListById(listId)
          if (list != null) {
            val content = TextFieldValue(list.content ?: "")
            _uiState.value =
              UnifiedCustomListUiState(
                title = list.name,
                content = content,
                isExistingList = true,
                isLoading = false,
                isSaveEnabled = true,
                list = list,
                toolbarState = createToolbarState(content, false),
                undoStack = listOf(content),
              )
          } else {
            _events.send(UnifiedCustomListEvent.NavigateBack("Список не знайдено"))
          }
        } else if (projectId != null) {
          val initialContent = TextFieldValue("• ", selection = TextRange(2))
          _uiState.value =
            UnifiedCustomListUiState(
              isExistingList = false,
              isLoading = false,
              content = initialContent,
              toolbarState = createToolbarState(initialContent, true),
              undoStack = listOf(initialContent),
            )
        } else {
          _events.send(UnifiedCustomListEvent.NavigateBack("Невірні параметри"))
        }
      } catch (e: Exception) {
        _events.send(UnifiedCustomListEvent.ShowError("Помилка завантаження: ${e.message}"))
      }
    }
  }

  private fun createToolbarState(content: TextFieldValue, isEditing: Boolean): ListToolbarState {
    val text = content.text
    val cursorPosition = content.selection.start
    val lines = text.lines()
    val itemCount = lines.count { it.isNotBlank() }

    // Визначаємо поточний формат
    val formatMode = detectFormatMode(lines)

    // Визначаємо можливості відступів та переміщення
    val (canIndent, canDeIndent, canMoveUp, canMoveDown) =
      calculateCapabilities(text, cursorPosition)

    return ListToolbarState(
      isEditing = isEditing,
      formatMode = formatMode,
      totalItems = itemCount,
      hasSelection = content.selection.length > 0,
      canIndent = canIndent,
      canDeIndent = canDeIndent,
      canMoveUp = canMoveUp,
      canMoveDown = canMoveDown,
    )
  }

  private fun detectFormatMode(lines: List<String>): ListFormatMode {
    val nonEmptyLines = lines.filter { it.isNotBlank() }
    if (nonEmptyLines.isEmpty()) return ListFormatMode.BULLET

    val bulletCount = nonEmptyLines.count { it.trimStart().startsWith("•") }
    val numberedCount =
      nonEmptyLines.count { line ->
        val trimmed = line.trimStart()
        trimmed.matches(Regex("^\\d+\\.\\s.*"))
      }
    val checklistCount =
      nonEmptyLines.count { line ->
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

  private fun calculateCapabilities(text: String, cursorPosition: Int): List<Boolean> {
    val lineInfo = getLineInfoForPosition(text, cursorPosition)
    val lines = text.lines()

    val canIndent = true // Завжди можна додати відступ
    val canDeIndent = lineInfo.indentLevel > 0
    val canMoveUp = lineInfo.lineIndex > 0
    val canMoveDown = lineInfo.lineIndex < lines.size - 1

    return listOf(canIndent, canDeIndent, canMoveUp, canMoveDown)
  }

  private fun addToUndoStack(content: TextFieldValue) {
    val currentState = _uiState.value
    val newUndoStack = (currentState.undoStack + content).takeLast(50) // Обмеження стеку
    _uiState.value =
      currentState.copy(
        undoStack = newUndoStack,
        redoStack = emptyList(), // Очищуємо redo при новій дії
      )
  }

  fun onTitleChange(newTitle: String) {
    _uiState.value =
      _uiState.value.copy(title = newTitle, error = null, isSaveEnabled = newTitle.isNotBlank())
    scheduleAutoSave()
  }

  fun onContentChange(newContent: TextFieldValue) {
    val currentContent = _uiState.value.content
    if (currentContent.text != newContent.text) {
      addToUndoStack(currentContent)
    }
    updateContent(newContent)
    scheduleAutoSave()
  }

  private fun updateContent(content: TextFieldValue) {
    val toolbarState = createToolbarState(content, _uiState.value.toolbarState.isEditing)
    _uiState.value = _uiState.value.copy(content = content, toolbarState = toolbarState)
  }

  fun onToggleEditMode(isEditing: Boolean) {
    _uiState.value =
      _uiState.value.copy(toolbarState = _uiState.value.toolbarState.copy(isEditing = isEditing))
  }

  fun onToggleFold(lineIndex: Int) {
    val currentCollapsed = _uiState.value.collapsedLines.toMutableSet()
    if (currentCollapsed.contains(lineIndex)) {
      currentCollapsed.remove(lineIndex)
    } else {
      currentCollapsed.add(lineIndex)
    }
    _uiState.value = _uiState.value.copy(collapsedLines = currentCollapsed)
  }

  // Додаткові функції форматування
  fun onToggleNumbered() {
    val content = _uiState.value.content
    val lines =
      content.text.lines().mapIndexed { index, line ->
        val trimmed = line.trimStart()
        val indent = line.takeWhile { it.isWhitespace() }

        when {
          trimmed.startsWith("• ") -> "$indent${index + 1}. ${trimmed.removePrefix("• ")}"
          trimmed.matches(Regex("^\\d+\\.\\s.*")) ->
            "$indent• ${trimmed.replaceFirst(Regex("^\\d+\\.\\s"), "")}"
          trimmed.startsWith("☐ ") || trimmed.startsWith("☑ ") ->
            "$indent${index + 1}. ${trimmed.substring(2)}"
          trimmed.isNotEmpty() -> "$indent${index + 1}. $trimmed"
          else -> line
        }
      }

    val newText = lines.joinToString("\n")
    updateContent(TextFieldValue(newText, content.selection))
  }

  fun onToggleChecklist() {
    val content = _uiState.value.content
    val lines =
      content.text.lines().map { line ->
        val trimmed = line.trimStart()
        val indent = line.takeWhile { it.isWhitespace() }

        when {
          trimmed.startsWith("• ") -> "$indent☐ ${trimmed.removePrefix("• ")}"
          trimmed.matches(Regex("^\\d+\\.\\s.*")) ->
            "$indent☐ ${trimmed.replaceFirst(Regex("^\\d+\\.\\s"), "")}"
          trimmed.startsWith("☐ ") -> "$indent☑ ${trimmed.removePrefix("☐ ")}"
          trimmed.startsWith("☑ ") -> "$indent☐ ${trimmed.removePrefix("☑ ")}"
          trimmed.isNotEmpty() -> "$indent☐ $trimmed"
          else -> line
        }
      }

    val newText = lines.joinToString("\n")
    updateContent(TextFieldValue(newText, content.selection))
  }

  fun onUndo() {
    val currentState = _uiState.value
    if (currentState.undoStack.size > 1) {
      val previousContent = currentState.undoStack[currentState.undoStack.size - 2]
      val newUndoStack = currentState.undoStack.dropLast(1)
      val newRedoStack = currentState.redoStack + currentState.content

      _uiState.value =
        currentState.copy(
          content = previousContent,
          undoStack = newUndoStack,
          redoStack = newRedoStack,
          toolbarState = createToolbarState(previousContent, currentState.toolbarState.isEditing),
        )
    }
  }

  fun onRedo() {
    val currentState = _uiState.value
    if (currentState.redoStack.isNotEmpty()) {
      val nextContent = currentState.redoStack.last()
      val newRedoStack = currentState.redoStack.dropLast(1)
      val newUndoStack = currentState.undoStack + currentState.content

      _uiState.value =
        currentState.copy(
          content = nextContent,
          undoStack = newUndoStack,
          redoStack = newRedoStack,
          toolbarState = createToolbarState(nextContent, currentState.toolbarState.isEditing),
        )
    }
  }

  private fun scheduleAutoSave() {
    if (!_uiState.value.autoSaveEnabled || !_uiState.value.isExistingList) return

    autoSaveJob?.cancel()
    autoSaveJob =
      viewModelScope.launch {
        kotlinx.coroutines.delay(2000) // Автозбереження через 2 секунди
        performAutoSave()
      }
  }

  private suspend fun performAutoSave() {
    try {
      val currentState = _uiState.value
      if (currentState.isExistingList && listId != null) {
        val updatedList =
          currentState.list!!.copy(name = currentState.title, content = currentState.content.text)
        projectRepository.updateCustomList(updatedList)
        _events.send(UnifiedCustomListEvent.AutoSaved)
      }
    } catch (e: Exception) {
      // Тихо ігноруємо помилки автозбереження
    }
  }

  // Інші функції залишаються такими ж, але з покращеним обробленням помилок
  private data class LineInfo(
    val fullLineText: String,
    val startIndexInText: Int,
    val indentLevel: Int,
    val lineIndex: Int,
  )

  private fun getLineInfoForPosition(text: String, position: Int): LineInfo {
    val safePosition = position.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', startIndex = safePosition - 1) + 1
    var lineEnd = text.indexOf('\n', startIndex = lineStart)
    if (lineEnd == -1) lineEnd = text.length

    val fullLine = text.substring(lineStart, lineEnd)
    val indent = fullLine.takeWhile { it.isWhitespace() }.length
    val lineIndex = text.substring(0, safePosition).count { it == '\n' }

    return LineInfo(fullLine, lineStart, indent, lineIndex)
  }

  private fun getTextBlockLines(startLineIndex: Int, allLines: List<String>): List<String> {
    if (startLineIndex >= allLines.size) return emptyList()
    val startIndent = allLines[startLineIndex].takeWhile { it.isWhitespace() }.length
    val block = mutableListOf(allLines[startLineIndex])

    for (i in (startLineIndex + 1) until allLines.size) {
      val nextLine = allLines[i]
      if (nextLine.isBlank()) {
        block.add(nextLine)
        continue
      }
      val nextLineIndent = nextLine.takeWhile { it.isWhitespace() }.length
      if (nextLineIndent > startIndent) {
        block.add(nextLine)
      } else {
        break
      }
    }
    return block
  }

  // Решта функцій залишаються такими ж, як в оригінальному коді
  fun onIndentBlock() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val allLines = content.text.lines()
    val currentLineInfo = getLineInfoForPosition(content.text, content.selection.start)
    val block = getTextBlockLines(currentLineInfo.lineIndex, allLines)

    val indentedBlock = block.map { "    $it" }
    val newLines = allLines.toMutableList()
    newLines.subList(currentLineInfo.lineIndex, currentLineInfo.lineIndex + block.size).clear()
    newLines.addAll(currentLineInfo.lineIndex, indentedBlock)

    val newText = newLines.joinToString("\n")
    val newSelection = (content.selection.start + 4).coerceAtMost(newText.length)
    updateContent(TextFieldValue(newText, TextRange(newSelection)))
  }

  fun onDeIndentBlock() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val allLines = content.text.lines()
    val currentLineInfo = getLineInfoForPosition(content.text, content.selection.start)
    val block = getTextBlockLines(currentLineInfo.lineIndex, allLines)

    val deIndentedBlock = block.map { it.removePrefix("    ") }
    val newLines = allLines.toMutableList()
    newLines.subList(currentLineInfo.lineIndex, currentLineInfo.lineIndex + block.size).clear()
    newLines.addAll(currentLineInfo.lineIndex, deIndentedBlock)

    val newText = newLines.joinToString("\n")
    val newSelection = (content.selection.start - 4).coerceAtLeast(0)
    updateContent(TextFieldValue(newText, TextRange(newSelection)))
  }

  fun onMoveBlockUp() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    if (content.text.isEmpty()) return
    val allLines = content.text.lines()
    val currentLineInfo = getLineInfoForPosition(content.text, content.selection.start)
    if (currentLineInfo.lineIndex == 0) return

    val blockToMove = getTextBlockLines(currentLineInfo.lineIndex, allLines)
    val prevLineInfo = getLineInfoForPosition(content.text, currentLineInfo.startIndexInText - 1)
    val targetBlock = getTextBlockLines(prevLineInfo.lineIndex, allLines)
    val targetBlockStartIndex = prevLineInfo.lineIndex

    if ((currentLineInfo.lineIndex - targetBlock.size) < 0) return

    val newLines = allLines.toMutableList()
    newLines.subList(targetBlockStartIndex, currentLineInfo.lineIndex + blockToMove.size).clear()
    newLines.addAll(targetBlockStartIndex, blockToMove)
    newLines.addAll(targetBlockStartIndex + blockToMove.size, targetBlock)

    val newText = newLines.joinToString("\n")
    val cursorOffset = targetBlock.joinToString("\n").length + 1
    updateContent(
      TextFieldValue(newText, TextRange((content.selection.start - cursorOffset).coerceAtLeast(0)))
    )
  }

  fun onMoveBlockDown() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    if (content.text.isEmpty()) return
    val allLines = content.text.lines()
    val currentLineInfo = getLineInfoForPosition(content.text, content.selection.start)
    val blockToMove = getTextBlockLines(currentLineInfo.lineIndex, allLines)

    val nextBlockStartIndex = currentLineInfo.lineIndex + blockToMove.size
    if (nextBlockStartIndex >= allLines.size) return

    val targetBlock = getTextBlockLines(nextBlockStartIndex, allLines)

    val newLines = allLines.toMutableList()
    newLines.subList(currentLineInfo.lineIndex, nextBlockStartIndex + targetBlock.size).clear()
    newLines.addAll(currentLineInfo.lineIndex, targetBlock)
    newLines.addAll(currentLineInfo.lineIndex + targetBlock.size, blockToMove)

    val newText = newLines.joinToString("\n")
    val cursorOffset = targetBlock.joinToString("\n").length + 1
    updateContent(
      TextFieldValue(
        newText,
        TextRange((content.selection.start + cursorOffset).coerceAtMost(newText.length)),
      )
    )
  }

  fun onIndentLine() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val lineInfo = getLineInfoForPosition(content.text, content.selection.start)
    val newText =
      content.text.replaceRange(lineInfo.startIndexInText, lineInfo.startIndexInText, "    ")
    val newSelection = content.selection.start + 4
    updateContent(TextFieldValue(newText, TextRange(newSelection)))
  }

  fun onDeIndentLine() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val lineInfo = getLineInfoForPosition(content.text, content.selection.start)
    if (lineInfo.fullLineText.startsWith("    ")) {
      val newText =
        content.text.replaceRange(lineInfo.startIndexInText, lineInfo.startIndexInText + 4, "")
      val newSelection = (content.selection.start - 4).coerceAtLeast(0)
      updateContent(TextFieldValue(newText, TextRange(newSelection)))
    }
  }

  fun onMoveLineUp() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val allLines = content.text.lines().toMutableList()
    val lineInfo = getLineInfoForPosition(content.text, content.selection.start)
    if (lineInfo.lineIndex > 0) {
      val prevLineIndex = lineInfo.lineIndex - 1
      val temp = allLines[prevLineIndex]
      allLines[prevLineIndex] = allLines[lineInfo.lineIndex]
      allLines[lineInfo.lineIndex] = temp

      val newText = allLines.joinToString("\n")
      val newCursor =
        (content.selection.start - (allLines[lineInfo.lineIndex].length + 1)).coerceAtLeast(0)
      updateContent(TextFieldValue(newText, TextRange(newCursor)))
    }
  }

  fun onMoveLineDown() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val allLines = content.text.lines().toMutableList()
    val lineInfo = getLineInfoForPosition(content.text, content.selection.start)
    if (lineInfo.lineIndex < allLines.size - 1) {
      val nextLineIndex = lineInfo.lineIndex + 1
      val temp = allLines[nextLineIndex]
      allLines[nextLineIndex] = allLines[lineInfo.lineIndex]
      allLines[lineInfo.lineIndex] = temp

      val newText = allLines.joinToString("\n")
      val newCursor =
        (content.selection.start + (allLines[nextLineIndex].length + 1)).coerceAtMost(
          newText.length
        )
      updateContent(TextFieldValue(newText, TextRange(newCursor)))
    }
  }

  fun onToggleBullet() {
    addToUndoStack(_uiState.value.content)
    val content = _uiState.value.content
    val currentLineInfo = getLineInfoForPosition(content.text, content.selection.start)
    val line = currentLineInfo.fullLineText

    val marker = "• "
    val leadingWhitespace = line.takeWhile { it.isWhitespace() }
    val trimmedLine = line.trimStart()

    val newLineText: String =
      if (trimmedLine.startsWith(marker)) {
        leadingWhitespace + trimmedLine.removePrefix(marker)
      } else {
        leadingWhitespace + marker + trimmedLine
      }

    val newText =
      content.text.replaceRange(
        currentLineInfo.startIndexInText,
        currentLineInfo.startIndexInText + line.length,
        newLineText,
      )
    updateContent(TextFieldValue(newText, content.selection))
  }

  fun onSave() {
    val currentState = _uiState.value
    if (currentState.title.isBlank() && !currentState.isExistingList) {
      _uiState.value = _uiState.value.copy(error = "Назва не може бути порожньою")
      return
    }

    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        if (currentState.isExistingList && listId != null) {
          val updatedList =
            currentState.list!!.copy(name = currentState.title, content = currentState.content.text)
          projectRepository.updateCustomList(updatedList)
          _events.send(UnifiedCustomListEvent.ShowSuccess("Список оновлено"))
        } else if (!currentState.isExistingList && projectId != null) {
          projectRepository.createCustomList(
            name = currentState.title,
            projectId = projectId,
            content = currentState.content.text,
          )
          _events.send(UnifiedCustomListEvent.NavigateBack("Список створено успішно"))
        }

        _uiState.value = _uiState.value.copy(isLoading = false)
      } catch (e: Exception) {
        _uiState.value =
          _uiState.value.copy(isLoading = false, error = "Помилка збереження: ${e.message}")
        _events.send(UnifiedCustomListEvent.ShowError("Не вдалося зберегти список"))
      }
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }

  fun toggleAutoSave() {
    _uiState.value = _uiState.value.copy(autoSaveEnabled = !_uiState.value.autoSaveEnabled)
  }

  override fun onCleared() {
    super.onCleared()
    autoSaveJob?.cancel()
  }
}
