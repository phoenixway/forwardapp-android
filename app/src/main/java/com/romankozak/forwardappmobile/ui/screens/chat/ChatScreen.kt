package com.romankozak.forwardappmobile.ui.screens.chat

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.domain.aichat.RoleFile
import com.romankozak.forwardappmobile.domain.aichat.RoleFolder
import com.romankozak.forwardappmobile.domain.aichat.RoleItem
import com.romankozak.forwardappmobile.ui.ModelsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AI_CHAT_DEBUG"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showRoleSelectorDialog by remember { mutableStateOf(false) }
    var showTemperatureDialog by remember { mutableStateOf(false) }
    var showModelSelectorDialog by remember { mutableStateOf(false) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                delay(150)
                Log.d(TAG, "[EFFECT 1] Scrolling to new message, index: ${uiState.messages.size - 1}")
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                Log.d(TAG, "[EFFECT 2] Bringing streaming text into view.")
                bringIntoViewRequester.bringIntoView()
            }
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

    Scaffold(
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
                            Text(uiState.roleTitle, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            if (uiState.messages.any { it.isStreaming }) {
                                Text(
                                    "Typing...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Chat") },
                                onClick = {
                                    viewModel.startNewChat()
                                    showMenu = false
                                },
                            )
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
                                    val chatText = viewModel.exportChat()
                                    val sendIntent =
                                        Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, chatText)
                                            type = "text/plain"
                                        }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                    showMenu = false
                                },
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    navController.navigate("settings_screen")
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
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 4.dp),
            ) {
                if (uiState.messages.isEmpty() && !uiState.messages.any { it.isStreaming }) {
                    item { EmptyStateMessage() }
                }

                itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { index, message ->
                    val isLastAssistantMessage = !message.isFromUser && index == uiState.messages.lastIndex
                    val isLastMessage = index == uiState.messages.lastIndex

                    MessageBubble(
                        message = message,
                        isLastAssistantMessage = isLastAssistantMessage,
                        bringIntoViewRequester = if (isLastMessage) bringIntoViewRequester else null,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isLastAssistantMessage: Boolean,
    onCopyToClipboard: (String) -> Unit,
    onRegenerate: () -> Unit,
    onTranslate: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester? = null,
) {
    val isUser = message.isFromUser

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
                        Text(
                            text = if (message.text.isBlank() && message.isStreaming) "..." else message.text,
                            modifier = Modifier.weight(1f, fill = false),
                            color =
                                when {
                                    message.isError -> MaterialTheme.colorScheme.onErrorContainer
                                    isUser -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        )
                        if (message.isStreaming && !isUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StreamingIndicator()
                        }
                    }

                    message.translatedText?.let {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                    }

                    if (!message.isStreaming) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatTime(message.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!message.isError && !isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    onCopyToClipboard(message.text)
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        "Copy",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (message.isTranslating) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp).padding(6.dp), strokeWidth = 1.5.dp)
                                } else {
                                    IconButton(onClick = onTranslate, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Translate,
                                            "Translate",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (isLastAssistantMessage) {
                                    IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            "Regenerate",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
        bringIntoViewRequester?.let {
            Box(
                modifier =
                    Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .bringIntoViewRequester(it),
            )
        }
    }
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
                Divider()
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
                        items(items = currentItems, key = { it.path }) { item ->
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
                                        headlineContent = {
                                            Text(
                                                item.name,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                item.prompt.take(100) + if (item.prompt.length > 100) "..." else "",
                                                maxLines = 2,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Article,
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
                Divider()

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
                                    items(modelsState.models.size, key = { modelsState.models[it] }) { index ->
                                        val model = modelsState.models[index]
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
                    text = String.format("%.2f", temp),
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
                            modelName.split(":")[0],
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f, fill = false),
                )

                InputChip(
                    onClick = onTemperatureClick,
                    label = { Text(String.format("%.1f", temperature)) },
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
                    placeholder = { Text("Напишіть повідомлення...") },
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
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(24.dp)
                            )
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
            "Привіт! Я ваш AI-асистент",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Поставте будь-яке питання, і я допоможу вам знайти відповідь",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
