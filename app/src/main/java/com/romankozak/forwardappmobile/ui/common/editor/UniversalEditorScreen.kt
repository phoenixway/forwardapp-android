package com.romankozak.forwardappmobile.ui.common.editor

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.common.components.ShareDialog
import com.romankozak.forwardappmobile.ui.common.editor.components.ExperimentalEnhancedListToolbar
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorEvent
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import kotlinx.coroutines.delay

@Composable
fun UniversalEditorScreen(
  title: String,
  onSave: (content: String, cursorPosition: Int) -> Unit,
  onNavigateBack: () -> Unit,
  navController: NavController,
  viewModel: UniversalEditorViewModel = hiltViewModel(),
  contentFocusRequester: FocusRequester,
  startInEditMode: Boolean = false,
) {
  var isToolbarVisible by remember { mutableStateOf(false) }
  val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
  val view = LocalView.current
  val isDarkTheme = isSystemInDarkTheme()
  val context = LocalContext.current
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
  val snackbarHostState = remember { SnackbarHostState() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val isEditing = uiState.isEditing || startInEditMode
  val readOnly = !isEditing

  LaunchedEffect(isEditing, startInEditMode) {
    if (isEditing) {
      delay(50)
      contentFocusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect {
      when (it) {
        is UniversalEditorEvent.ShowLocation -> {
          val projectId = it.projectId
          android.util.Log.d("ProjectRevealDebug", "Navigating to project screen for projectId: $projectId in ATTACHMENTS mode")
          navController.navigate("goal_detail_screen/$projectId?initialViewMode=${com.romankozak.forwardappmobile.data.database.models.ProjectViewMode.ATTACHMENTS.name}")
        }

        is UniversalEditorEvent.ShowError -> {
          snackbarHostState.showSnackbar(it.message)
        }
      }
    }
  }

  if (uiState.showShareDialog) {
    ShareDialog(
        onDismiss = viewModel::onShareDialogDismiss,
        onCopyToClipboard = viewModel::onCopyAll,
        content = uiState.content.text
    )
  }

  Scaffold(
    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).imePadding(),
    topBar = {
      EditorTopAppBar(
        title = title,
        isLoading = uiState.isLoading,
        onNavigateBack = onNavigateBack,
        onSave = { onSave(uiState.content.text, uiState.content.selection.start) },
        onCopyAll = viewModel::onCopyAll,
        onShare = viewModel::onShare,
        onShowLocation = viewModel::onShowLocation,
        showLocationEnabled = uiState.projectId != null,
      )
    },
    bottomBar = {
      Column {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Box(modifier = Modifier.navigationBarsPadding()) {
          AnimatedContent(
            targetState = isToolbarVisible,
            label = "toolbar_visibility",
            transitionSpec = {
              (slideInVertically { height -> height } + fadeIn()).togetherWith(
                slideOutVertically { height -> height } + fadeOut()
              )
            },
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
                onToggleCheckbox = viewModel::onToggleCheckbox,
                onUndo = viewModel::onUndo,
                onRedo = viewModel::onRedo,
                onToggleVisibility = { isToolbarVisible = false },
                onInsertDateTime = viewModel::onInsertDateTime,
                onInsertTime = viewModel::onInsertTime,
                onH1 = viewModel::onH1,
                onH2 = viewModel::onH2,
                onH3 = viewModel::onH3,
                onBold = viewModel::onBold,
                onItalic = viewModel::onItalic,
                onInsertSeparator = viewModel::onInsertSeparator,
              )
            } else {
              ShowToolbarButton(onClick = { isToolbarVisible = true })
            }
          }
        }
      }
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          val nextMode = !isEditing
          viewModel.setEditingMode(nextMode)
          if (nextMode) {
            contentFocusRequester.requestFocus()
            keyboardController?.show()
          } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
          }
        }
      ) {
        if (isEditing) {
          Icon(Icons.Default.Visibility, contentDescription = "Switch to read mode")
        } else {
          Icon(Icons.Default.Edit, contentDescription = "Switch to edit mode")
        }
      }
    },
  ) { paddingValues ->
    Box(
      modifier =
        Modifier.padding(paddingValues)
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
      Editor(
        content = uiState.content,
        onContentChange = { viewModel.onContentChange(it) },
        onToggleCheckbox = viewModel::onToggleCheckbox,
        contentFocusRequester = contentFocusRequester,
        isToolbarVisible = isToolbarVisible,
        readOnly = readOnly,
      )
      if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopAppBar(
  title: String,
  isLoading: Boolean,
  onNavigateBack: () -> Unit,
  onSave: () -> Unit,
  onCopyAll: () -> Unit,
  onShare: () -> Unit,
  onShowLocation: () -> Unit,
  showLocationEnabled: Boolean,
) {
  TopAppBar(
    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
      IconButton(onClick = onNavigateBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
    },
    actions = {
      if (showLocationEnabled) {
        IconButton(onClick = onShowLocation) {
          Icon(Icons.Default.RemoveRedEye, contentDescription = "Show")
        }
      }
      IconButton(onClick = onCopyAll) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy all")
      }
      IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share") }
      FilledTonalIconButton(onClick = onSave, enabled = !isLoading) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Check, contentDescription = "Save")
        }
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Editor(
  content: TextFieldValue,
  onContentChange: (TextFieldValue) -> Unit,
  onToggleCheckbox: (Int) -> Unit,
  contentFocusRequester: FocusRequester,
  isToolbarVisible: Boolean,
  readOnly: Boolean,
) {
  val scrollState = rememberScrollState()
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val highlightColor = MaterialTheme.colorScheme.surfaceVariant
  val textColor = MaterialTheme.colorScheme.onSurface
  val accentColor = MaterialTheme.colorScheme.primary
  val density = LocalDensity.current

  val imeHeight = WindowInsets.ime.getBottom(density)
  val isImeVisible = imeHeight > 0

  LaunchedEffect(content.selection, textLayoutResult, imeHeight, isToolbarVisible) {
    delay(100)

    textLayoutResult?.let { layoutResult ->
      if (layoutResult.lineCount > 0) {
        val cursorRect = layoutResult.getCursorRect(content.selection.start)
        val extraPadding = with(density) { 24.dp.toPx() }
        val adjustedRect = cursorRect.copy(
          bottom = cursorRect.bottom + extraPadding
        )
        bringIntoViewRequester.bringIntoView(adjustedRect)
      }
    }
  }

  Row(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(scrollState)
        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
  ) {
    BasicTextField(
      value = content,
      onValueChange = onContentChange,
      onTextLayout = { result -> textLayoutResult = result },
      readOnly = readOnly,
      modifier =
        Modifier.padding(start = 16.dp)
          .weight(1f)
          .fillMaxHeight()
          .focusRequester(contentFocusRequester)
          .bringIntoViewRequester(bringIntoViewRequester)
          .focusProperties { canFocus = isEditing }
          .pointerInput(content.text, isEditing) {
              if (!isEditing) return@pointerInput
              detectTapGestures { offset ->
                  textLayoutResult?.let { layoutResult ->
                      val clickedOffset = layoutResult.getOffsetForPosition(offset)
                      val lineIndex = layoutResult.getLineForOffset(clickedOffset)
                      val lines = content.text.lines()

                      if (lineIndex >= lines.size) return@detectTapGestures

                      val line = lines[lineIndex]
                      val trimmedLine = line.trimStart()

                      val checkboxRegex = Regex("""^\s*-\s\[[ x]\]\s?.*""", RegexOption.IGNORE_CASE)
                      if (checkboxRegex.matches(trimmedLine)) {
                          val lineStartOffset = layoutResult.getLineStart(lineIndex)
                          val originalIndentLength = line.takeWhile { it.isWhitespace() }.length

                          val offsetInLine = clickedOffset - lineStartOffset

                          val checkboxStart = originalIndentLength
                          val checkboxEnd = originalIndentLength + 8

                          if (offsetInLine >= checkboxStart && offsetInLine < checkboxEnd) {
                              onToggleCheckbox(lineIndex)
                          }
                      }
                  }
              }
          }
      readOnly = readOnly
          .drawBehind {
            textLayoutResult?.let {
              layoutResult ->
              val currentLine =
                content.selection.start.let { offset -> layoutResult.getLineForOffset(offset) }
              if (currentLine < layoutResult.lineCount) {
                val top = layoutResult.getLineTop(currentLine)
                val bottom = layoutResult.getLineBottom(currentLine)
                drawRect(
                  color = highlightColor,
                  topLeft = Offset(0f, top),
                  size = Size(size.width, bottom - top),
                )
              }
            }
          },
      textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      visualTransformation = ListVisualTransformation(emptySet(), textColor, accentColor),
      readOnly = readOnly,
    )
  }
}

@Composable
private fun ShowToolbarButton(onClick: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shadowElevation = 4.dp,
    tonalElevation = 2.dp,
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .background(
            MaterialTheme.colorScheme.primary.copy(
              alpha = 0.3f
            )
          )
      )
      
      Box(
        modifier = Modifier
          .height(40.dp)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(48.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(
              MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.5f
              )
            )
        )
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

        val headingRegex = Regex("""^(\s*)(#+\s)(.*)""")
        val bulletRegex = Regex("""^(\s*)\*\s(.*)""")
        val numberedRegex = Regex("""^(\s*)(\d+)\.\s(.*)""")
        val checkedRegex = Regex("""^(\s*)-\s\[x\]\s(.*)""", RegexOption.IGNORE_CASE)
        val uncheckedRegex = Regex("""^(\s*)-\s\[\s\]\s(.*)""")
        val boldRegex = Regex("""(\*\*|__)(.*?)\1""")
        val italicRegex = Regex("""(\*|_)(.*?)\1""")

        var matched = false

        if (!matched) {
          headingRegex.find(line)?.let {
            val (indent, hashes, content) = it.destructured
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
              append(hashes)
            }
            withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) {
              append(content)
            }
            matched = true
          }
        }

        if (!matched) {
          bulletRegex.find(line)?.let {
            val (indent, content) = it.destructured
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("• ") }
            withStyle(SpanStyle(color = textColor)) { append(content) }
            matched = true
          }
        }
        
        if (!matched) {
          numberedRegex.find(line)?.let {
            val (indent, number, content) = it.destructured
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
              append("$number. ")
            }
            withStyle(SpanStyle(color = textColor)) { append(content) }
            matched = true
          }
        }
        
        if (!matched) {
          uncheckedRegex.find(line)?.let {
            val (indent, content) = it.destructured
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☐ ") }
            withStyle(SpanStyle(color = textColor)) { append(content) }
            matched = true
          }
        }
        
        if (!matched) {
          checkedRegex.find(line)?.let {
            val (indent, content) = it.destructured
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☑ ") }
            withStyle(SpanStyle(color = textColor)) { append(content) }
            matched = true
          }
        }

        if (!matched) {
          val annotatedString = buildAnnotatedString {
            append(line)
            boldRegex.findAll(line).forEach { matchResult ->
              addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
              )
            }
            italicRegex.findAll(line).forEach { matchResult ->
              addStyle(
                style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
              )
            }
          }
          append(annotatedString)
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
