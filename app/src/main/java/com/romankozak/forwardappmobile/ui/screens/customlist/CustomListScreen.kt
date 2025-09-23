package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.screens.customlist.components.EnhancedListToolbar

enum class ScreenMode {
  CREATE,
  EDIT_EXISTING,
  VIEW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCustomListScreen(
  navController: NavController,
  viewModel: UnifiedCustomListViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var screenMode by remember {
    mutableStateOf(if (viewModel.isNewList) ScreenMode.CREATE else ScreenMode.VIEW)
  }

  val keyboardController = LocalSoftwareKeyboardController.current
  val titleFocusRequester = remember { FocusRequester() }
  val contentFocusRequester = remember { FocusRequester() }

  // Анімовані кольори для smooth transition
  val animatedBackgroundColor by
    animateColorAsState(
      targetValue =
        when (screenMode) {
          ScreenMode.VIEW -> MaterialTheme.colorScheme.surface
          else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
      animationSpec = tween(300),
      label = "background_color",
    )

  LaunchedEffect(screenMode) { viewModel.onToggleEditMode(screenMode != ScreenMode.VIEW) }

  LaunchedEffect(Unit) { 
    viewModel.events.collect { event ->
      when (event) {
        is UnifiedCustomListEvent.NavigateBack -> {
          navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
          navController.popBackStack()
        }
        is UnifiedCustomListEvent.ShowError -> {
          // TODO: Показати снекбар з помилкою
        }
        is UnifiedCustomListEvent.ShowSuccess -> {
          // TODO: Показати снекбар з успіхом
        }
        is UnifiedCustomListEvent.AutoSaved -> {
          // TODO: Показати невеликий індикатор автозбереження
        }
      }
    }
  }

  LaunchedEffect(screenMode) {
    when (screenMode) {
      ScreenMode.CREATE -> titleFocusRequester.requestFocus()
      ScreenMode.EDIT_EXISTING -> contentFocusRequester.requestFocus()
      ScreenMode.VIEW -> keyboardController?.hide()
    }
  }

  Scaffold(
    modifier = Modifier.safeDrawingPadding().background(animatedBackgroundColor),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      EnhancedTopAppBar(
        screenMode = screenMode,
        title = uiState.title,
        totalItems = uiState.toolbarState.totalItems,
        isLoading = uiState.isLoading,
        onNavigateBack = { navController.popBackStack() },
        onEdit = { screenMode = ScreenMode.EDIT_EXISTING },
        onSave = {
          viewModel.onSave()
          screenMode = ScreenMode.VIEW
        },
      )
    },
    bottomBar = {
      AnimatedVisibility(
        visible = screenMode != ScreenMode.VIEW,
        enter =
          slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(),
      ) {
        EnhancedListToolbar(
          state = uiState.toolbarState,
          onIndentBlock = viewModel::onIndentBlock,
          onDeIndentBlock = viewModel::onDeIndentBlock,
          onMoveBlockUp = viewModel::onMoveBlockUp,
          onMoveBlockDown = viewModel::onMoveBlockDown,
          onIndentLine = viewModel::onIndentLine,
          onDeIndentLine = viewModel::onDeIndentLine,
          onMoveLineUp = viewModel::onMoveLineUp,
          onMoveLineDown = viewModel::onMoveLineDown,
          onToggleBullet = viewModel::onToggleBullet,
          onToggleNumbered = viewModel::onToggleNumbered,
          onToggleChecklist = viewModel::onToggleChecklist,
          onUndo = viewModel::onUndo,
          onRedo = viewModel::onRedo,
        )
      }
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = screenMode == ScreenMode.CREATE,
        enter =
          scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
            fadeIn(),
        exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
      ) {
        ExtendedFloatingActionButton(
          onClick = viewModel::onSave,
          icon = { Icon(Icons.Default.Add, contentDescription = "Створити список") },
          text = { Text("Створити") },
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        )
      }
    },
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(animatedBackgroundColor)) {
      when (screenMode) {
        ScreenMode.CREATE,
        ScreenMode.EDIT_EXISTING -> {
          CreateEditContent(
            uiState = uiState,
            viewModel = viewModel,
            titleFocusRequester = titleFocusRequester,
            contentFocusRequester = contentFocusRequester,
          )
        }
        ScreenMode.VIEW -> {
          ViewContent(
            uiState = uiState,
            viewModel = viewModel,
            onContentClick = { screenMode = ScreenMode.EDIT_EXISTING },
          )
        }
      }

      // Показати помилку якщо є
      uiState.error?.let { error ->
        LaunchedEffect(error) {
          // TODO: Показати снекбар
        }
      }

      // Індикатор завантаження
      if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
              ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          ) {
            Row(
              modifier = Modifier.padding(24.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
              Text(text = "Збереження...", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedTopAppBar(
  screenMode: ScreenMode,
  title: String,
  totalItems: Int,
  isLoading: Boolean,
  onNavigateBack: () -> Unit,
  onEdit: () -> Unit,
  onSave: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    TopAppBar(
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ListAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.fillMaxSize().padding(6.dp),
            )
          }
          Column {
            Text(
              text =
                when (screenMode) {
                  ScreenMode.CREATE -> "Новий список"
                  ScreenMode.EDIT_EXISTING -> "Редагування"
                  ScreenMode.VIEW -> title.ifEmpty { "Список" }
                },
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.SemiBold,
            )
            if (screenMode == ScreenMode.VIEW && totalItems > 0) {
              Text(
                text = "$totalItems пунктів",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      },
      navigationIcon = {
        IconButton(onClick = onNavigateBack) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      actions = {
        when (screenMode) {
          ScreenMode.VIEW -> {
            IconButton(onClick = onEdit) {
              Icon(
                Icons.Default.Edit,
                contentDescription = "Редагувати",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          ScreenMode.EDIT_EXISTING -> {
            FilledTonalIconButton(onClick = onSave, enabled = !isLoading) {
              if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Check, contentDescription = "Зберегти")
              }
            }
          }
          ScreenMode.CREATE -> {
            // FAB обробляє збереження
          }
        }
      },
      colors =
        TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    )
  }
}

@Composable
private fun CreateEditContent(
    uiState: UnifiedCustomListUiState,
    viewModel: UnifiedCustomListViewModel,
    titleFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            BasicTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .focusRequester(titleFocusRequester),
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (uiState.title.isEmpty()) {
                        Text(
                            text = "Enter list title...",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Контент
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            val content = uiState.content
            val text = content.text
            val selection = content.selection
            val lines = text.lines()

            val textUpToCursor = text.take(selection.start)
            val cursorLineIndex = textUpToCursor.count { it == '\n' }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Top
            ) {
                var i = 0
                while (i < lines.size) {
                    val line = lines[i]
                    val indent = line.takeWhile { it.isWhitespace() }.length
                    val nextIndent = if (i + 1 < lines.size) lines[i + 1].takeWhile { it.isWhitespace() }.length else -1
                    val isParent = nextIndent > indent && line.isNotBlank()
                    val isCollapsed = uiState.collapsedLines.contains(i)

                    val lineIndex = i
                    // Основний рядок
                    item(key = "line_$lineIndex") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width((indent * 16).dp))
                            // Індикатор згортання
                            if (isParent) {
                                val icon = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown
                                IconButton(
                                    onClick = { viewModel.onToggleFold(lineIndex) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = if (isCollapsed) "Expand" else "Collapse"
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(24.dp))
                            }

                            // Редагований текст
                            val valueForField = if (lineIndex == cursorLineIndex) {
                                val lineStartOffset = lines.take(lineIndex).sumOf { it.length + 1 }
                                val selectionInLine = TextRange(
                                    (selection.start - lineStartOffset).coerceIn(0, line.length),
                                    (selection.end - lineStartOffset).coerceIn(0, line.length)
                                )
                                TextFieldValue(line, selectionInLine)
                            } else {
                                TextFieldValue(line)
                            }

                            BasicTextField(
                                value = valueForField,
                                onValueChange = { newValue ->
                                    val mutable = lines.toMutableList()
                                    if (lineIndex < mutable.size) {
                                        mutable[lineIndex] = newValue.text
                                        val newText = mutable.joinToString("\n")

                                        val startOfLineOffset = lines.take(lineIndex).sumOf { it.length + 1 }
                                        val newSelection = TextRange(
                                            start = startOfLineOffset + newValue.selection.start,
                                            end = startOfLineOffset + newValue.selection.end
                                        )
                                        viewModel.onContentChange(TextFieldValue(text = newText, selection = newSelection))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(contentFocusRequester),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (line.isEmpty()) {
                                        Text(
                                            text = "…",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Якщо рядок згорнутий — додаємо індикатор кількості прихованих
                    if (isCollapsed) {
                        val hiddenCount = countHiddenLines(lines, i, indent)
                        if (hiddenCount > 0) {
                            item(key = "collapsed_$i") {
                                Text(
                                    text = "... $hiddenCount collapsed items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier
                                        .padding(start = (32 + indent * 16).dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                        }

                        // Пропускаємо дочірні рядки
                        while (i + 1 < lines.size &&
                            (lines[i + 1].isBlank() || lines[i + 1].takeWhile { it.isWhitespace() }.length > indent)
                        ) {
                            i++
                        }
                    }

                    i++
                }
            }
        }
    }
}


@Composable
private fun FoldingGutter(
  lines: List<String>,
  collapsedLines: Set<Int>,
  onToggleFold: (Int) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.width(48.dp).padding(PaddingValues(top = 16.dp, start = 8.dp, end = 4.dp)),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    itemsIndexed(lines) { index, line ->
      val indent = line.takeWhile { it.isWhitespace() }.length
      val nextLineIndent =
        if (index + 1 < lines.size) {
          lines[index + 1].takeWhile { it.isWhitespace() }.length
        } else -1
      val isParent = nextLineIndent > indent && !line.isBlank()

      Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
        if (isParent) {
          val isCollapsed = collapsedLines.contains(index)
          val icon =
            if (isCollapsed) {
              Icons.Default.ChevronRight
            } else {
              Icons.Default.KeyboardArrowDown
            }

          FilledTonalIconButton(
            onClick = { onToggleFold(index) },
            modifier = Modifier.size(20.dp),
            colors =
              IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              ),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = if (isCollapsed) "Розгорнути" else "Згорнути",
              modifier = Modifier.size(12.dp),
            )
          }
        }
      }
    }
  }
}

// Клас для візуальної трансформації складання
private class FoldingVisualTransformation(
    private val collapsedLines: Set<Int>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (collapsedLines.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val originalText = text.text
        val lines = originalText.lines()
        val transformedText = StringBuilder()
        
        val originalToTransformed = IntArray(originalText.length + 1)
        val transformedToOriginal = mutableListOf<Int>()

        var i = 0
        var originalOffset = 0
        var transformedOffset = 0

        while (i < lines.size) {
            val line = lines[i] 
            
            transformedText.append(line)

            for (k in 0..line.length) {
                if (originalOffset + k < originalToTransformed.size) {
                    originalToTransformed[originalOffset + k] = transformedOffset + k
                }
                transformedToOriginal.add(originalOffset + k)
            }

            originalOffset += line.length
            transformedOffset += line.length

            if (i < lines.size - 1) {
                transformedText.append('\n')
                if (originalOffset < originalToTransformed.size) {
                    originalToTransformed[originalOffset] = transformedOffset
                }
                transformedToOriginal.add(originalOffset)
                originalOffset++
                transformedOffset++
            }

            if (collapsedLines.contains(i)) {
                val indent = line.takeWhile { it.isWhitespace() }.length
                val mapTo = if (transformedOffset > 0) transformedOffset - 1 else 0
                i++
                while (i < lines.size && (lines[i].isBlank() || lines[i].takeWhile { it.isWhitespace() }.length > indent)) {
                    val collapsedLine = lines[i]
                    
                    for (k in 0..collapsedLine.length) {
                        if (originalOffset + k < originalToTransformed.size) {
                            originalToTransformed[originalOffset + k] = mapTo
                        }
                    }
                    originalOffset += collapsedLine.length
                    if (i < lines.size - 1) {
                        originalOffset++
                    }
                    i++
                }
            } else {
                i++
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset < originalToTransformed.size) originalToTransformed[offset] else transformedText.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transformedToOriginal.getOrElse(offset) { originalText.length }
            }
        }

        return TransformedText(AnnotatedString(transformedText.toString()), offsetMapping)
    }
}

@Composable
private fun ViewContent(
  uiState: UnifiedCustomListUiState,
  viewModel: UnifiedCustomListViewModel,
  onContentClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .clickable(onClick = onContentClick)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Заголовок
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Text(
        text = uiState.title.ifEmpty { "Без назви" },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(24.dp),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Контент списку - використовуємо Column замість LazyColumn для правильного позиціонування
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      shape = RoundedCornerShape(12.dp),
    ) {
      if (uiState.content.text.isBlank()) {
        // Пусте повідомлення
        Column(
          modifier = Modifier.fillMaxWidth().padding(48.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
          )
          Text(
            text = "Список порожній",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = "Натисніть щоб додати елементи",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          )
        }
      } else {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Обробляємо рядки послідовно без LazyColumn
          processLinesForView(
            lines = uiState.content.text.lines(),
            collapsedLines = uiState.collapsedLines,
            onToggleFold = viewModel::onToggleFold,
          )
        }
      }
    }

    // Додаткова інформація
    if (uiState.content.text.isNotBlank()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Всього елементів: ${uiState.toolbarState.totalItems}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          val collapsedCount = uiState.collapsedLines.size
          if (collapsedCount > 0) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
              Text(
                text = "$collapsedCount згорнуто",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun processLinesForView(
  lines: List<String>,
  collapsedLines: Set<Int>,
  onToggleFold: (Int) -> Unit,
) {
  var i = 0
  while (i < lines.size) {
    val line = lines[i]
    if (line.isBlank() && i == lines.size - 1) {
      i++
      continue
    }

    val indent = line.takeWhile { it.isWhitespace() }.length
    val nextLineIndent =
      if (i + 1 < lines.size) {
        lines[i + 1].takeWhile { it.isWhitespace() }.length
      } else -1
    val isParent = nextLineIndent > indent && !line.isBlank()
    val isCollapsed = collapsedLines.contains(i)

    // Анімований елемент списку
    AnimatedVisibility(
      visible = true,
      enter = slideInVertically() + fadeIn(),
      exit = slideOutVertically() + fadeOut(),
    ) {
      EnhancedListItem(
        text = line.trimStart(),
        indent = indent,
        isParent = isParent,
        isCollapsed = isCollapsed,
        lineIndex = i,
        onClick =
          if (isParent) {
            { onToggleFold(i) }
          } else null,
      )
    }

    if (isCollapsed) {
      // Додаємо індикатор згорнутих елементів
      val hiddenCount = countHiddenLines(lines, i, indent)
      if (hiddenCount > 0) {
        Surface(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(start = (32 + indent * 16).dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ) {
          Text(
            text = "... $hiddenCount згорнутих елементів",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      // Пропускаємо згорнуті рядки
      i++
      while (
        i < lines.size &&
          (lines[i].isBlank() || lines[i].takeWhile { it.isWhitespace() }.length > indent)
      ) {
        i++
      }
    } else {
      i++
    }
  }
}

private fun countHiddenLines(lines: List<String>, parentIndex: Int, parentIndent: Int): Int {
  var count = 0
  var i = parentIndex + 1
  while (
    i < lines.size &&
      (lines[i].isBlank() || lines[i].takeWhile { it.isWhitespace() }.length > parentIndent)
  ) {
    if (lines[i].isNotBlank()) count++
    i++
  }
  return count
}

@Composable
private fun EnhancedListItem(
  text: String,
  indent: Int,
  isParent: Boolean,
  isCollapsed: Boolean,
  lineIndex: Int,
  onClick: (() -> Unit)?,
) {
  val animatedBackgroundColor by
    animateColorAsState(
      targetValue =
        when {
          isParent && isCollapsed -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
          isParent -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
          else -> Color.Transparent
        },
      animationSpec = tween(300),
      label = "background",
    )

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = (indent * 16).dp)
        .let { modifier ->
          if (onClick != null) {
            modifier.clickable(onClick = onClick)
          } else {
            modifier
          }
        }
        .background(
          color = animatedBackgroundColor,
          shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = if (isCollapsed) 12.dp else 8.dp,
            bottomEnd = if (isCollapsed) 12.dp else 8.dp,
          )
        )
        .padding(horizontal = 16.dp, vertical = if (isParent) 16.dp else 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
      // Індикатор типу елементу
      if (isParent) {
        val icon =
          if (isCollapsed) {
            Icons.Default.ChevronRight
          } else {
            Icons.Default.KeyboardArrowDown
          }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
          modifier = Modifier.size(32.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = if (isCollapsed) "Розгорнути" else "Згорнути",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize().padding(6.dp),
          )
        }
      } else {
        // Маркер для звичайних елементів
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
          when {
            text.startsWith("• ") -> {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(8.dp),
              ) {}
            }
            text.matches(Regex("^[0-9]+\\..*")) -> {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                    text = text.takeWhile { it.isDigit() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
            text.startsWith("☐ ") -> {
              Icon(
                imageVector = Icons.Default.CheckBoxOutlineBlank,
                contentDescription = "Не виконано",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
              )
            }
            text.startsWith("☑ ") -> {
              Icon(
                imageVector = Icons.Default.CheckBox,
                contentDescription = "Виконано",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
            else -> {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(6.dp),
              ) {}
            }
          }
        }
      }

      // Текст елементу
      val displayText =
        when {
          text.startsWith("• ") -> text.removePrefix("• ")
          text.startsWith("☐ ") -> text.removePrefix("☐ ")
          text.startsWith("☑ ") -> text.removePrefix("☑ ")
            text.matches(Regex("^[0-9]+\\. .*")) -> text.replaceFirst(Regex("^[0-9]+\\. "), "")
          else -> text
        }

      Text(
        text = displayText,
        style =
          when {
            isParent -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.bodyLarge
          },
        color =
          when {
            isParent && isCollapsed -> MaterialTheme.colorScheme.onTertiaryContainer
            isParent -> MaterialTheme.colorScheme.onSecondaryContainer
            text.startsWith("☑ ") -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
        fontWeight = if (isParent) FontWeight.SemiBold else FontWeight.Normal,
        textDecoration = if (text.startsWith("☑ ")) TextDecoration.LineThrough else null,
        modifier = Modifier.weight(1f),
      )

      // Додаткові індикатори
      if (isCollapsed) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ) {
          Text(
            text = "...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }
}
