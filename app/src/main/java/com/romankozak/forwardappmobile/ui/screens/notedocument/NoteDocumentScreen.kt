package com.romankozak.forwardappmobile.ui.screens.notedocument

import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.editor.components.ExperimentalEnhancedListToolbar
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenMode {
  CREATE,
  EDIT_EXISTING,
  VIEW,
}

@Composable
private fun ShowToolbarButton(onClick: () -> Unit) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Показати тулбар",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDocumentScreen(
  navController: NavController,
  viewModel: NoteDocumentViewModel = hiltViewModel(),
) {
  var isToolbarVisible by remember { mutableStateOf(true) }
  val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
  val view = LocalView.current
  val isDarkTheme = isSystemInDarkTheme()

  if (!view.isInEditMode) {
    LaunchedEffect(Unit) {
      val window = (view.context as Activity).window
      WindowCompat.setDecorFitsSystemWindows(window, false)

      @Suppress("DEPRECATION")
      window.statusBarColor = Color.Transparent.toArgb()
      @Suppress("DEPRECATION")
      window.navigationBarColor = Color.Transparent.toArgb()

      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = topBarContainerColor.luminance() > 0.5
      insetsController.isAppearanceLightNavigationBars = !isDarkTheme
    }
  }

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val startEditArg = navController.currentBackStackEntry?.arguments?.getBoolean("startEdit") ?: false
  var screenMode by remember {
    mutableStateOf(
      when {
        startEditArg -> ScreenMode.EDIT_EXISTING
        viewModel.isNewDocument -> ScreenMode.EDIT_EXISTING
        else -> ScreenMode.VIEW
      }
    )
  }

  val keyboardController = LocalSoftwareKeyboardController.current
  val contentFocusRequester = remember { FocusRequester() }

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

  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  LaunchedEffect(screenMode) { viewModel.onToggleEditMode(screenMode != ScreenMode.VIEW) }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event: NoteDocumentEvent ->
      when (event) {
        is NoteDocumentEvent.NavigateBack -> {
          navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
          navController.popBackStack()
        }
        is NoteDocumentEvent.ShowError -> {}
        is NoteDocumentEvent.ShowSuccess -> {}
        is NoteDocumentEvent.AutoSaved -> {}
      }
    }
  }

  LaunchedEffect(screenMode) {
    when (screenMode) {
      ScreenMode.CREATE, ScreenMode.EDIT_EXISTING -> contentFocusRequester.requestFocus()
      ScreenMode.VIEW -> keyboardController?.hide()
    }
  }

  Scaffold(
    modifier = Modifier.background(animatedBackgroundColor).imePadding(),
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
      if (screenMode != ScreenMode.VIEW) {
        Box(modifier = Modifier.navigationBarsPadding()) {
          AnimatedContent(
            targetState = isToolbarVisible,
            label = "toolbar_visibility",
            transitionSpec = {
              (slideInVertically { height -> height } + fadeIn())
                .togetherWith(slideOutVertically { height -> height } + fadeOut())
            }
          ) { isVisible ->
            if (isVisible) {
              ExperimentalEnhancedListToolbar(
                state = uiState.toolbarState,
                onIndentBlock = viewModel::onIndentBlock,
                onDeIndentBlock = viewModel::onDeIndentBlock,
                onMoveBlockUp = viewModel::onMoveBlockUp,
                onMoveBlockDown = viewModel::onMoveBlockDown,
                onIndentLine = viewModel::onIndentLine,
                onDeIndentLine = viewModel::onDeIndentLine,
                onMoveLineUp = viewModel::onMoveLineUp,
                onMoveLineDown = viewModel::onMoveLineDown,
                onDeleteLine = viewModel::onDeleteLine,
                onCopyLine = viewModel::onCopyLine,
                onCutLine = viewModel::onCutLine,
                onPasteLine = viewModel::onPasteLine,
                onToggleBullet = viewModel::onToggleBullet,
                onToggleCheckbox = {},
                onUndo = viewModel::onUndo,
                onRedo = viewModel::onRedo,
                onToggleVisibility = { isToolbarVisible = !isToolbarVisible },
              )
            } else {
              ShowToolbarButton(onClick = { isToolbarVisible = true })
            }
          }
        }
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
          modifier = Modifier.navigationBarsPadding().clip(RoundedCornerShape(16.dp)),
        )
      }
    },
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize()
        .background(animatedBackgroundColor)
    ) {
      when (screenMode) {
        ScreenMode.CREATE,
        ScreenMode.EDIT_EXISTING -> {
          CreateEditContent(
            uiState = uiState,
            viewModel = viewModel,
            contentFocusRequester = contentFocusRequester,
            bringIntoViewRequester = bringIntoViewRequester,
            isToolbarVisible = isToolbarVisible,
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
              text = title.ifEmpty { "Новий список" },
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
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
          ScreenMode.CREATE -> {}
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
      windowInsets = WindowInsets.statusBars,
    )
  }
}

@Composable
private fun CreateEditContent(
  uiState: NoteDocumentUiState,
  viewModel: NoteDocumentViewModel,
  contentFocusRequester: FocusRequester,
  bringIntoViewRequester: BringIntoViewRequester,
  isToolbarVisible: Boolean,
) {
  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

  LaunchedEffect(uiState.content.selection, textLayoutResult, isToolbarVisible) {
      if (!isToolbarVisible) return@LaunchedEffect
      val layoutResult = textLayoutResult ?: return@LaunchedEffect
      val cursorRect = layoutResult.getCursorRect(uiState.content.selection.start)

      val paddedRect = cursorRect.copy(
          top = (cursorRect.top - 150).coerceAtLeast(0f),
          bottom = (cursorRect.bottom + 150)
      )

      coroutineScope.launch {
          delay(350) // Wait for animations
          bringIntoViewRequester.bringIntoView(paddedRect)
      }
  }

  val highlightColor = MaterialTheme.colorScheme.surfaceVariant
  val textColor = MaterialTheme.colorScheme.onSurface
  val accentColor = MaterialTheme.colorScheme.primary

  Row(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(start = 16.dp, end = 16.dp, top = 16.dp)
  ) {
    Gutter(
      lines = uiState.content.text.lines(),
      collapsedLines = uiState.collapsedLines,
      onToggleFold = viewModel::onToggleFold,
      lineHeight = 24.sp
    )

   BasicTextField(
    value = uiState.content,
    onValueChange = { newValue ->
        val firstLine = newValue.text.lines().firstOrNull() ?: ""
        Log.d("TitleFormation", "Original first line: '$firstLine'")
val markerRegex = Regex(
    """^(\s*)(\*|•|\d+\.|\[\s*x?\])\s*""",
    RegexOption.IGNORE_CASE
)
        val title = firstLine.replaceFirst(markerRegex, "").trim()
        Log.d("TitleFormation", "Cleaned title: '$title'")
        viewModel.onTitleChange(title)

        val oldValue = uiState.content
        if (
            newValue.text.length > oldValue.text.length &&
            newValue.text.count { it == '\n' } > oldValue.text.count { it == '\n' }
        ) {
          viewModel.onEnter(newValue)
        } else {
          viewModel.onContentChange(newValue)
        }
    },
      onTextLayout = { result ->
        textLayoutResult = result
      },
      modifier = Modifier
        .padding(start = 16.dp)
        .weight(1f)
        .fillMaxHeight()
        .focusRequester(contentFocusRequester)
        .bringIntoViewRequester(bringIntoViewRequester)
        .drawBehind {
          uiState.currentLine?.let { line ->
            textLayoutResult?.let { layoutResult ->
              if (line < layoutResult.lineCount) {
                val top = layoutResult.getLineTop(line)
                val bottom = layoutResult.getLineBottom(line)
                drawRect(
                  color = highlightColor,
                  topLeft = Offset(0f, top),
                  size = Size(size.width, bottom - top)
                )
              }
            }
          }
        },
      textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      visualTransformation = ListVisualTransformation(uiState.collapsedLines, textColor, accentColor),
    )
  }
}

@Composable
private fun Gutter(lines: List<String>, collapsedLines: Set<Int>, onToggleFold: (Int) -> Unit, lineHeight: TextUnit) {
  val focusManager = LocalFocusManager.current
  val lineHeightDp = with(LocalDensity.current) { lineHeight.toDp() }
  Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    lines.forEachIndexed { index, line ->
      val indent = line.takeWhile { it.isWhitespace() }.length
      val nextIndent =
        if (index + 1 < lines.size) lines[index + 1].takeWhile { it.isWhitespace() }.length else -1
      val isParent = nextIndent > indent && line.isNotBlank()

      Box(modifier = Modifier.height(lineHeightDp), contentAlignment = Alignment.Center) {
        if (isParent) {
          val isCollapsed = collapsedLines.contains(index)
          val icon =
            if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown
          Icon(
            imageVector = icon,
            contentDescription = if (isCollapsed) "Expand" else "Collapse",
            modifier =
              Modifier.size(16.dp).clickable {
                focusManager.clearFocus()
                onToggleFold(index)
              },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

private class ListVisualTransformation(
  private val collapsedLines: Set<Int>,
  private val textColor: Color,
  private val accentColor: Color,
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val originalText = text.text
    val lines = originalText.lines()
    val visibleLines = mutableListOf<IndexedValue<String>>()

    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      visibleLines.add(IndexedValue(i, line))
      if (collapsedLines.contains(i)) {
        val indent = line.takeWhile { it.isWhitespace() }.length
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

    val transformedText = buildAnnotatedString {
      visibleLines.forEachIndexed { visibleIndex, indexedValue ->
        val (_, line) = indexedValue

val headingRegex   = Regex("""^(\s*)(#+\s)(.*)""")
val bulletRegex    = Regex("""^(\s*)\*\s(.*)""")
val numberedRegex  = Regex("""^(\s*)(\d+)\.\s(.*)""")
val checkedRegex   = Regex("""^(\s*)\[x\]\s(.*)""", RegexOption.IGNORE_CASE)
val uncheckedRegex = Regex("""^(\s*)\[\s\]\s(.*)""")



        var matched = false

        if (!matched) headingRegex.find(line)?.let {
          val (indent, hashes, content) = it.destructured
          append(indent)
          withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append(hashes) }
          withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) { append(content) }
          matched = true
        }

        if (!matched) bulletRegex.find(line)?.let {
          val (indent, content) = it.destructured
          append(indent)
          withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("• ") }
          withStyle(SpanStyle(color = textColor)) { append(content) }
          matched = true
        }
        if (!matched) numberedRegex.find(line)?.let {
          val (indent, number, content) = it.destructured
          append(indent)
          withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("$number. ") }
          withStyle(SpanStyle(color = textColor)) { append(content) }
          matched = true
        }
        if (!matched) uncheckedRegex.find(line)?.let {
          val (indent, content) = it.destructured
          append(indent)
          withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☐ ") }
          withStyle(SpanStyle(color = textColor)) { append(content) }
          matched = true
        }
        if (!matched) checkedRegex.find(line)?.let {
          val (indent, content) = it.destructured
          append(indent)
          withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☑ ") }
          withStyle(SpanStyle(color = textColor)) { append(content) }
          matched = true
        }

        if (!matched) {
          withStyle(SpanStyle(color = textColor)) { append(line) }
        }

        if (visibleIndex < visibleLines.size - 1) {
          append("\n")
        }
      }
    }

    val offsetMapping =
      object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
          if (offset <= 0) return 0
          val prefix = originalText.substring(0, offset)
          val parts = prefix.lines()
          val originalLineIndex = parts.size - 1
          val charInLine = parts.lastOrNull()?.length ?: 0

          var transformedLineStart = 0
          var found = false
          for (v in visibleLines) {
            if (v.index == originalLineIndex) {
              found = true
              break
            }
            transformedLineStart += v.value.length + 1
          }
          if (!found) return transformedText.length
          return (transformedLineStart + charInLine).coerceIn(0, transformedText.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
          if (offset <= 0) return 0
          val prefix = transformedText.substring(0, offset)
          val parts = prefix.lines()
          val transformedLineIndex = parts.size - 1
          val charInLine = parts.lastOrNull()?.length ?: 0
          if (transformedLineIndex >= visibleLines.size) return originalText.length
          val originalLineIndex = visibleLines[transformedLineIndex].index
          val originalLineStart = lines.take(originalLineIndex).sumOf { it.length + 1 }
          return (originalLineStart + charInLine).coerceIn(0, originalText.length)
        }
      }

    return TransformedText(transformedText, offsetMapping)
  }
}

@Composable
private fun ViewContent(
  uiState: NoteDocumentUiState,
  viewModel: NoteDocumentViewModel,
  onContentClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .clickable { onContentClick() }
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    if (uiState.content.text.isBlank()) {
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
      val annotatedString = buildAnnotatedString {
        val lines = uiState.content.text.lines()
        var i = 0
        while (i < lines.size) {
          val line = lines[i]
          val indent = line.takeWhile { it.isWhitespace() }.length
          val isCollapsed = uiState.collapsedLines.contains(i)

          withStyle(
            style =
              SpanStyle(background = if (isCollapsed) Color.LightGray else Color.Transparent)
          ) {
            append(line)
          }
          append("\n")

          if (isCollapsed) {
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
      Text(
        text = annotatedString,
        style = TextStyle(lineHeight = 20.sp),
      )
    }
    // Додатковий відступ знизу, щоб контент не ховався за панеллю навігації
    Spacer(modifier = Modifier.navigationBarsPadding())
  }
}
