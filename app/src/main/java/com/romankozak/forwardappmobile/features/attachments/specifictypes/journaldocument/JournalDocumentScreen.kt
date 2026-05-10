package com.romankozak.forwardappmobile.features.attachments.specifictypes.journaldocument

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog.parseJournalLine
import kotlinx.coroutines.flow.collect

@Composable
private fun markerColor(marker: String?): Color =
    when (marker) {
        "*", "@", "t" -> Color(0xFF534AB7)
        ">" -> Color(0xFF0F6E56)
        "?" -> Color(0xFF854F0B)
        "!", "!!", "!!!" -> Color(0xFF993C1D)
        "+", "-" -> Color(0xFF3B6D11)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDocumentScreen(
    navController: NavController,
    viewModel: JournalDocumentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var newEntryText by remember { mutableStateOf("") }
    var reorderMode by remember { mutableStateOf(false) }
    var autoScrollNonce by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedLine by remember { mutableStateOf<String?>(null) }
    var selectedLineIndex by remember { mutableStateOf<Int?>(null) }
    var renameDraft by remember(uiState.document?.id, uiState.document?.name) {
        mutableStateOf(uiState.document?.name.orEmpty())
    }

    val entriesCount = uiState.document?.content.orEmpty().lineSequence().count { it.isNotBlank() }
    val lines =
        uiState.document?.content
            .orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

    LaunchedEffect(autoScrollNonce, lines.size) {
        if (autoScrollNonce > 0 && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                JournalDocumentEvent.NavigateBack -> navController.popBackStack()
                is JournalDocumentEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистити журнал") },
            text = { Text("Усі записи буде видалено.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearDocument()
                }) { Text("Очистити") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Скасувати") }
            },
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename document") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Document name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameDocument(renameDraft)
                        showRenameDialog = false
                    },
                    enabled = renameDraft.trim().isNotBlank(),
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Скасувати")
                }
            },
        )
    }

    if (selectedLine != null && selectedLineIndex != null) {
        val scope = rememberCoroutineScope()
        val line = selectedLine!!
        val index = selectedLineIndex!!
        JournalEntryActionsBottomSheet(
            line = line,
            onDismiss = {
                selectedLine = null
                selectedLineIndex = null
            },
            onSave = { updatedText -> viewModel.updateLine(index, updatedText) },
            onDelete = { viewModel.deleteLine(index) },
            onEnterReorderMode = { reorderMode = true },
            scope = scope,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(uiState.document?.name?.ifBlank { "Journal log" } ?: "Journal log")
                        Text(
                            text = "$entriesCount записів",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Більше дій")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename document") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    renameDraft = uiState.document?.name.orEmpty()
                                    showRenameDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Експортувати") },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.exportDocument()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (reorderMode) "Завершити порядок" else "Змінити порядок") },
                                leadingIcon = { Icon(Icons.Default.DragHandle, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    reorderMode = !reorderMode
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Очистити журнал", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showClearDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            JournalComposerBar(
                value = newEntryText,
                onValueChange = { newEntryText = it },
                onSubmit = {
                    val normalized = newEntryText.trim()
                    if (normalized.isBlank()) return@JournalComposerBar
                    viewModel.addEntry(normalized)
                    newEntryText = ""
                    autoScrollNonce += 1
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
        ) {
            itemsIndexed(lines) { index, line ->
                EntryRow(
                    line = line,
                    modifier = Modifier.fillMaxWidth(),
                    interactive = true,
                    onLongClick = {
                        selectedLine = line
                        selectedLineIndex = index
                    },
                )
                if (index < lines.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EntryRow(
    line: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    if (isJournalHorizontalSplitter(line)) {
        Row(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = "///",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        return
    }

    val parsed = parseJournalLine(line)
    val displaySymbol = parsed?.marker ?: "·"
    val bodyText = parsed?.text ?: line.trim()
    val color = markerColor(parsed?.marker)

    Row(
        modifier = modifier
            .then(
                if (interactive && onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick,
                    )
                } else Modifier,
            )
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                ) {}
            }
            Text(
                text = displaySymbol,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp, bottom = 10.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp,
            )
        }
    }
}

private fun isJournalHorizontalSplitter(line: String): Boolean = line.trim() == "---"

@Composable
private fun JournalComposerBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("* ціль  > крок  ? питання  ! блокер") },
                maxLines = 3,
                shape = MaterialTheme.shapes.large,
            )
            if (value.isNotBlank()) {
                IconButton(onClick = onSubmit) {
                    Icon(Icons.Default.Send, contentDescription = "Надіслати")
                }
            }
        }
    }
}
