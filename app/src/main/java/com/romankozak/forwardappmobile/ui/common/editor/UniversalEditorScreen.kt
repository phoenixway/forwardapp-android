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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import android.util.Log
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
  onAutoSave: ((content: String, cursorPosition: Int) -> Unit)? = null,
  onNavigateBack: () -> Unit,
  onWikiLinkClick: (String) -> Unit = {},
  linkSuggestions: List<String> = emptyList(),
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
  var lastSavedText by remember { mutableStateOf("") }
  var isDirty by remember { mutableStateOf(false) }
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
  var initialEditApplied by remember { mutableStateOf(false) }
  val isEditing = uiState.isEditing || (startInEditMode && !initialEditApplied)
  val readOnly = !isEditing

  LaunchedEffect(startInEditMode) {
    if (startInEditMode) {
      viewModel.setEditingMode(true)
    }
    initialEditApplied = true
  }

  LaunchedEffect(isEditing) {
    if (isEditing) {
      delay(50)
      contentFocusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  LaunchedEffect(uiState.content.text) {
    if (lastSavedText.isEmpty()) {
      lastSavedText = uiState.content.text
    }
    isDirty = uiState.content.text != lastSavedText
    val currentText = uiState.content.text
    delay(2000)
    if (currentText == uiState.content.text && isDirty) {
      onAutoSave?.invoke(uiState.content.text, uiState.content.selection.start)
      onAutoSave?.let {
        lastSavedText = uiState.content.text
        isDirty = false
      }
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
      val handleSave = {
        onSave(uiState.content.text, uiState.content.selection.start)
        lastSavedText = uiState.content.text
        isDirty = false
      }
      EditorTopAppBar(
        title = title,
        isLoading = uiState.isLoading,
        isSaveEnabled = isDirty && !uiState.isLoading,
        onNavigateBack = onNavigateBack,
        onSave = handleSave,
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
        onWikiLinkClick = onWikiLinkClick,
        linkSuggestions = linkSuggestions,
        contentFocusRequester = contentFocusRequester,
        isToolbarVisible = isToolbarVisible,
        readOnly = readOnly,
        isEditing = isEditing
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
  isSaveEnabled: Boolean,
  onNavigateBack: () -> Unit,
  onSave: () -> Unit,
  onCopyAll: () -> Unit,
  onShare: () -> Unit,
  onShowLocation: () -> Unit,
  showLocationEnabled: Boolean,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  TopAppBar(
    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
      IconButton(onClick = onNavigateBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
    },
    actions = {
      FilledTonalIconButton(onClick = onSave, enabled = isSaveEnabled) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Check, contentDescription = "Save")
        }
      }
      IconButton(onClick = { menuExpanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More")
      }
      DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
        if (showLocationEnabled) {
          DropdownMenuItem(
            text = { Text("Show location") },
            leadingIcon = { Icon(Icons.Default.RemoveRedEye, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShowLocation()
            },
          )
        }
        DropdownMenuItem(
          text = { Text("Copy all") },
          leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
          onClick = {
            menuExpanded = false
            onCopyAll()
          },
        )
        DropdownMenuItem(
          text = { Text("Share") },
          leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
          onClick = {
            menuExpanded = false
            onShare()
          },
        )
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
  onWikiLinkClick: (String) -> Unit,
  linkSuggestions: List<String>,
  contentFocusRequester: FocusRequester,
  isToolbarVisible: Boolean,
  readOnly: Boolean,
  isEditing: Boolean,
) {
  val scrollState = rememberScrollState()
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val highlightColor = MaterialTheme.colorScheme.surfaceVariant
  val textColor = MaterialTheme.colorScheme.onSurface
  val accentColor = MaterialTheme.colorScheme.primary
  val density = LocalDensity.current
  val focusManager = LocalFocusManager.current

  val imeHeight = WindowInsets.ime.getBottom(density)
  val isImeVisible = imeHeight > 0

  data class InlineQuery(val start: Int, val prefix: String, val query: String)

  fun extractInlineQuery(text: String, cursor: Int): InlineQuery? {
    val before = text.take(cursor)
    val lastLink = before.lastIndexOf("[[")
    val lastTag = before.lastIndexOf("#")
    val lastContext = before.lastIndexOf("@")
    val candidates =
      listOf(
        "link" to lastLink,
        "tag" to lastTag,
        "context" to lastContext,
      ).filter { it.second >= 0 }
    if (candidates.isEmpty()) return null
    val (type, start) = candidates.maxByOrNull { it.second } ?: return null
    val remainder = before.substring(start + if (type == "link") 2 else 1)
    if (remainder.contains("\n")) return null
    when (type) {
      "link" -> {
        val hasClose = before.indexOf("]]", startIndex = start) != -1
        if (hasClose) return null
        return InlineQuery(start, "[[", remainder)
      }
      "tag" -> {
        if (start > 0 && !before[start - 1].isWhitespace()) return null
        if (remainder.length < 1) return null
        return InlineQuery(start, "#", remainder)
      }
      "context" -> {
        if (start > 0 && !before[start - 1].isWhitespace()) return null
        if (remainder.length < 1) return null
        return InlineQuery(start, "@", remainder)
      }
    }
    return null
  }

  val activeQuery = remember(content.text, content.selection) {
    extractInlineQuery(content.text, content.selection.start)
  }

  val filteredSuggestions =
    remember(activeQuery, linkSuggestions) {
      activeQuery?.let { q ->
        if (q.query.length < 3) emptyList()
        else linkSuggestions.filter { it.contains(q.query, ignoreCase = true) }.take(6)
      } ?: emptyList()
    }

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

  Box(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(scrollState)
          .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
      val visualTransformation =
        if (readOnly) ListVisualTransformation(emptySet(), textColor, accentColor)
        else VisualTransformation.None

      val baseModifier =
        Modifier.padding(start = 16.dp)
          .weight(1f)
          .fillMaxHeight()
          .focusRequester(contentFocusRequester)
          .bringIntoViewRequester(bringIntoViewRequester)
          .focusProperties { canFocus = isEditing }

      if (readOnly) {
        val transformed = visualTransformation.filter(AnnotatedString(content.text))
        ClickableText(
          text = transformed.text,
          modifier = baseModifier,
          style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
          onClick = { clickOffset: Int ->
              val annotation =
                transformed.text.getStringAnnotations("wikilink", clickOffset, clickOffset).firstOrNull()
                  ?: transformed.text.getStringAnnotations("tag", clickOffset, clickOffset).firstOrNull()
                  ?: transformed.text.getStringAnnotations("context", clickOffset, clickOffset).firstOrNull()
            when (annotation?.tag) {
              "wikilink" -> onWikiLinkClick(annotation.item)
              "tag" -> onWikiLinkClick("#${annotation.item}")
              "context" -> onWikiLinkClick("@${annotation.item}")
            }
          },
        )
      } else {
        BasicTextField(
          value = content,
          onValueChange = onContentChange,
          onTextLayout = { result -> textLayoutResult = result },
          modifier =
            baseModifier.pointerInput(content.text, isEditing) {
              detectTapGestures { offset ->
                val layout = textLayoutResult ?: return@detectTapGestures

                val clickedOffset = layout.getOffsetForPosition(offset)
                val lineIndex = layout.getLineForOffset(clickedOffset)
                val lines = content.text.lines()

                if (lineIndex >= lines.size) return@detectTapGestures

                val line = lines[lineIndex]
                val trimmedLine = line.trimStart()

                val checkboxRegex = Regex("""^\s*-\s\[[ x]\]\s?.*""", RegexOption.IGNORE_CASE)
                if (checkboxRegex.matches(trimmedLine)) {
                  val lineStartOffset = layout.getLineStart(lineIndex)
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
            .drawBehind {
              if (!isEditing) return@drawBehind
              textLayoutResult?.let { layoutResult ->
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
          visualTransformation = visualTransformation,
          readOnly = false,
          interactionSource = remember { MutableInteractionSource() },
          singleLine = false,
          maxLines = Int.MAX_VALUE,
        )
      }
    }

    if (!readOnly && filteredSuggestions.isNotEmpty() && activeQuery != null) {
      Card(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
      ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          filteredSuggestions.forEach { suggestion ->
            Text(
              text = suggestion,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable {
                    val query = activeQuery ?: return@clickable
                    val cursor = content.selection.start
                    val newText = buildString {
                      append(content.text.substring(0, query.start))
                      when (query.prefix) {
                        "[[" -> {
                          append("[[")
                          append(suggestion)
                          append("]]")
                        }
                        "#" -> {
                          append("#")
                          append(suggestion)
                          append(" ")
                        }
                        "@" -> {
                          append("@")
                          append(suggestion)
                          append(" ")
                        }
                      }
                      append(content.text.substring(cursor))
                    }
                    val newCursor =
                      when (query.prefix) {
                        "[[" -> query.start + 2 + suggestion.length + 2
                        "#" -> query.start + 1 + suggestion.length + 1
                        "@" -> query.start + 1 + suggestion.length + 1
                        else -> cursor
                      }
                    onContentChange(TextFieldValue(newText, TextRange(newCursor)))
                  }
                  .padding(horizontal = 6.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
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
    data class LineInfo(val originalIndex: Int, val transformedLength: Int)
    val visibleLines = mutableListOf<IndexedValue<String>>()
    val lineInfos = mutableListOf<LineInfo>()
    val wikiLinkRegex = Regex("\\[\\[([^\\[\\]]+)\\]\\]")
    val tagRegex = Regex("#(\\w+)")
    val contextRegex = Regex("@(\\w+)")

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
        val (originalIndex, line) = indexedValue
        val lineStart = length

        val headingRegex = Regex("""^(\s*)(#+\s)(.*)""")
        val bulletRegex = Regex("""^(\s*)\*\s(.*)""")
        val numberedRegex = Regex("""^(\s*)(\d+)\.\s(.*)""")
        val checkedRegex = Regex("""^(\s*)-\s\[x\]\s(.*)""", RegexOption.IGNORE_CASE)
        val uncheckedRegex = Regex("""^(\s*)-\s\[\s\]\s(.*)""")
        val boldRegex = Regex("""(\*\*|__)(.*?)\1""")
        val italicRegex = Regex("""(\*|_)(.*?)\1""")
        val allInlineRegex = listOf(
          "wikilink" to wikiLinkRegex,
          "tag" to tagRegex,
          "context" to contextRegex,
        )

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
            var cursor = 0
            while (cursor < line.length) {
              val nextMatch =
                allInlineRegex
                  .mapNotNull { (tag, regex) -> regex.find(line, startIndex = cursor)?.let { tag to it } }
                  .minByOrNull { it.second.range.first }
              if (nextMatch == null) {
                append(line.substring(cursor))
                break
              }
              val (tag, match) = nextMatch
              if (match.range.first > cursor) {
                append(line.substring(cursor, match.range.first))
              }
              val contentText = match.groupValues[1]
              val start = length
              when (tag) {
                "wikilink" -> {
                  append(contentText)
                }
                "tag" -> append("#$contentText")
                "context" -> append("@$contentText")
              }
              val end = length
              val style =
                when (tag) {
                  "wikilink" -> SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)
                  "tag" -> SpanStyle(color = Color(0xFF0D47A1), fontWeight = FontWeight.Medium)
                  "context" -> SpanStyle(color = Color(0xFF00695C), fontWeight = FontWeight.Medium)
                  else -> SpanStyle(color = textColor)
                }
              addStyle(style, start = start, end = end)
              addStringAnnotation(tag = tag, annotation = contentText, start = start, end = end)
              cursor = match.range.last + 1
            }

            boldRegex.findAll(toString()).forEach { matchResult ->
              addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
              )
            }
            italicRegex.findAll(toString()).forEach { matchResult ->
              addStyle(
                style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
              )
            }
          }
          append(annotatedString)
        }

        val lineEnd = length
        val transformedLengthForLine = lineEnd - lineStart
        lineInfos.add(LineInfo(originalIndex, transformedLengthForLine))
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
          var lineTransformedLength = 0
          for (i in lineInfos.indices) {
            val info = lineInfos[i]
            if (info.originalIndex == originalLineIndex) {
              found = true
              lineTransformedLength = info.transformedLength
              break
            }
            transformedLineStart += lineInfos[i].transformedLength + 1
          }
          if (!found) return transformedText.length
          val adjustedChar = charInLine.coerceAtMost(lineTransformedLength)
          return (transformedLineStart + adjustedChar).coerceIn(0, transformedText.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
          if (offset <= 0) return 0
          val prefix = transformedText.substring(0, offset)
          val parts = prefix.lines()
          val transformedLineIndex = parts.size - 1
          val charInLine = parts.lastOrNull()?.length ?: 0
          if (transformedLineIndex >= visibleLines.size) return originalText.length
          val originalLineIndex = lineInfos[transformedLineIndex].originalIndex
          val originalLineStart = lines.take(originalLineIndex).sumOf { it.length + 1 }
          return (originalLineStart + charInLine).coerceIn(0, originalText.length)
        }
      }

    return TransformedText(transformedText, offsetMapping)
  }
}
