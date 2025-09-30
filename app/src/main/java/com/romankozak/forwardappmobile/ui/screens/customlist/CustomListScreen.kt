package com.romankozak.forwardappmobile.ui.screens.customlist

// FIX: Added a newline and separated the package declaration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

enum class ListFormatMode {
  BULLET,
  NUMBERED,
  CHECKLIST,
  PLAIN,
}

enum class SortMode {
  NONE,
  ALPHABETICAL,
  CREATION_DATE,
  PRIORITY,
}

data class ListToolbarState(
  val isEditing: Boolean = false,
  val formatMode: ListFormatMode = ListFormatMode.BULLET,
  val sortMode: SortMode = SortMode.NONE,
  val hasSelection: Boolean = false,
  val selectedCount: Int = 0,
  val totalItems: Int = 0,
  val canUndo: Boolean = false,
  val isSelectAllMode: Boolean = false,
)

enum class ScreenMode {
  CREATE,
  EDIT_EXISTING,
  VIEW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCustomListScreen(
  navController: NavController,
  listId: String? = null, // null для створення нового списку
  projectId: String? = null, // для створення нового списку
  viewModel: UnifiedCustomListViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var screenMode by remember {
    mutableStateOf(
      when {
        listId != null -> ScreenMode.VIEW
        projectId != null -> ScreenMode.CREATE
        else -> ScreenMode.CREATE
      }
    )
  }

  // Toolbar state
  var toolbarState: ListToolbarState by remember {
    mutableStateOf(
      ListToolbarState(
        isEditing = screenMode != ScreenMode.VIEW,
        formatMode = ListFormatMode.BULLET,
        totalItems = 0,
      )
    )
  }

  val keyboardController = LocalSoftwareKeyboardController.current
  val titleFocusRequester = remember { FocusRequester() }
  val contentFocusRequester = remember { FocusRequester() }

  // Update toolbar state when screen mode changes
  LaunchedEffect(screenMode, uiState.content.text) {
    val itemCount = uiState.content.text.lines().count { it.trim().isNotEmpty() }
    toolbarState = 
      toolbarState.copy(
        isEditing = screenMode != ScreenMode.VIEW,
        totalItems = itemCount,
        canUndo = false, // TODO: implement undo functionality
      )
  }

  // Ініціалізація
  LaunchedEffect(listId, projectId) { viewModel.initialize(listId, projectId) }

  // Обробка подій
  LaunchedEffect(Unit) { 
    viewModel.events.collect { event ->
      when (event) {
        is UnifiedCustomListEvent.NavigateBack -> {
          navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
          navController.popBackStack()
        }
        is UnifiedCustomListEvent.ShowError -> {
          // Обробка помилки
        }
      }
    }
  }

  // Керування фокусом при зміні режиму
  LaunchedEffect(screenMode) {
    when (screenMode) {
      ScreenMode.CREATE -> {
        titleFocusRequester.requestFocus()
      }
      ScreenMode.EDIT_EXISTING -> {
        contentFocusRequester.requestFocus()
      }
      ScreenMode.VIEW -> {
        keyboardController?.hide()
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.ListAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text =
                  when (screenMode) {
                    ScreenMode.CREATE -> "New Custom List"
                    ScreenMode.EDIT_EXISTING -> "Edit List"
                    ScreenMode.VIEW -> uiState.title.ifEmpty { "Custom List" }
                  },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
              )
              if (screenMode == ScreenMode.VIEW) {
                Text(
                  text = "Tap to edit content",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
              }
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          when (screenMode) {
            ScreenMode.VIEW -> {
              IconButton(onClick = { screenMode = ScreenMode.EDIT_EXISTING }) {
                Icon(
                  Icons.Default.Edit,
                  contentDescription = "Edit",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            ScreenMode.EDIT_EXISTING -> {
              IconButton(onClick = { screenMode = ScreenMode.VIEW }) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Cancel",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            ScreenMode.CREATE -> {
              // Показуємо Save button у FAB
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
    floatingActionButton = {
      when (screenMode) {
        ScreenMode.CREATE -> {
          AnimatedVisibility(
            visible = uiState.isSaveEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            FloatingActionButton(
              onClick = { viewModel.onSave() },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
              Icon(Icons.Default.Add, contentDescription = "Create")
            }
          }
        }
        ScreenMode.EDIT_EXISTING -> {
          FloatingActionButton(
            onClick = {
              viewModel.onSave()
              screenMode = ScreenMode.VIEW
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(Icons.Default.Check, contentDescription = "Save")
          }
        }
        ScreenMode.VIEW -> {
          // Немає FAB у режимі перегляду
        }
      }
    },
  ) { it ->
    Column(modifier = Modifier.fillMaxSize()) {
      // Enhanced Toolbar
      if (screenMode == ScreenMode.VIEW || screenMode == ScreenMode.EDIT_EXISTING) {
        SimpleListToolbar(
          state = toolbarState,
          onToggleEdit = {
            screenMode =
              if (screenMode == ScreenMode.VIEW) {
                ScreenMode.EDIT_EXISTING
              } else {
                ScreenMode.VIEW
              }
          },
          onAddItem = {
            val currentText = uiState.content.text
            val newText =
              if (currentText.isEmpty()) {
                "• "
              } else if (!currentText.endsWith("\n")) {
                "$currentText\n• "
              } else {
                "$currentText• "
              }
            viewModel.onContentChange(
              TextFieldValue(newText, selection = TextRange(newText.length))
            )
          },
          onFormatChange = { format ->
            toolbarState = toolbarState.copy(formatMode = format)
            // TODO: Convert existing content to new format
          },
          onSortChange = { sort ->
            toolbarState = toolbarState.copy(sortMode = sort)
            // TODO: Implement sorting
          },
          onSelectAll = {
            // TODO: Implement select all
          },
          onClearSelection = {
            toolbarState = toolbarState.copy(hasSelection = false, selectedCount = 0)
          },
          onDeleteSelected = {
            // TODO: Implement delete selected
          },
          onUndo = {
            // TODO: Implement undo
          },
        )
      }

      // Main content
      when (screenMode) {
        ScreenMode.CREATE ->
          CreateEditContent(
            uiState = uiState,
            viewModel = viewModel,
            paddingValues = it,
            titleFocusRequester = titleFocusRequester,
            contentFocusRequester = contentFocusRequester,
            isCreating = true,
          )
        ScreenMode.EDIT_EXISTING ->
          CreateEditContent(
            uiState = uiState,
            viewModel = viewModel,
            paddingValues = it,
            titleFocusRequester = titleFocusRequester,
            contentFocusRequester = contentFocusRequester,
            isCreating = false,
          )
        ScreenMode.VIEW ->
          ViewContent(
            uiState = uiState,
            paddingValues = it,
            onContentClick = { screenMode = ScreenMode.EDIT_EXISTING },
          )
      }
    }
  }
}

@Composable
private fun SimpleListToolbar(
  state: ListToolbarState,
  onToggleEdit: () -> Unit,
  onAddItem: () -> Unit,
  onFormatChange: (ListFormatMode) -> Unit,
  onSortChange: (SortMode) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onDeleteSelected: () -> Unit,
  onUndo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFormatMenu by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (state.isEditing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (state.isEditing) 4.dp else 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left side - Mode indicator and stats
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Mode toggle button
        Surface(
          shape = CircleShape,
          color =
            if (state.isEditing) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(32.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable { onToggleEdit() }.padding(8.dp),
          ) {
            Icon(
              imageVector = if (state.isEditing) Icons.Default.Check else Icons.Default.Edit,
              contentDescription = if (state.isEditing) "Finish editing" else "Start editing",
              tint =
                if (state.isEditing) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        // Stats
        Column {
          Text(
            text = "${state.totalItems} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          if (state.isEditing) {
            Text(
              text = "Editing mode",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            )
          }
        }
      }

      // Right side - Action buttons
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (state.isEditing) {
          // Add item button
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.clickable { onAddItem() },
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add item",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        // Format menu
        Box {
          IconButton(onClick = { showFormatMenu = true }) {
            Icon(
              imageVector =
                when (state.formatMode) {
                  ListFormatMode.BULLET -> Icons.Default.FormatListBulleted
                  ListFormatMode.NUMBERED -> Icons.Default.FormatListNumbered
                  ListFormatMode.CHECKLIST -> Icons.Default.CheckBox
                  ListFormatMode.PLAIN -> Icons.Default.FormatAlignLeft
                },
              contentDescription = "Change format",
              tint = MaterialTheme.colorScheme.primary,
            )
          }

          DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }) {
            DropdownMenuItem(
              text = { Text("Bullet List") },
              onClick = {
                onFormatChange(ListFormatMode.BULLET)
                showFormatMenu = false
              },
              leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
            )
            DropdownMenuItem(
              text = { Text("Numbered List") },
              onClick = {
                onFormatChange(ListFormatMode.NUMBERED)
                showFormatMenu = false
              },
              leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
            )
            DropdownMenuItem(
              text = { Text("Checklist") },
              onClick = {
                onFormatChange(ListFormatMode.CHECKLIST)
                showFormatMenu = false
              },
              leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
            )
          }
        }
      }
    }
  }
} // <-- FIX: Added missing closing brace

@Composable
private fun CreateEditContent(
  uiState: UnifiedCustomListUiState,
  viewModel: UnifiedCustomListViewModel,
  paddingValues: androidx.compose.foundation.layout.PaddingValues,
  titleFocusRequester: FocusRequester,
  contentFocusRequester: FocusRequester,
  isCreating: Boolean,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(start = 16.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding())
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    // Title Section (показуємо тільки при створенні або якщо потрібно редагувати назву)
    if (isCreating || !uiState.isExistingList) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Title,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "List Title",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester),
            placeholder = {
              Text("Enter list title...", style = MaterialTheme.typography.bodyLarge)
            },
            singleLine = true,
            isError = uiState.error != null,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                errorBorderColor = MaterialTheme.colorScheme.error,
              ),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
          )

          AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            val error = uiState.error
            if (error != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Error,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = error,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
      }
    }

    // Content Section
    Card(
      modifier = Modifier.fillMaxWidth().weight(1f),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "✏",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text = "List Content",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        BasicTextField(
          value = uiState.content,
          onValueChange = { newText ->
            val oldText = uiState.content
            viewModel.onContentChange(newText)

            // Handle Enter key - auto add bullet points
            if (
              newText.text.length > oldText.text.length &&
                oldText.selection.end < newText.text.length &&
                newText.text[oldText.selection.start] == '\n'
            ) {

              val lineStart =
                newText.text.lastIndexOf('\n', startIndex = oldText.selection.start - 1) + 1
              val previousLine = newText.text.substring(lineStart, oldText.selection.start)
              val leadingWhitespace = previousLine.takeWhile { it.isWhitespace() }

              if (previousLine.trim().startsWith("• ")) {
                val listMarker = "• "
                val newCursorPos =
                  newText.selection.start + leadingWhitespace.length + listMarker.length
                val finalText =
                  newText.text.substring(0, newText.selection.start) +
                    leadingWhitespace +
                    listMarker +
                    newText.text.substring(newText.selection.start)
                viewModel.onContentChange(
                  TextFieldValue(finalText, selection = TextRange(newCursorPos))
                )
              }
            }
          },
          modifier = Modifier.fillMaxSize().focusRequester(contentFocusRequester),
          textStyle =
            TextStyle(
              fontSize = MaterialTheme.typography.bodyLarge.fontSize,
              lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
              color = MaterialTheme.colorScheme.onSurface,
            ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
          decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
              if (uiState.content.text.isEmpty()) {
                Text(
                  text =
                    "• Start typing your list items\n• Each line can be a new item\n• Use bullet points for better organization",
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
              }
              innerTextField()
            }
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(80.dp))
  }
}

@Composable
private fun ViewContent(
  uiState: UnifiedCustomListUiState,
  paddingValues: androidx.compose.foundation.layout.PaddingValues,
  onContentClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(start = 16.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding())
  ) {
    Card(
      modifier = Modifier.fillMaxSize().padding(16.dp).clickable { onContentClick() },
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        val content = uiState.content.text
        if (content.isNotBlank()) {
          content.lines().forEach { line ->
            if (line.trim().isNotEmpty()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Box(
                  modifier =
                    Modifier.size(6.dp)
                      .clip(RoundedCornerShape(3.dp))
                      .background(MaterialTheme.colorScheme.primary)
                      .padding(top = 8.dp)
                )
                Text(
                  text = line.removePrefix("• ").removePrefix("-").trim(),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        } else {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Icon(
              imageVector = Icons.Default.ListAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = "This list is empty.\nTap anywhere to add items.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        }
      }
    }
  }
}
