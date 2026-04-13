@file:Suppress("TooManyFunctions")

package com.romankozak.forwardappmobile.ui.common.editor

import android.app.Activity
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.ui.common.components.ShareDialog
import com.romankozak.forwardappmobile.ui.common.editor.components.ExperimentalEnhancedListToolbar
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorEvent
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import kotlinx.coroutines.delay

private data class UniversalEditorConfig(
    val title: String,
    val startInEditMode: Boolean,
    val foldingPersistenceKey: String?,
    val useFirstLineAsTitle: Boolean,
)

private data class UniversalEditorCallbacks(
    val onSave: (content: String, cursorPosition: Int) -> Unit,
    val onAutoSave: ((content: String, cursorPosition: Int) -> Unit)?,
    val onNavigateBack: () -> Unit,
    val onWikiLinkClick: (String) -> Unit,
)

private data class UniversalEditorDependencies(
    val navController: NavController,
    val navigationManager: EnhancedNavigationManager?,
    val viewModel: UniversalEditorViewModel,
    val contentFocusRequester: FocusRequester,
)

private data class InlineQuery(
    val start: Int,
    val prefix: String,
    val query: String,
)

private data class EditorReadModeState(
    val source: String,
    val documentTitle: String,
    val showDocumentTitle: Boolean,
    val transformedText: AnnotatedString,
    val collapsedHeadingLines: List<Int>,
    val onToggleHeading: (Int) -> Unit,
    val onAnnotationClick: (AnnotatedString.Range<String>?) -> Unit,
)

private data class EditorEditModeState(
    val content: TextFieldValue,
    val isEditing: Boolean,
    val highlightColor: Color,
    val textColor: Color,
    val textLayoutResult: TextLayoutResult?,
)

private data class InlineSuggestionsState(
    val activeQuery: InlineQuery?,
    val suggestions: List<String>,
)

private data class EditorAutoSaveArgs(
    val content: TextFieldValue,
    val lastSavedText: String,
    val onLastSavedTextChange: (String) -> Unit,
    val isDirty: Boolean,
    val onDirtyChange: (Boolean) -> Unit,
    val onAutoSave: ((content: String, cursorPosition: Int) -> Unit)?,
)

private data class EditorDialogsArgs(
    val uiState: com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorUiState,
    val showAttachmentPicker: Boolean,
    val onShowAttachmentPickerChange: (Boolean) -> Unit,
    val showContextPicker: Boolean,
    val onShowContextPickerChange: (Boolean) -> Unit,
    val linkSuggestions: List<String>,
    val contextSuggestions: List<String>,
    val onContentChange: (TextFieldValue) -> Unit,
    val onShareDialogDismiss: () -> Unit,
    val onCopyAll: () -> Unit,
)

private data class EditorBottomBarArgs(
    val snackbarHostState: SnackbarHostState,
    val isEditing: Boolean,
    val isToolbarVisible: Boolean,
    val onToolbarVisibleChange: (Boolean) -> Unit,
    val uiState: com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorUiState,
    val viewModel: UniversalEditorViewModel,
    val linkSuggestions: List<String>,
    val contextSuggestions: List<String>,
    val onShowAttachmentPicker: () -> Unit,
    val onShowContextPicker: () -> Unit,
)

private data class EditorScaffoldContentArgs(
    val paddingValues: PaddingValues,
    val uiState: com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorUiState,
    val onContentChange: (TextFieldValue) -> Unit,
    val onToggleCheckbox: (Int) -> Unit,
    val onWikiLinkClick: (String) -> Unit,
    val linkSuggestions: List<String>,
    val contextSuggestions: List<String>,
    val contentFocusRequester: FocusRequester,
    val isToolbarVisible: Boolean,
    val readOnly: Boolean,
    val isEditing: Boolean,
    val foldingPersistenceKey: String?,
    val useFirstLineAsTitle: Boolean,
)

private data class EditorTopAppBarArgs(
    val title: String,
    val isLoading: Boolean,
    val isSaveEnabled: Boolean,
    val onNavigateBack: () -> Unit,
    val onSave: () -> Unit,
    val onCopyAll: () -> Unit,
    val onShare: () -> Unit,
    val onShowLocation: () -> Unit,
    val showLocationEnabled: Boolean,
)

private data class EditorArgs(
    val content: TextFieldValue,
    val onContentChange: (TextFieldValue) -> Unit,
    val onToggleCheckbox: (Int) -> Unit,
    val onWikiLinkClick: (String) -> Unit,
    val linkSuggestions: List<String>,
    val contextSuggestions: List<String>,
    val contentFocusRequester: FocusRequester,
    val isToolbarVisible: Boolean,
    val readOnly: Boolean,
    val isEditing: Boolean,
    val foldingPersistenceKey: String?,
    val useFirstLineAsTitle: Boolean,
)

private data class EditorDerivedState(
    val activeQuery: InlineQuery?,
    val filteredSuggestions: List<String>,
    val documentTitle: String,
    val showDocumentTitle: Boolean,
    val readModeSource: String,
    val sanitizedCollapsedHeadingLines: Set<Int>,
    val onToggleHeading: (Int) -> Unit,
    val textColor: Color,
    val accentColor: Color,
)

@Composable
@Suppress("LongParameterList")
fun UniversalEditorScreen(
    title: String,
    onSave: (content: String, cursorPosition: Int) -> Unit,
    onAutoSave: ((content: String, cursorPosition: Int) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onWikiLinkClick: (String) -> Unit = {},
    linkSuggestions: List<String> = emptyList(),
    contextSuggestions: List<String> = emptyList(),
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: UniversalEditorViewModel = hiltViewModel(),
    contentFocusRequester: FocusRequester,
    startInEditMode: Boolean = false,
    foldingPersistenceKey: String? = null,
    useFirstLineAsTitle: Boolean = true,
) {
    UniversalEditorRoute(
        config =
            UniversalEditorConfig(
                title = title,
                startInEditMode = startInEditMode,
                foldingPersistenceKey = foldingPersistenceKey,
                useFirstLineAsTitle = useFirstLineAsTitle,
            ),
        callbacks =
            UniversalEditorCallbacks(
                onSave = onSave,
                onAutoSave = onAutoSave,
                onNavigateBack = onNavigateBack,
                onWikiLinkClick = onWikiLinkClick,
            ),
        dependencies =
            UniversalEditorDependencies(
                navController = navController,
                navigationManager = navigationManager,
                viewModel = viewModel,
                contentFocusRequester = contentFocusRequester,
            ),
        linkSuggestions = linkSuggestions,
        contextSuggestions = contextSuggestions,
    )
}

@Composable
@Suppress("LongMethod")
private fun UniversalEditorRoute(
    config: UniversalEditorConfig,
    callbacks: UniversalEditorCallbacks,
    dependencies: UniversalEditorDependencies,
    linkSuggestions: List<String>,
    contextSuggestions: List<String>,
) {
    var isToolbarVisible by remember { mutableStateOf(false) }
    val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    var lastSavedText by remember { mutableStateOf("") }
    var isDirty by remember { mutableStateOf(false) }
    ConfigureEditorSystemBars(
        view = view,
        topBarContainerColor = topBarContainerColor,
        isDarkTheme = isDarkTheme,
    )

    val uiState by dependencies.viewModel.uiState.collectAsStateWithLifecycle()
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showContextPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var initialEditApplied by remember { mutableStateOf(false) }
    val isEditing = uiState.isEditing || (config.startInEditMode && !initialEditApplied)
    val readOnly = !isEditing

    HandleStartInEditMode(
        startInEditMode = config.startInEditMode,
        initialEditApplied = initialEditApplied,
        onInitialEditApplied = { initialEditApplied = it },
        setEditingMode = dependencies.viewModel::setEditingMode,
    )
    HandleEditingFocus(
        isEditing = isEditing,
        contentFocusRequester = dependencies.contentFocusRequester,
        keyboardController = keyboardController,
    )
    HandleEditorAutoSave(
        args =
            EditorAutoSaveArgs(
                content = uiState.content,
                lastSavedText = lastSavedText,
                onLastSavedTextChange = { lastSavedText = it },
                isDirty = isDirty,
                onDirtyChange = { isDirty = it },
                onAutoSave = callbacks.onAutoSave,
            ),
    )
    HandleEditorEvents(
        viewModel = dependencies.viewModel,
        navController = dependencies.navController,
        navigationManager = dependencies.navigationManager,
        snackbarHostState = snackbarHostState,
    )

    UniversalEditorDialogs(
        args =
            EditorDialogsArgs(
                uiState = uiState,
                showAttachmentPicker = showAttachmentPicker,
                onShowAttachmentPickerChange = { showAttachmentPicker = it },
                showContextPicker = showContextPicker,
                onShowContextPickerChange = { showContextPicker = it },
                linkSuggestions = linkSuggestions,
                contextSuggestions = contextSuggestions,
                onContentChange = dependencies.viewModel::onContentChange,
                onShareDialogDismiss = dependencies.viewModel::onShareDialogDismiss,
                onCopyAll = dependencies.viewModel::onCopyAll,
            ),
    )

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).imePadding(),
        topBar = {
            val handleSave = {
                callbacks.onSave(uiState.content.text, uiState.content.selection.start)
                lastSavedText = uiState.content.text
                isDirty = false
            }
            EditorTopAppBar(
                args =
                    EditorTopAppBarArgs(
                        title = config.title,
                        isLoading = uiState.isLoading,
                        isSaveEnabled = isDirty && !uiState.isLoading,
                        onNavigateBack = callbacks.onNavigateBack,
                        onSave = handleSave,
                        onCopyAll = dependencies.viewModel::onCopyAll,
                        onShare = dependencies.viewModel::onShare,
                        onShowLocation = dependencies.viewModel::onShowLocation,
                        showLocationEnabled = uiState.projectId != null,
                    ),
            )
        },
        bottomBar = {
            EditorBottomBar(
                args =
                    EditorBottomBarArgs(
                        snackbarHostState = snackbarHostState,
                        isEditing = isEditing,
                        isToolbarVisible = isToolbarVisible,
                        onToolbarVisibleChange = { isToolbarVisible = it },
                        uiState = uiState,
                        viewModel = dependencies.viewModel,
                        linkSuggestions = linkSuggestions,
                        contextSuggestions = contextSuggestions,
                        onShowAttachmentPicker = { showAttachmentPicker = true },
                        onShowContextPicker = { showContextPicker = true },
                    ),
            )
        },
        floatingActionButton = {
            EditorFloatingActionButton(
                isEditing = isEditing,
                onToggleEditing = { nextMode ->
                    dependencies.viewModel.setEditingMode(nextMode)
                    if (nextMode) {
                        dependencies.contentFocusRequester.requestFocus()
                        keyboardController?.show()
                    } else {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                },
            )
        },
    ) { paddingValues ->
        EditorScaffoldContent(
            args =
                EditorScaffoldContentArgs(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    onContentChange = dependencies.viewModel::onContentChange,
                    onToggleCheckbox = dependencies.viewModel::onToggleCheckbox,
                    onWikiLinkClick = callbacks.onWikiLinkClick,
                    linkSuggestions = linkSuggestions,
                    contextSuggestions = contextSuggestions,
                    contentFocusRequester = dependencies.contentFocusRequester,
                    isToolbarVisible = isToolbarVisible,
                    readOnly = readOnly,
                    isEditing = isEditing,
                    foldingPersistenceKey = config.foldingPersistenceKey,
                    useFirstLineAsTitle = config.useFirstLineAsTitle,
                ),
        )
    }
}

@Composable
private fun ConfigureEditorSystemBars(
    view: android.view.View,
    topBarContainerColor: Color,
    isDarkTheme: Boolean,
) {
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
}

@Composable
private fun HandleStartInEditMode(
    startInEditMode: Boolean,
    initialEditApplied: Boolean,
    onInitialEditApplied: (Boolean) -> Unit,
    setEditingMode: (Boolean) -> Unit,
) {
    LaunchedEffect(startInEditMode) {
        if (startInEditMode && !initialEditApplied) {
            setEditingMode(true)
        }
        onInitialEditApplied(true)
    }
}

@Composable
private fun HandleEditingFocus(
    isEditing: Boolean,
    contentFocusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
) {
    LaunchedEffect(isEditing) {
        if (isEditing) {
            delay(50)
            contentFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

@Composable
private fun HandleEditorAutoSave(
    args: EditorAutoSaveArgs,
) {
    LaunchedEffect(args.content.text) {
        if (args.lastSavedText.isEmpty()) {
            args.onLastSavedTextChange(args.content.text)
        }
        val dirtyNow = args.content.text != args.lastSavedText
        args.onDirtyChange(dirtyNow)
        val currentText = args.content.text
        delay(2000)
        if (currentText == args.content.text && dirtyNow) {
            args.onAutoSave?.invoke(args.content.text, args.content.selection.start)
            args.onAutoSave?.let {
                args.onLastSavedTextChange(args.content.text)
                args.onDirtyChange(false)
            }
        }
    }
}

@Composable
private fun HandleEditorEvents(
    viewModel: UniversalEditorViewModel,
    navController: NavController,
    navigationManager: EnhancedNavigationManager?,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                is UniversalEditorEvent.ShowLocation -> {
                    val projectId = it.projectId
                    android.util.Log.d(
                        "ProjectRevealDebug",
                        "Navigating to project screen for projectId: $projectId in CONNECTIONS mode",
                    )
                    navigationManager?.navigate(
                        target =
                            NavTarget.ContextDetail(
                                contextId = projectId,
                                initialViewMode = ContextViewMode.CONNECTIONS.name,
                            ),
                        recordInHistory = true,
                    ) ?: navController.navigate(
                        "goal_detail_screen/$projectId?initialViewMode=${ContextViewMode.CONNECTIONS.name}",
                    )
                }

                is UniversalEditorEvent.ShowError -> snackbarHostState.showSnackbar(it.message)
            }
        }
    }
}

@Composable
private fun UniversalEditorDialogs(
    args: EditorDialogsArgs,
) {
    if (args.uiState.showShareDialog) {
        ShareDialog(
            onDismiss = args.onShareDialogDismiss,
            onCopyToClipboard = args.onCopyAll,
            content = args.uiState.content.text,
        )
    }

    if (args.showAttachmentPicker) {
        LinkPickerDialog(
            title = "Виберіть вкладення",
            suggestions = args.linkSuggestions.filterNot { it.startsWith("ctx:", ignoreCase = true) },
            onDismiss = { args.onShowAttachmentPickerChange(false) },
            onSelect = { token ->
                args.onContentChange(insertEditorToken(args.uiState.content, "[[$token]]"))
                args.onShowAttachmentPickerChange(false)
            },
        )
    }

    if (args.showContextPicker) {
        val contextTokens =
            args.linkSuggestions.filter { it.startsWith("ctx:", ignoreCase = true) }
                .ifEmpty { args.contextSuggestions.map { "ctx:unknown|$it" } }
        LinkPickerDialog(
            title = "Виберіть контекст",
            suggestions = contextTokens,
            onDismiss = { args.onShowContextPickerChange(false) },
            onSelect = { token ->
                val insertion =
                    if (token.startsWith("ctx:unknown|")) {
                        "@${token.substringAfter('|')} "
                    } else {
                        "[[$token]]"
                    }
                args.onContentChange(insertEditorToken(args.uiState.content, insertion))
                args.onShowContextPickerChange(false)
            },
        )
    }
}

private fun insertEditorToken(
    current: TextFieldValue,
    insertion: String,
): TextFieldValue {
    val start = current.selection.start
    val end = current.selection.end
    val newText = current.text.replaceRange(start, end, insertion)
    val newCursor = start + insertion.length
    return TextFieldValue(newText, TextRange(newCursor))
}

@Composable
@Suppress("LongMethod")
private fun EditorBottomBar(
    args: EditorBottomBarArgs,
) {
    Column {
        SnackbarHost(
            hostState = args.snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (args.isEditing) {
            Box(modifier = Modifier.navigationBarsPadding()) {
                AnimatedContent(
                    targetState = args.isToolbarVisible,
                    label = "toolbar_visibility",
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut(),
                        )
                    },
                ) { isVisible ->
                    if (isVisible) {
                        ExperimentalEnhancedListToolbar(
                            state = args.uiState.toolbarState,
                            onIndentBlock = args.viewModel::onIndentBlock,
                            onDeIndentBlock = args.viewModel::onDeIndentBlock,
                            onMoveBlockUp = args.viewModel::onMoveBlockUp,
                            onMoveBlockDown = args.viewModel::onMoveBlockDown,
                            onIndentLine = args.viewModel::onIndentLine,
                            onDeIndentLine = args.viewModel::onDeIndentLine,
                            onMoveLineUp = args.viewModel::onMoveLineUp,
                            onMoveLineDown = args.viewModel::onMoveLineDown,
                            onDeleteLine = args.viewModel::onDeleteLine,
                            onCopyLine = args.viewModel::onCopyLine,
                            onCutLine = args.viewModel::onCutLine,
                            onPasteLine = args.viewModel::onPasteLine,
                            onToggleBullet = args.viewModel::onToggleBullet,
                            onToggleCheckbox = args.viewModel::onToggleCheckbox,
                            onUndo = args.viewModel::onUndo,
                            onRedo = args.viewModel::onRedo,
                            onToggleVisibility = { args.onToolbarVisibleChange(false) },
                            onInsertDateTime = args.viewModel::onInsertDateTime,
                            onInsertTime = args.viewModel::onInsertTime,
                            onInsertAttachmentLink = args.onShowAttachmentPicker,
                            onInsertContextLink = args.onShowContextPicker,
                            canInsertAttachmentLink =
                                args.uiState.isEditing && args.linkSuggestions.any {
                                    !it.startsWith("ctx:", ignoreCase = true)
                                },
                            canInsertContextLink =
                                args.uiState.isEditing && (
                                    args.linkSuggestions.any {
                                        it.startsWith("ctx:", ignoreCase = true)
                                    } || args.contextSuggestions.isNotEmpty()
                                ),
                            onH1 = args.viewModel::onH1,
                            onH2 = args.viewModel::onH2,
                            onH3 = args.viewModel::onH3,
                            onBold = args.viewModel::onBold,
                            onItalic = args.viewModel::onItalic,
                            onInsertSeparator = args.viewModel::onInsertSeparator,
                        )
                    } else {
                        ShowToolbarButton(onClick = { args.onToolbarVisibleChange(true) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorFloatingActionButton(
    isEditing: Boolean,
    onToggleEditing: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.padding(bottom = 16.dp)) {
        FloatingActionButton(onClick = { onToggleEditing(!isEditing) }) {
            if (isEditing) {
                Icon(Icons.Default.Visibility, contentDescription = "Switch to read mode")
            } else {
                Icon(Icons.Default.Edit, contentDescription = "Switch to edit mode")
            }
        }
    }
}

@Composable
private fun EditorScaffoldContent(
    args: EditorScaffoldContentArgs,
) {
    Box(
        modifier =
            Modifier.padding(args.paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Editor(
            args =
                EditorArgs(
                    content = args.uiState.content,
                    onContentChange = args.onContentChange,
                    onToggleCheckbox = args.onToggleCheckbox,
                    onWikiLinkClick = args.onWikiLinkClick,
                    linkSuggestions = args.linkSuggestions,
                    contextSuggestions = args.contextSuggestions,
                    contentFocusRequester = args.contentFocusRequester,
                    isToolbarVisible = args.isToolbarVisible,
                    readOnly = args.readOnly,
                    isEditing = args.isEditing,
                    foldingPersistenceKey = args.foldingPersistenceKey,
                    useFirstLineAsTitle = args.useFirstLineAsTitle,
                ),
        )
        if (args.uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopAppBar(
    args: EditorTopAppBarArgs,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = args.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = args.onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            FilledTonalIconButton(onClick = args.onSave, enabled = args.isSaveEnabled) {
                if (args.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (args.showLocationEnabled) {
                    DropdownMenuItem(
                        text = { Text("Show location") },
                        leadingIcon = { Icon(Icons.Default.RemoveRedEye, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            args.onShowLocation()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy all") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        args.onCopyAll()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        args.onShare()
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
    args: EditorArgs,
) {
    val scrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current
    val imeHeight = WindowInsets.ime.getBottom(density)
    val editorState = rememberEditorDerivedState(args = args)

    LaunchedEffect(args.content.selection, textLayoutResult, imeHeight, args.isToolbarVisible) {
        scrollCursorIntoView(
            content = args.content,
            textLayoutResult = textLayoutResult,
            density = density,
            bringIntoViewRequester = bringIntoViewRequester,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EditorMainContent(
            args = args,
            editorState = editorState,
            scrollState = scrollState,
            bringIntoViewRequester = bringIntoViewRequester,
            highlightColor = highlightColor,
            textLayoutResult = textLayoutResult,
            onTextLayout = { result -> textLayoutResult = result },
        )

        InlineSuggestionsCard(
            state =
                InlineSuggestionsState(
                    activeQuery = editorState.activeQuery,
                    suggestions = editorState.filteredSuggestions,
                ),
            content = args.content,
            visible = !args.readOnly,
            modifier = Modifier.align(Alignment.BottomStart),
            onContentChange = args.onContentChange,
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun rememberEditorDerivedState(
    args: EditorArgs,
): EditorDerivedState {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    val activeQuery =
        remember(args.content.text, args.content.selection) {
            extractInlineQuery(args.content.text, args.content.selection.start)
        }
    val filteredSuggestions =
        remember(activeQuery, args.linkSuggestions, args.contextSuggestions) {
            buildFilteredSuggestions(activeQuery, args.linkSuggestions, args.contextSuggestions)
        }
    val sourceLines = remember(args.content.text) { args.content.text.lines() }
    val firstLine = sourceLines.firstOrNull().orEmpty()
    val showDocumentTitle = args.useFirstLineAsTitle && firstLine.isNotBlank()
    val documentTitle = if (showDocumentTitle) NoteTitleExtractor.extract(args.content.text) else ""
    val readModeSource =
        if (showDocumentTitle) {
            sourceLines.drop(1).joinToString("\n")
        } else {
            args.content.text
        }
    val readModeLines = remember(readModeSource) { readModeSource.lines() }
    val readModeHeadingLevels =
        remember(readModeSource) {
            HeadingFolding.computeHeadingLevels(readModeLines)
        }
    var collapsedHeadingLines by
        rememberSaveable(args.foldingPersistenceKey) {
            mutableStateOf(
                if (args.foldingPersistenceKey.isNullOrBlank()) {
                    listOf()
                } else {
                    loadCollapsedHeadings(context, args.foldingPersistenceKey)
                },
            )
        }
    val sanitizedCollapsedHeadingLines =
        remember(collapsedHeadingLines, readModeHeadingLevels) {
            HeadingFolding.sanitizeCollapsedHeadings(collapsedHeadingLines, readModeHeadingLevels)
        }

    LaunchedEffect(readModeHeadingLevels) {
        val normalized = sanitizedCollapsedHeadingLines.toList().sorted()
        if (collapsedHeadingLines != normalized) {
            collapsedHeadingLines = normalized
        }
    }

    LaunchedEffect(args.foldingPersistenceKey, sanitizedCollapsedHeadingLines) {
        if (args.foldingPersistenceKey.isNullOrBlank()) return@LaunchedEffect
        saveCollapsedHeadings(context, args.foldingPersistenceKey, sanitizedCollapsedHeadingLines)
    }

    return EditorDerivedState(
        activeQuery = activeQuery,
        filteredSuggestions = filteredSuggestions,
        documentTitle = documentTitle,
        showDocumentTitle = showDocumentTitle,
        readModeSource = readModeSource,
        sanitizedCollapsedHeadingLines = sanitizedCollapsedHeadingLines,
        onToggleHeading = { headingLineIndex ->
            collapsedHeadingLines =
                toggleCollapsedHeading(
                    collapsedHeadingLines = sanitizedCollapsedHeadingLines.toList(),
                    headingLineIndex = headingLineIndex,
                )
        },
        textColor = textColor,
        accentColor = accentColor,
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun EditorMainContent(
    args: EditorArgs,
    editorState: EditorDerivedState,
    scrollState: ScrollState,
    bringIntoViewRequester: BringIntoViewRequester,
    highlightColor: Color,
    textLayoutResult: TextLayoutResult?,
    onTextLayout: (TextLayoutResult) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        val readModeTransformation =
            ListVisualTransformation(
                editorState.sanitizedCollapsedHeadingLines,
                editorState.textColor,
                editorState.accentColor,
            )

        val baseModifier =
            Modifier.padding(start = 16.dp)
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(args.contentFocusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .focusProperties { canFocus = args.isEditing }

        if (args.readOnly) {
            val transformed =
                readModeTransformation.filter(AnnotatedString(editorState.readModeSource))
            EditorReadModeContent(
                state =
                    EditorReadModeState(
                        source = editorState.readModeSource,
                        documentTitle = editorState.documentTitle,
                        showDocumentTitle = editorState.showDocumentTitle,
                        transformedText = transformed.text,
                        collapsedHeadingLines = editorState.sanitizedCollapsedHeadingLines.toList(),
                        onToggleHeading = editorState.onToggleHeading,
                        onAnnotationClick = { annotation ->
                            when (annotation?.tag) {
                                "wikilink" -> args.onWikiLinkClick(annotation.item)
                                "tag" -> args.onWikiLinkClick("#${annotation.item}")
                                "context" -> args.onWikiLinkClick("@${annotation.item}")
                            }
                        },
                    ),
                modifier = baseModifier,
                textColor = editorState.textColor,
            )
        } else {
            EditorEditModeContent(
                state =
                    EditorEditModeState(
                        content = args.content,
                        isEditing = args.isEditing,
                        highlightColor = highlightColor,
                        textColor = editorState.textColor,
                        textLayoutResult = textLayoutResult,
                    ),
                modifier = baseModifier,
                onContentChange = args.onContentChange,
                onToggleCheckbox = args.onToggleCheckbox,
                onTextLayout = onTextLayout,
            )
        }
    }
}

private fun extractInlineQuery(
    text: String,
    cursor: Int,
): InlineQuery? {
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
    return when (type) {
        "link" -> buildLinkInlineQuery(before, start, remainder)
        "tag" -> buildPrefixedInlineQuery(before, start, remainder, "#")
        "context" -> buildPrefixedInlineQuery(before, start, remainder, "@")
        else -> null
    }
}

private fun buildLinkInlineQuery(
    before: String,
    start: Int,
    remainder: String,
): InlineQuery? {
    val hasClose = before.indexOf("]]", startIndex = start) != -1
    return if (hasClose) {
        null
    } else {
        InlineQuery(start, "[[", remainder)
    }
}

private fun buildPrefixedInlineQuery(
    before: String,
    start: Int,
    remainder: String,
    prefix: String,
): InlineQuery? {
    if (start > 0 && !before[start - 1].isWhitespace()) return null
    if (remainder.isBlank()) return null
    return InlineQuery(start, prefix, remainder)
}

private fun buildFilteredSuggestions(
    activeQuery: InlineQuery?,
    linkSuggestions: List<String>,
    contextSuggestions: List<String>,
): List<String> =
    activeQuery?.takeIf { it.query.length >= 3 }?.let { query ->
        when (query.prefix) {
            "[[" ->
                linkSuggestions.filter { suggestion ->
                    val display = extractWikiLinkDisplay(suggestion)
                    display.contains(query.query, ignoreCase = true) ||
                        suggestion.contains(query.query, ignoreCase = true)
                }.take(6)
            "@" -> contextSuggestions.filter { it.contains(query.query, ignoreCase = true) }.take(6)
            else -> emptyList()
        }
    } ?: emptyList()

@Composable
private fun EditorReadModeContent(
    state: EditorReadModeState,
    modifier: Modifier,
    textColor: Color,
) {
    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.showDocumentTitle) {
            Text(
                text = state.documentTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
        if (state.source.isNotBlank()) {
            SelectionContainer {
                ClickableText(
                    text = state.transformedText,
                    style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
                    onClick = { clickOffset ->
                        val headingLineIndex =
                            state.transformedText
                                .getStringAnnotations("fold_heading", clickOffset, clickOffset)
                                .firstOrNull()
                                ?.item
                                ?.toIntOrNull()
                        if (headingLineIndex != null) {
                            state.onToggleHeading(headingLineIndex)
                        } else {
                            state.onAnnotationClick(findEditorAnnotation(state.transformedText, clickOffset))
                        }
                    },
                )
            }
        }
    }
}

private fun toggleCollapsedHeading(
    collapsedHeadingLines: List<Int>,
    headingLineIndex: Int,
): List<Int> =
    if (collapsedHeadingLines.contains(headingLineIndex)) {
        collapsedHeadingLines.filterNot { it == headingLineIndex }.sorted()
    } else {
        (collapsedHeadingLines + headingLineIndex).sorted()
    }

private fun findEditorAnnotation(
    transformedText: AnnotatedString,
    clickOffset: Int,
): AnnotatedString.Range<String>? =
    transformedText.getStringAnnotations("wikilink", clickOffset, clickOffset).firstOrNull()
        ?: transformedText.getStringAnnotations("tag", clickOffset, clickOffset).firstOrNull()
        ?: transformedText.getStringAnnotations("context", clickOffset, clickOffset).firstOrNull()

@Composable
private fun EditorEditModeContent(
    state: EditorEditModeState,
    modifier: Modifier,
    onContentChange: (TextFieldValue) -> Unit,
    onToggleCheckbox: (Int) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit,
) {
    BasicTextField(
        value = state.content,
        onValueChange = onContentChange,
        onTextLayout = onTextLayout,
        modifier =
            modifier
                .pointerInput(state.content.text, state.isEditing) {
                    detectTapGestures { offset ->
                        handleCheckboxTap(
                            content = state.content,
                            textLayoutResult = state.textLayoutResult,
                            offset = offset,
                            onToggleCheckbox = onToggleCheckbox,
                        )
                    }
                }
                .drawBehind {
                    if (!state.isEditing) return@drawBehind
                    drawCurrentLineHighlight(
                        content = state.content,
                        textLayoutResult = state.textLayoutResult,
                        highlightColor = state.highlightColor,
                        width = size.width,
                    )
                },
        textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = state.textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = VisualTransformation.None,
        readOnly = false,
        interactionSource = remember { MutableInteractionSource() },
        singleLine = false,
        maxLines = Int.MAX_VALUE,
    )
}

private fun handleCheckboxTap(
    content: TextFieldValue,
    textLayoutResult: TextLayoutResult?,
    offset: Offset,
    onToggleCheckbox: (Int) -> Unit,
) {
    val layout = textLayoutResult ?: return
    val clickedOffset = layout.getOffsetForPosition(offset)
    val lineIndex = layout.getLineForOffset(clickedOffset)
    val lines = content.text.lines()
    if (lineIndex >= lines.size) return

    val line = lines[lineIndex]
    val trimmedLine = line.trimStart()
    val checkboxRegex = Regex("""^\s*-\s\[[ x]\]\s?.*""", RegexOption.IGNORE_CASE)
    if (!checkboxRegex.matches(trimmedLine)) return

    val lineStartOffset = layout.getLineStart(lineIndex)
    val originalIndentLength = line.takeWhile { it.isWhitespace() }.length
    val offsetInLine = clickedOffset - lineStartOffset
    val checkboxStart = originalIndentLength
    val checkboxEnd = originalIndentLength + 8

    if (offsetInLine in checkboxStart until checkboxEnd) {
        onToggleCheckbox(lineIndex)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentLineHighlight(
    content: TextFieldValue,
    textLayoutResult: TextLayoutResult?,
    highlightColor: Color,
    width: Float,
) {
    textLayoutResult?.let { layoutResult ->
        val currentLine = layoutResult.getLineForOffset(content.selection.start)
        if (currentLine < layoutResult.lineCount) {
            val top = layoutResult.getLineTop(currentLine)
            val bottom = layoutResult.getLineBottom(currentLine)
            drawRect(
                color = highlightColor,
                topLeft = Offset(0f, top),
                size = Size(width, bottom - top),
            )
        }
    }
}

@Composable
private fun InlineSuggestionsCard(
    state: InlineSuggestionsState,
    content: TextFieldValue,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onContentChange: (TextFieldValue) -> Unit,
) {
    if (!visible || state.suggestions.isEmpty() || state.activeQuery == null) return

    Card(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.suggestions.forEach { suggestion ->
                Text(
                    text = if (state.activeQuery.prefix == "[[") extractWikiLinkDisplay(suggestion) else suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onContentChange(applyInlineSuggestion(content, state.activeQuery, suggestion))
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun applyInlineSuggestion(
    content: TextFieldValue,
    query: InlineQuery,
    suggestion: String,
): TextFieldValue {
    val cursor = content.selection.start
    val insertedText =
        when (query.prefix) {
            "[[" -> "[[$suggestion]]"
            "#" -> "#$suggestion "
            "@" -> "@$suggestion "
            else -> suggestion
        }
    val newText =
        buildString {
            append(content.text.substring(0, query.start))
            append(insertedText)
            append(content.text.substring(cursor))
        }
    val newCursor = query.start + insertedText.length
    return TextFieldValue(newText, TextRange(newCursor))
}

private suspend fun scrollCursorIntoView(
    content: TextFieldValue,
    textLayoutResult: TextLayoutResult?,
    density: androidx.compose.ui.unit.Density,
    bringIntoViewRequester: BringIntoViewRequester,
) {
    delay(100)
    textLayoutResult?.let { layoutResult ->
        if (layoutResult.lineCount > 0) {
            val cursorRect = layoutResult.getCursorRect(content.selection.start)
            val extraPadding = with(density) { 24.dp.toPx() }
            bringIntoViewRequester.bringIntoView(
                cursorRect.copy(bottom = cursorRect.bottom + extraPadding),
            )
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f,
                            ),
                        ),
            )

            Box(
                modifier =
                    Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.5f,
                                ),
                            ),
                )
            }
        }
    }
}

@Composable
private fun LinkPickerDialog(
    title: String,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query, suggestions) {
            if (query.isBlank()) {
                suggestions.take(24)
            } else {
                suggestions.filter {
                    extractWikiLinkDisplay(it).contains(query, ignoreCase = true) || it.contains(query, ignoreCase = true)
                }.take(24)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Пошук...") },
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    filtered.forEach { token ->
                        Text(
                            text = extractWikiLinkDisplay(token),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(token) }
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрити") }
        },
    )
}

private fun extractWikiLinkDisplay(raw: String): String {
    val trimmed = raw.trim()
    val parts = trimmed.split("|", limit = 2)
    if (parts.size == 2 && parts[1].isNotBlank()) return parts[1]
    val typed = Regex("""^(doc|ctx|music|checklist):(.+)$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
    return typed?.groupValues?.get(2)?.takeIf { it.isNotBlank() } ?: trimmed
}

private fun isMarkdownSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.length < 3) return false
    val compact = trimmed.replace(" ", "")
    if (compact.length < 3) return false
    val first = compact.first()
    if (first != '*' && first != '-' && first != '_') return false
    return compact.all { it == first }
}

private const val FOLDING_PREFS_NAME = "universal_editor_heading_folding"
private const val FOLDING_PREFS_PREFIX = "folded_headings_"
private const val SEPARATOR_RENDER_LINE = "• • •"
private val TAG_STYLE_COLOR = Color(0xFF0D47A1)
private val CONTEXT_STYLE_COLOR = Color(0xFF00695C)

private data class TransformationLineInfo(
    val originalIndex: Int,
    val transformedLength: Int,
)

private data class ReadTransformationContext(
    val headingLevels: List<Int?>,
    val sanitizedCollapsedHeadings: Set<Int>,
    val textColor: Color,
    val accentColor: Color,
    val separatorColor: Color,
)

private fun loadCollapsedHeadings(
    context: Context,
    persistenceKey: String,
): List<Int> {
    val prefs = context.getSharedPreferences(FOLDING_PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(FOLDING_PREFS_PREFIX + persistenceKey, "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split(",")
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .distinct()
        .sorted()
}

private fun saveCollapsedHeadings(
    context: Context,
    persistenceKey: String,
    collapsedHeadings: Set<Int>,
) {
    val prefs = context.getSharedPreferences(FOLDING_PREFS_NAME, Context.MODE_PRIVATE)
    if (collapsedHeadings.isEmpty()) {
        prefs.edit().remove(FOLDING_PREFS_PREFIX + persistenceKey).apply()
        return
    }
    val serialized = collapsedHeadings.toList().sorted().joinToString(",")
    prefs.edit().putString(FOLDING_PREFS_PREFIX + persistenceKey, serialized).apply()
}

private class ListVisualTransformation(
    private val collapsedHeadingLines: Set<Int>,
    private val textColor: Color,
    private val accentColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val lines = originalText.lines()
        val transformationContext = buildReadTransformationContext(lines, collapsedHeadingLines, textColor, accentColor)
        val visibleLineIndices =
            HeadingFolding.computeVisibleLineIndices(
                lines = lines,
                headingLevels = transformationContext.headingLevels,
                collapsedHeadingLines = transformationContext.sanitizedCollapsedHeadings,
            )
        val visibleLines = visibleLineIndices.map { index -> IndexedValue(index, lines[index]) }
        val lineInfos = mutableListOf<TransformationLineInfo>()
        val transformedText =
            buildAnnotatedString {
                visibleLines.forEachIndexed { visibleIndex, indexedValue ->
                    val lineStart = length
                    appendTransformedLine(
                        indexedValue = indexedValue,
                        context = transformationContext,
                    )
                    lineInfos.add(
                        TransformationLineInfo(
                            originalIndex = indexedValue.index,
                            transformedLength = length - lineStart,
                        ),
                    )
                    if (visibleIndex < visibleLines.size - 1) {
                        append("\n")
                    }
                }
            }

        return TransformedText(
            transformedText,
            buildReadModeOffsetMapping(
                originalText = originalText,
                transformedText = transformedText.text,
                lineInfos = lineInfos,
                lines = lines,
                visibleLines = visibleLines,
            ),
        )
    }
}

private fun buildReadTransformationContext(
    lines: List<String>,
    collapsedHeadingLines: Set<Int>,
    textColor: Color,
    accentColor: Color,
): ReadTransformationContext {
    val headingLevels = HeadingFolding.computeHeadingLevels(lines)
    val sanitizedCollapsedHeadings =
        HeadingFolding.sanitizeCollapsedHeadings(collapsedHeadingLines, headingLevels)
    return ReadTransformationContext(
        headingLevels = headingLevels,
        sanitizedCollapsedHeadings = sanitizedCollapsedHeadings,
        textColor = textColor,
        accentColor = accentColor,
        separatorColor = textColor.copy(alpha = 0.45f),
    )
}

private fun AnnotatedString.Builder.appendTransformedLine(
    indexedValue: IndexedValue<String>,
    context: ReadTransformationContext,
) {
    val originalIndex = indexedValue.index
    val line = indexedValue.value
    appendSeparatorLine(line, context)
        || appendHeadingLine(line, originalIndex, context)
        || appendBulletLine(line, context)
        || appendNumberedLine(line, context)
        || appendCheckboxLine(line, checked = false, context = context)
        || appendCheckboxLine(line, checked = true, context = context)
        || appendInlineAnnotatedLine(line, context)
}

private fun AnnotatedString.Builder.appendSeparatorLine(
    line: String,
    context: ReadTransformationContext,
): Boolean {
    if (!isMarkdownSeparatorLine(line)) return false
    withStyle(SpanStyle(color = context.separatorColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)) {
        append(SEPARATOR_RENDER_LINE)
    }
    return true
}

private fun AnnotatedString.Builder.appendHeadingLine(
    line: String,
    originalIndex: Int,
    context: ReadTransformationContext,
): Boolean {
    val headingLevel = context.headingLevels[originalIndex] ?: return false
    val headingMatch = Regex("""^(\s*)(#{1,6})(?:\s+|$)(.*)$""").find(line)
    val indent = headingMatch?.groupValues?.getOrNull(1).orEmpty()
    val hashes = headingMatch?.groupValues?.getOrNull(2).orEmpty().ifEmpty { "#".repeat(headingLevel) }
    val contentPart = headingMatch?.groupValues?.getOrNull(3).orEmpty().trimStart()
    val foldMarker = if (context.sanitizedCollapsedHeadings.contains(originalIndex)) "▸ " else "▾ "
    val headingStart = length

    withStyle(SpanStyle(color = context.accentColor, fontWeight = FontWeight.Bold)) { append(foldMarker) }
    append(indent)
    withStyle(SpanStyle(color = context.accentColor, fontWeight = FontWeight.Bold)) {
        append(hashes)
        if (contentPart.isNotBlank()) append(" ")
    }
    if (contentPart.isNotBlank()) {
        withStyle(SpanStyle(color = context.textColor, fontWeight = FontWeight.Bold)) { append(contentPart) }
    }
    addStringAnnotation(
        tag = "fold_heading",
        annotation = originalIndex.toString(),
        start = headingStart,
        end = length,
    )
    return true
}

private fun AnnotatedString.Builder.appendBulletLine(
    line: String,
    context: ReadTransformationContext,
): Boolean {
    val match = Regex("""^(\s*)\*\s(.*)""").find(line) ?: return false
    val (indent, content) = match.destructured
    appendPrefixedContent(indent = indent, prefix = "• ", content = content, context = context)
    return true
}

private fun AnnotatedString.Builder.appendNumberedLine(
    line: String,
    context: ReadTransformationContext,
): Boolean {
    val match = Regex("""^(\s*)(\d+)\.\s(.*)""").find(line) ?: return false
    val (indent, number, content) = match.destructured
    appendPrefixedContent(indent = indent, prefix = "$number. ", content = content, context = context)
    return true
}

private fun AnnotatedString.Builder.appendCheckboxLine(
    line: String,
    checked: Boolean,
    context: ReadTransformationContext,
): Boolean {
    val regex =
        if (checked) {
            Regex("""^(\s*)-\s\[x\]\s(.*)""", RegexOption.IGNORE_CASE)
        } else {
            Regex("""^(\s*)-\s\[\s\]\s(.*)""")
        }
    val match = regex.find(line) ?: return false
    val (indent, content) = match.destructured
    appendPrefixedContent(indent = indent, prefix = if (checked) "☑ " else "☐ ", content = content, context = context)
    return true
}

private fun AnnotatedString.Builder.appendPrefixedContent(
    indent: String,
    prefix: String,
    content: String,
    context: ReadTransformationContext,
) {
    append(indent)
    withStyle(SpanStyle(color = context.accentColor, fontWeight = FontWeight.Bold)) { append(prefix) }
    withStyle(SpanStyle(color = context.textColor)) { append(content) }
}

private fun AnnotatedString.Builder.appendInlineAnnotatedLine(
    line: String,
    context: ReadTransformationContext,
): Boolean {
    append(
        buildInlineAnnotatedString(
            line = line,
            textColor = context.textColor,
            accentColor = context.accentColor,
        ),
    )
    return true
}

private fun buildInlineAnnotatedString(
    line: String,
    textColor: Color,
    accentColor: Color,
): AnnotatedString {
    val wikiLinkRegex = Regex("\\[\\[([^\\[\\]]+)\\]\\]")
    val tagRegex = Regex("#(\\w+)")
    val contextRegex = Regex("@(\\w+)")
    val allInlineRegex =
        listOf(
            "wikilink" to wikiLinkRegex,
            "tag" to tagRegex,
            "context" to contextRegex,
        )

    return buildAnnotatedString {
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
                "wikilink" -> append(extractWikiLinkDisplay(contentText))
                "tag" -> append("#$contentText")
                "context" -> append("@$contentText")
            }
            val end = length
            addStyle(resolveInlineStyle(tag, textColor, accentColor), start = start, end = end)
            addStringAnnotation(tag = tag, annotation = contentText, start = start, end = end)
            cursor = match.range.last + 1
        }

        addInlineEmphasisStyles(this)
    }
}

private fun resolveInlineStyle(
    tag: String,
    textColor: Color,
    accentColor: Color,
): SpanStyle =
    when (tag) {
        "wikilink" -> SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)
        "tag" -> SpanStyle(color = TAG_STYLE_COLOR, fontWeight = FontWeight.Medium)
        "context" -> SpanStyle(color = CONTEXT_STYLE_COLOR, fontWeight = FontWeight.Medium)
        else -> SpanStyle(color = textColor)
    }

private fun addInlineEmphasisStyles(builder: AnnotatedString.Builder) {
    val boldRegex = Regex("""(\*\*|__)(.*?)\1""")
    val italicRegex = Regex("""(\*|_)(.*?)\1""")
    boldRegex.findAll(builder.toString()).forEach { matchResult ->
        builder.addStyle(
            style = SpanStyle(fontWeight = FontWeight.Bold),
            start = matchResult.range.first,
            end = matchResult.range.last + 1,
        )
    }
    italicRegex.findAll(builder.toString()).forEach { matchResult ->
        builder.addStyle(
            style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            start = matchResult.range.first,
            end = matchResult.range.last + 1,
        )
    }
}

private fun buildReadModeOffsetMapping(
    originalText: String,
    transformedText: String,
    lineInfos: List<TransformationLineInfo>,
    lines: List<String>,
    visibleLines: List<IndexedValue<String>>,
): OffsetMapping =
    object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            if (offset <= 0) return 0
            val prefix = originalText.substring(0, offset)
            val parts = prefix.lines()
            val originalLineIndex = parts.size - 1
            val charInLine = parts.lastOrNull()?.length ?: 0
            val linePosition = resolveTransformedLinePosition(lineInfos, originalLineIndex)
            if (linePosition == null) return transformedText.length
            val adjustedChar = charInLine.coerceAtMost(linePosition.second)
            return (linePosition.first + adjustedChar).coerceIn(0, transformedText.length)
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

private fun resolveTransformedLinePosition(
    lineInfos: List<TransformationLineInfo>,
    originalLineIndex: Int,
): Pair<Int, Int>? {
    var transformedLineStart = 0
    lineInfos.forEach { info ->
        if (info.originalIndex == originalLineIndex) {
            return transformedLineStart to info.transformedLength
        }
        transformedLineStart += info.transformedLength + 1
    }
    return null
}
