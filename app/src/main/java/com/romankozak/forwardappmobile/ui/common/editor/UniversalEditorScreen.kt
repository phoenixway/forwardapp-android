package com.romankozak.forwardappmobile.ui.common.editor

import android.app.Activity
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.screens.customlist.components.EnhancedListToolbar
import com.romankozak.forwardappmobile.ui.screens.customlist.components.ListToolbarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UniversalEditorScreen(
    title: String,
    onSave: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: UniversalEditorViewModel = hiltViewModel(),
)
{
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
    val contentFocusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).imePadding(),
        topBar = {
            EditorTopAppBar(
                title = title,
                isLoading = uiState.isLoading,
                onNavigateBack = onNavigateBack,
                onSave = { onSave(uiState.content.text) },
            )
        },
        bottomBar = {
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
                            onDeleteLine = viewModel::onDeleteLine,
                            onCopyLine = viewModel::onCopyLine,
                            onCutLine = viewModel::onCutLine,
                            onPasteLine = viewModel::onPasteLine,
                            onToggleBullet = viewModel::onToggleBullet,
                            onUndo = viewModel::onUndo,
                            onRedo = viewModel::onRedo,
                            onToggleVisibility = { isToolbarVisible = false },
                        )
                    } else {
                        ShowToolbarButton(onClick = { isToolbarVisible = true })
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Editor(
                content = uiState.content,
                onContentChange = { viewModel.onContentChange(it) },
                contentFocusRequester = contentFocusRequester,
                isToolbarVisible = isToolbarVisible,
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
)
{
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            FilledTonalIconButton(onClick = onSave, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun Editor(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    contentFocusRequester: FocusRequester,
    isToolbarVisible: Boolean,
)
{
    val scrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    )
    {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            onTextLayout = { result ->
                textLayoutResult = result
            },
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(contentFocusRequester)
                .drawBehind {
                    textLayoutResult?.let { layoutResult ->
                        val currentLine = content.selection.start.let { offset ->
                            layoutResult.getLineForOffset(offset)
                        }
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
        )
    }
}

@Composable
private fun ShowToolbarButton(onClick: () -> Unit)
{
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    )
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        )
        {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Show Toolbar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
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
                val checkedRegex = Regex("""^(\s*)\[x\]\s(.*)""", RegexOption.IGNORE_CASE)
                val uncheckedRegex = Regex("""^(\s*)\[\s\]\s(.*)""")

                var matched = false

                if (!matched) {
                    headingRegex.find(line)?.let {
                        val (indent, hashes, content) = it.destructured
                        append(indent)
                        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append(hashes) }
                        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) { append(content) }
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
                        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("$number. ") }
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
                    withStyle(SpanStyle(color = textColor)) { append(line) }
                }

                if (visibleIndex < visibleLines.size - 1) {
                    append("\n")
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
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
