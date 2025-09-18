package com.romankozak.forwardappmobile.ui.screens.noteedit

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.notesEditors.StyledTextFieldWrapper
import com.romankozak.forwardappmobile.ui.components.notesEditors.WebViewMarkdownViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavController,
    viewModel: NoteEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isEditMode by remember { mutableStateOf(value = true) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is NoteEditEvent.NavigateBack -> {
                        event.message?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("refresh_needed", true)

                        navController.popBackStack()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewNote) "Нова нотатка" else "Редагувати нотатку") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Режим перегляду" else "Режим редагування",
                        )
                    }
                    if (uiState.isSaveButtonEnabled) {
                        TextButton(onClick = viewModel::onSave) {
                            Text("Зберегти")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding(),
        ) {
            if (!uiState.isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Заголовок") },
                    singleLine = true,
                    isError = uiState.error != null,
                )

                FinalMarkdownEditor(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    isEditMode = isEditMode,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )

                val footerText = uiState.error ?: "${uiState.content.text.length} символів"
                val footerColor = if (uiState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

                Text(
                    text = footerText,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = footerColor,
                )
            }
        }
    }
}

@Composable
fun FinalMarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = isEditMode,
        label = "EditorViewSwitcher",
        modifier = modifier,
    ) { isEditing ->
        if (isEditing) {
            StyledTextFieldWrapper(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else {
            if (value.text.isNotBlank()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            WebViewMarkdownViewer(ctx).apply {
                                layoutParams =
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                            }
                        },
                        update = { viewer ->
                            viewer.renderMarkdown(value.text)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Попередній перегляд буде тут", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}