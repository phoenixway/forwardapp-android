package com.romankozak.forwardappmobile.features.ai.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.domain.aichat.RoleFile
import com.romankozak.forwardappmobile.domain.aichat.RoleFolder
import com.romankozak.forwardappmobile.domain.aichat.RoleItem
import com.romankozak.forwardappmobile.ui.ModelsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AI_CHAT_DEBUG"
private const val CHAT_BOTTOM_SCROLL_OFFSET = 100_000

private data class ChatScrollSnapshot(
    val isAtBottom: Boolean,
    val isUserScrollingAwayFromBottom: Boolean,
    val isScrolling: Boolean,
)

private fun LazyListState.toChatScrollSnapshot(): ChatScrollSnapshot {
    val totalItems = layoutInfo.totalItemsCount
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
    val lastItemBottom = lastVisibleItem?.let { it.offset + it.size }
    val isAtBottom =
        totalItems == 0 ||
            (
                lastVisibleItem?.index == totalItems - 1 &&
                    lastItemBottom != null &&
                    lastItemBottom <= layoutInfo.viewportEndOffset
            )
    return ChatScrollSnapshot(
        isAtBottom = isAtBottom,
        isUserScrollingAwayFromBottom = isScrollInProgress && !isAtBottom,
        isScrolling = isScrollInProgress,
    )
}

private suspend fun LazyListState.animateToChatBottom(lastIndex: Int) {
    if (lastIndex < 0) return
    animateScrollToItem(
        index = lastIndex,
        scrollOffset = CHAT_BOTTOM_SCROLL_OFFSET,
    )
}

private suspend fun LazyListState.scrollToChatBottom(lastIndex: Int) {
    if (lastIndex < 0) return
    scrollToItem(
        index = lastIndex,
        scrollOffset = CHAT_BOTTOM_SCROLL_OFFSET,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scriptsEnabled = FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)

    var showMenu by remember { mutableStateOf(false) }
    var showRoleSelectorDialog by remember { mutableStateOf(false) }
    var showTemperatureDialog by remember { mutableStateOf(false) }
    var showModelSelectorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var autoScrollToBottom by remember { mutableStateOf(true) }
    var showScrollControls by remember { mutableStateOf(false) }
    val isAtChatBottom by remember {
        derivedStateOf { listState.toChatScrollSnapshot().isAtBottom }
    }

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(navController, viewModel) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow<String?>("script_chooser_result", null)
            ?.collect { scriptId ->
                if (scriptId != null) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("script_chooser_result", null)
                    viewModel.runScript(scriptId)
                }
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.toChatScrollSnapshot() }
            .distinctUntilChanged()
            .collect { snapshot ->
                when {
                    snapshot.isAtBottom -> autoScrollToBottom = true
                    snapshot.isUserScrollingAwayFromBottom -> autoScrollToBottom = false
                }
                if (snapshot.isScrolling || !snapshot.isAtBottom) {
                    showScrollControls = true
                }
            }
    }

    LaunchedEffect(showScrollControls, listState.isScrollInProgress, isAtChatBottom) {
        if (showScrollControls && !listState.isScrollInProgress) {
            delay(3_000)
            if (!listState.isScrollInProgress) {
                showScrollControls = false
            }
        }
    }

    LaunchedEffect(uiState.messages.size, autoScrollToBottom) {
        if (uiState.messages.isNotEmpty() && autoScrollToBottom && !listState.isScrollInProgress) {
            delay(150)
            if (autoScrollToBottom && !listState.isScrollInProgress) {
                Log.d(TAG, "[EFFECT 1] Scrolling to new message, index: ${uiState.messages.size - 1}")
                listState.animateToChatBottom(uiState.messages.size - 1)
            }
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.text, autoScrollToBottom) {
        if (uiState.messages.isNotEmpty() && autoScrollToBottom && !listState.isScrollInProgress) {
            Log.d(TAG, "[EFFECT 2] Keeping latest streaming message visible.")
            listState.scrollToChatBottom(uiState.messages.lastIndex)
        }
    }

    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
        )

    if (showRoleSelectorDialog) {
        RoleSelectorDialog(
            roles = uiState.rolesHierarchy,
            onDismiss = { showRoleSelectorDialog = false },
        ) { roleFile ->
            viewModel.updateSystemPromptAndTitle(
                newPrompt = roleFile.prompt,
                newTitle = roleFile.name,
            )
            showRoleSelectorDialog = false
        }
    }

    if (showTemperatureDialog) {
        TemperatureDialog(
            currentTemperature = uiState.temperature,
            onDismiss = { showTemperatureDialog = false },
        ) { newTemp ->
            viewModel.updateTemperature(newTemp)
            showTemperatureDialog = false
        }
    }

    if (showModelSelectorDialog) {
        ModelSelectorDialog(
            modelsState = uiState.availableModels,
            onDismiss = { showModelSelectorDialog = false },
            onModelSelected = { modelName ->
                viewModel.selectSmartModel(modelName)
                showModelSelectorDialog = false
            },
        )
    }

    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                uiState.currentConversation?.let { viewModel.deleteConversation(it.id) }
                showDeleteConfirmationDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Chat deleted")
                }
            },
            onDismiss = { showDeleteConfirmationDialog = false },
        )
    }

    if (showEditTitleDialog) {
        EditTitleDialog(
            currentTitle = uiState.currentConversation?.title ?: "",
            onDismiss = { showEditTitleDialog = false },
            onSave = { newTitle ->
                viewModel.updateConversationTitle(newTitle)
                showEditTitleDialog = false
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ConversationDrawer(
                    drawerItems = uiState.drawerItems,
                    onConversationClick = {
                        viewModel.setCurrentConversation(it)
                        coroutineScope.launch { drawerState.close() }
                    },
                )
            },
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(uiState.currentConversation?.title ?: "Chat", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                    if (uiState.messages.any { it.isStreaming }) {
                                        Text(
                                            "Preparing response…",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showEditTitleDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit title")
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New Chat") },
                                        onClick = {
                                            viewModel.startNewChat()
                                            showMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Chat") },
                                        onClick = {
                                            showDeleteConfirmationDialog = true
                                            showMenu = false
                                        },
                                    )
                                    if (scriptsEnabled) {
                                        DropdownMenuItem(
                                            text = { Text("Execute script") },
                                            onClick = {
                                                navigationManager.navigateOrFallback(
                                                    navController = navController,
                                                    target = NavTarget.ScriptChooser,
                                                )
                                                showMenu = false
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Change Role") },
                                        onClick = {
                                            showRoleSelectorDialog = true
                                            showMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export Chat") },
                                        onClick = {
                                            showMenu = false
                                            coroutineScope.launch {
                                                val result = viewModel.exportChatMarkdownToConfiguredFolder()
                                                result
                                                    .onSuccess { fileName ->
                                                        snackbarHostState.showSnackbar("Chat exported: $fileName")
                                                    }
                                                    .onFailure { error ->
                                                        snackbarHostState.showSnackbar(
                                                            error.message ?: "Chat export folder is unavailable",
                                                        )
                                                        val chatText = viewModel.exportChat()
                                                        val sendIntent =
                                                            Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_TEXT, chatText)
                                                                type = "text/plain"
                                                            }
                                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                                        context.startActivity(shareIntent)
                                                    }
                                            }
                                        },
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        onClick = {
                                            navigationManager.navigateOrFallback(
                                                navController = navController,
                                                target = NavTarget.Settings,
                                            )
                                            showMenu = false
                                        },
                                    )
                                }
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(backgroundBrush)
                            .padding(paddingValues)
                            .imePadding(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 56.dp),
                        ) {
                            if (uiState.messages.isEmpty() && !uiState.messages.any { it.isStreaming }) {
                                item { EmptyStateMessage() }
                            }

                            itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { index, message ->
                                val isLastAssistantMessage = !message.isFromUser && index == uiState.messages.lastIndex

                                MessageBubble(
                                    message = message,
                                    isLastAssistantMessage = isLastAssistantMessage,
                                    onCopyToClipboard = { text ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("chat_message", text)
                                        clipboard.setPrimaryClip(clip)
                                    },
                                    onRegenerate = viewModel::regenerateLastResponse,
                                    onTranslate = { viewModel.translateMessage(message.id) },
                                )
                            }
                        }

                        if (uiState.messages.isNotEmpty() && showScrollControls) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 18.dp, bottom = 12.dp),
                            ) {
                                ChatScrollControls(
                                    canScrollDown = !isAtChatBottom,
                                    onTopClick = {
                                        autoScrollToBottom = false
                                        showScrollControls = true
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                    },
                                    onPreviousClick = {
                                        autoScrollToBottom = false
                                        showScrollControls = true
                                        val targetIndex = maxOf(0, listState.firstVisibleItemIndex - 1)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    },
                                    onNextClick = {
                                        autoScrollToBottom = false
                                        showScrollControls = true
                                        val targetIndex = minOf(uiState.messages.lastIndex, listState.firstVisibleItemIndex + 1)
                                        coroutineScope.launch {
                                            if (targetIndex == uiState.messages.lastIndex) {
                                                listState.animateToChatBottom(targetIndex)
                                            } else {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    onBottomClick = {
                                        autoScrollToBottom = true
                                        showScrollControls = true
                                        coroutineScope.launch {
                                            listState.animateToChatBottom(uiState.messages.lastIndex)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    ChatInput(
                        value = userInput,
                        onValueChange = viewModel::onUserInputChange,
                        onSendClick = {
                            viewModel.sendMessage()
                            keyboardController?.hide()
                        },
                        onStopClick = viewModel::stopGeneration,
                        isLoading = uiState.messages.any { it.isStreaming },
                        roleTitle = uiState.roleTitle,
                        temperature = uiState.temperature,
                        modelName = uiState.smartModel,
                        onModelClick = {
                            viewModel.loadAvailableModels()
                            showModelSelectorDialog = true
                        },
                        onRoleClick = { showRoleSelectorDialog = true },
                        onTemperatureClick = { showTemperatureDialog = true },
                        modifier = Modifier.shadow(8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isLastAssistantMessage: Boolean,
    onCopyToClipboard: (String) -> Unit,
    onRegenerate: () -> Unit,
    onTranslate: () -> Unit,
) {
    val isUser = message.isFromUser
    val isStreaming = message.isStreaming && !isUser
    var animatedText by remember(message.id) { mutableStateOf(if (isUser) message.text else "") }

    LaunchedEffect(message.text, message.isStreaming) {
        animatedText = message.text
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isUser) {
                Surface(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "AI Avatar",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                modifier =
                    Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isUser) 20.dp else 4.dp,
                                topEnd = if (isUser) 4.dp else 20.dp,
                                bottomStart = 20.dp,
                                bottomEnd = 20.dp,
                            ),
                        ),
                color =
                    when {
                        message.isError -> MaterialTheme.colorScheme.errorContainer
                        isUser -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                tonalElevation = if (isUser) 0.dp else 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        ChatMessageBody(
                            text = animatedText.ifBlank { if (isStreaming) "…" else message.text },
                            modifier = Modifier.weight(1f, fill = false),
                            color =
                                when {
                                    message.isError -> MaterialTheme.colorScheme.onErrorContainer
                                    isUser -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            renderMarkdown = !isUser && !message.isError && !isStreaming,
                        )
                        if (isStreaming) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StreamingIndicator()
                        }
                    }

                    message.translatedText?.let {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                    }

                    if (!message.isStreaming) {
                        val footerTint =
                            if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatTime(message.timestamp),
                                fontSize = 11.sp,
                                color = footerTint,
                            )
                            if (!message.isError) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onCopyToClipboard(message.text) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        "Copy",
                                        modifier = Modifier.size(16.dp),
                                        tint = footerTint,
                                    )
                                }
                            }
                            if (!message.isError && !isUser) {
                                if (message.isTranslating) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp).padding(6.dp), strokeWidth = 1.5.dp)
                                } else {
                                    IconButton(onClick = onTranslate, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Translate,
                                            "Translate",
                                            modifier = Modifier.size(16.dp),
                                            tint = footerTint,
                                        )
                                    }
                                }
                                if (isLastAssistantMessage) {
                                    IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            "Regenerate",
                                            modifier = Modifier.size(16.dp),
                                            tint = footerTint,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("U", color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScrollControls(
    canScrollDown: Boolean,
    onTopClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onBottomClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChatScrollIconButton(
                icon = Icons.Default.VerticalAlignTop,
                contentDescription = "Scroll to top",
                onClick = onTopClick,
            )
            ChatScrollIconButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Previous message",
                onClick = onPreviousClick,
            )
            ChatScrollIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Next message",
                onClick = onNextClick,
            )
            ChatScrollIconButton(
                icon = Icons.Default.VerticalAlignBottom,
                contentDescription = "Scroll to bottom",
                enabled = canScrollDown,
                onClick = onBottomClick,
            )
        }
    }
}

@Composable
private fun ChatScrollIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint =
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ChatMessageBody(
    text: String,
    color: Color,
    renderMarkdown: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!renderMarkdown) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        return
    }

    ChatMarkdownText(
        text = text,
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun ChatMarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseChatMarkdownBlocks(text) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is ChatMarkdownBlock.Paragraph -> {
                    ChatMarkdownInlineText(
                        text = block.text,
                        color = color,
                        fontSize = 15,
                        lineHeight = 20,
                    )
                }
                is ChatMarkdownBlock.Heading -> {
                    ChatMarkdownInlineText(
                        text = block.text,
                        color = color,
                        fontSize = when (block.level) {
                            1 -> 19
                            2 -> 17
                            else -> 16
                        },
                        lineHeight = when (block.level) {
                            1 -> 24
                            2 -> 22
                            else -> 21
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                is ChatMarkdownBlock.Bullet -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(6.dp))
                        ChatMarkdownInlineText(
                            text = block.text,
                            color = color,
                            fontSize = 15,
                            lineHeight = 20,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                is ChatMarkdownBlock.Numbered -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${block.number}.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(6.dp))
                        ChatMarkdownInlineText(
                            text = block.text,
                            color = color,
                            fontSize = 15,
                            lineHeight = 20,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                is ChatMarkdownBlock.Code -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = block.text,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            color = color,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMarkdownInlineText(
    text: String,
    color: Color,
    fontSize: Int,
    lineHeight: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val annotatedText =
        remember(text, color, codeBackground, fontWeight) {
            buildChatInlineMarkdown(text, color, codeBackground, fontWeight)
        }

    Text(
        text = annotatedText,
        modifier = modifier,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        color = color,
    )
}

private sealed interface ChatMarkdownBlock {
    data class Paragraph(val text: String) : ChatMarkdownBlock

    data class Heading(
        val level: Int,
        val text: String,
    ) : ChatMarkdownBlock

    data class Bullet(val text: String) : ChatMarkdownBlock

    data class Numbered(
        val number: String,
        val text: String,
    ) : ChatMarkdownBlock

    data class Code(val text: String) : ChatMarkdownBlock
}

private fun parseChatMarkdownBlocks(text: String): List<ChatMarkdownBlock> {
    if (text.isBlank()) return listOf(ChatMarkdownBlock.Paragraph(text))

    val blocks = mutableListOf<ChatMarkdownBlock>()
    val paragraphLines = mutableListOf<String>()
    val codeLines = mutableListOf<String>()
    var insideCodeBlock = false

    fun flushParagraph() {
        if (paragraphLines.isNotEmpty()) {
            blocks += ChatMarkdownBlock.Paragraph(paragraphLines.joinToString("\n"))
            paragraphLines.clear()
        }
    }

    fun flushCode() {
        blocks += ChatMarkdownBlock.Code(codeLines.joinToString("\n").trimEnd())
        codeLines.clear()
    }

    text.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.trimStart().startsWith("```")) {
            if (insideCodeBlock) {
                flushCode()
            } else {
                flushParagraph()
            }
            insideCodeBlock = !insideCodeBlock
            return@forEach
        }

        if (insideCodeBlock) {
            codeLines += rawLine
            return@forEach
        }

        if (line.isBlank()) {
            flushParagraph()
            return@forEach
        }

        val headingMatch = Regex("""^(#{1,6})\s+(.*)$""").find(line)
        val bulletMatch = Regex("""^\s*[-*+]\s+(.*)$""").find(line)
        val numberedMatch = Regex("""^\s*(\d+)[.)]\s+(.*)$""").find(line)

        when {
            headingMatch != null -> {
                flushParagraph()
                blocks +=
                    ChatMarkdownBlock.Heading(
                        level = headingMatch.groupValues[1].length,
                        text = headingMatch.groupValues[2].trim(),
                    )
            }
            bulletMatch != null -> {
                flushParagraph()
                blocks += ChatMarkdownBlock.Bullet(bulletMatch.groupValues[1].trim())
            }
            numberedMatch != null -> {
                flushParagraph()
                blocks +=
                    ChatMarkdownBlock.Numbered(
                        number = numberedMatch.groupValues[1],
                        text = numberedMatch.groupValues[2].trim(),
                    )
            }
            else -> paragraphLines += line
        }
    }

    if (insideCodeBlock) {
        flushCode()
    } else {
        flushParagraph()
    }

    return blocks.ifEmpty { listOf(ChatMarkdownBlock.Paragraph(text)) }
}

private fun buildChatInlineMarkdown(
    text: String,
    color: Color,
    codeBackground: Color,
    baseFontWeight: FontWeight?,
): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val codeRange = findDelimitedRange(text, cursor, "`", "`")
            val boldRange = findDelimitedRange(text, cursor, "**", "**")
            val italicRange = findDelimitedRange(text, cursor, "*", "*")
            val nextRange =
                listOfNotNull(
                    codeRange?.let { InlineRange.Code(it) },
                    boldRange?.let { InlineRange.Bold(it) },
                    italicRange?.let { InlineRange.Italic(it) },
                ).minByOrNull { it.range.first }

            if (nextRange == null) {
                appendStyledPlain(text.substring(cursor), color, baseFontWeight)
                break
            }

            if (nextRange.range.first > cursor) {
                appendStyledPlain(text.substring(cursor, nextRange.range.first), color, baseFontWeight)
            }

            when (nextRange) {
                is InlineRange.Code -> {
                    withStyle(
                        SpanStyle(
                            color = color,
                            background = codeBackground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = baseFontWeight,
                        ),
                    ) {
                        append(nextRange.content(text))
                    }
                }
                is InlineRange.Bold -> {
                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                        append(nextRange.content(text))
                    }
                }
                is InlineRange.Italic -> {
                    withStyle(SpanStyle(color = color, fontStyle = FontStyle.Italic, fontWeight = baseFontWeight)) {
                        append(nextRange.content(text))
                    }
                }
            }

            cursor = nextRange.range.last + 1
        }
    }

private fun AnnotatedString.Builder.appendStyledPlain(
    text: String,
    color: Color,
    fontWeight: FontWeight?,
) {
    withStyle(SpanStyle(color = color, fontWeight = fontWeight)) {
        append(text)
    }
}

private sealed class InlineRange(
    val range: IntRange,
    private val delimiterLength: Int,
) {
    class Code(range: IntRange) : InlineRange(range, delimiterLength = 1)

    class Bold(range: IntRange) : InlineRange(range, delimiterLength = 2)

    class Italic(range: IntRange) : InlineRange(range, delimiterLength = 1)

    fun content(source: String): String =
        source.substring(range.first + delimiterLength, range.last + 1 - delimiterLength)
}

private fun findDelimitedRange(
    text: String,
    startIndex: Int,
    startDelimiter: String,
    endDelimiter: String,
): IntRange? {
    var start = text.indexOf(startDelimiter, startIndex)
    while (start >= 0) {
        if (startDelimiter == "*" && text.getOrNull(start + 1) == '*') {
            start = text.indexOf(startDelimiter, start + 1)
            continue
        }
        val end = text.indexOf(endDelimiter, start + startDelimiter.length)
        if (end >= 0) return start until end + endDelimiter.length
        start = text.indexOf(startDelimiter, start + startDelimiter.length)
    }
    return null
}

@Composable
fun RoleSelectorDialog(
    roles: List<RoleItem>,
    onDismiss: () -> Unit,
    onRoleSelected: (RoleFile) -> Unit,
) {
    val backStack = remember { mutableStateListOf<RoleFolder>() }
    val currentItems: List<RoleItem> by remember(backStack.size) {
        derivedStateOf {
            if (backStack.isEmpty()) roles else backStack.last().children
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp),
        ) {
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (backStack.isNotEmpty()) {
                        IconButton(onClick = { backStack.removeAt(backStack.lastIndex) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = backStack.lastOrNull()?.name ?: "Available Roles",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()
                if (roles.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Folder with roles is not selected in settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (currentItems.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No roles found in this folder.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(currentItems, key = { it.path }) { item ->
                            when (item) {
                                is RoleFolder -> {
                                    ListItem(
                                        headlineContent = { Text(item.name) },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = "Folder",
                                            )
                                        },
                                        modifier = Modifier.clickable { backStack.add(item) },
                                    )
                                }
                                is RoleFile -> {
                                    ListItem(
                                        headlineContent = { Text(item.name, fontWeight = FontWeight.Medium) },
                                        supportingContent = {
                                            Text(
                                                item.prompt.take(100) + if (item.prompt.length > 100) "..." else "",
                                                maxLines = 2,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Article,
                                                contentDescription = "Role file",
                                            )
                                        },
                                        modifier = Modifier.clickable { onRoleSelected(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelSelectorDialog(
    modelsState: ModelsState,
    onDismiss: () -> Unit,
    onModelSelected: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp),
        ) {
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Select a Model",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    when (modelsState) {
                        is ModelsState.Loading -> CircularProgressIndicator()
                        is ModelsState.Error ->
                            Text(
                                modelsState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                        is ModelsState.Success -> {
                            if (modelsState.models.isEmpty()) {
                                Text("No models found", modifier = Modifier.padding(16.dp))
                            } else {
                                LazyColumn {
                                    items(modelsState.models) { model ->
                                        ListItem(
                                            headlineContent = { Text(model) },
                                            modifier = Modifier.clickable { onModelSelected(model) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemperatureDialog(
    currentTemperature: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
) {
    var temp by remember { mutableStateOf(currentTemperature) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Model Temperature", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Controls randomness. Lower values make the model more deterministic, higher values make it more creative.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    text = String.format(Locale.US, "%.2f", temp),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = temp,
                    onValueChange = { temp = it },
                    valueRange = 0f..2f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(temp) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    Box(
        modifier =
            Modifier
                .size(8.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    RoundedCornerShape(4.dp),
                ),
    )
}

@Composable
fun PendingResponseIndicator() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            text = "Preparing response…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    isLoading: Boolean,
    roleTitle: String,
    temperature: Float,
    modelName: String,
    onModelClick: () -> Unit,
    onRoleClick: () -> Unit,
    onTemperatureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InputChip(
                    onClick = onRoleClick,
                    label = { Text(roleTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f, fill = false),
                )

                InputChip(
                    onClick = onModelClick,
                    label = {
                        Text(
                            modelName.substringBefore(':'),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f, fill = false),
                )

                InputChip(
                    onClick = onTemperatureClick,
                    label = { Text(String.format(Locale.US, "%.1f", temperature)) },
                    icon = Icons.Default.Thermostat,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send,
                        ),
                    keyboardActions = KeyboardActions(onSend = { if (value.isNotBlank()) onSendClick() }),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    maxLines = 4,
                )

                AnimatedContent(
                    targetState = isLoading,
                    label = "send_button",
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    },
                ) { loading ->
                    if (loading) {
                        IconButton(
                            onClick = onStopClick,
                            modifier = Modifier.size(48.dp),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(24.dp))
                        }
                    } else {
                        IconButton(
                            onClick = onSendClick,
                            enabled = value.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = if (value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (value.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                label()
            }
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Hi! I'm your AI assistant",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ask anything and I'll help you quickly.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun EditTitleDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var newTitle by remember { mutableStateOf(currentTitle) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Edit Title", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("New title") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(newTitle) }) { Text("Save") }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
