// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/noteedit/NoteEditScreen.kt
package com.romankozak.forwardappmobile.ui.screens.noteedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.ui.components.notesEditors.MarkdownEditorViewer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavController,
    viewModel: NoteEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isEditMode by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteEditEvent.NavigateBack -> {
                    event.message?.let {
                        scope.launch { snackbarHostState.showSnackbar(it) }
                    }
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewNote) "Нова нотатка" else "Редагувати",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = stringResource(
                                if (isEditMode) R.string.toggle_to_preview_mode else R.string.toggle_to_edit_mode,
                            ),
                        )
                    }

                    AnimatedContent(
                        targetState = uiState.isSaveButtonEnabled,
                        label = "save_button_animation",
                        modifier = Modifier.padding(end = 8.dp),
                    ) { isEnabled ->
                        if (isEnabled) {
                            Button(
                                onClick = { viewModel.onSave() },
                                shape = RoundedCornerShape(50),
                            ) {
                                Text("Зберегти", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            Text(
                                text = "Збережено",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { paddingValues ->
        if (!uiState.isReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            NoteEditor(
                title = uiState.title,
                onTitleChange = viewModel::onTitleChange,
                content = uiState.content,
                onContentChange = viewModel::onContentChange,
                isEditMode = isEditMode,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
fun NoteEditor(
    title: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        // 🔹 спільний горизонтальний відступ для обох полів
        val horizontalPadding = 16.dp

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Заголовок (необов'язково)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 8.dp),
            textStyle = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            singleLine = false,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
        )

        MarkdownEditorViewer(
            value = content,
            onValueChange = onContentChange,
            isEditMode = isEditMode,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = horizontalPadding, end = horizontalPadding)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.medium
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 20.dp), // трохи відступ праворуч
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${content.text.length}/5000",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
