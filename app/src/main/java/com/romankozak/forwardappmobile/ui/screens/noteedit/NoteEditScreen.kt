// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/noteedit/NoteEditScreen.kt
package com.romankozak.forwardappmobile.ui.screens.noteedit

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.MarkdownEditorViewer
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteEditEvent.NavigateBack -> {
                    event.message?.let {
                        scope.launch {
                            snackbarHostState.showSnackbar(it)
                        }
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
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewNote) "Нова нотатка" else "Редагувати",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                    AnimatedContent(
                        targetState = uiState.isSaveButtonEnabled,
                        label = "save_button_animation",
                        modifier = Modifier.padding(end = 8.dp),
                    ) { isEnabled ->
                        if (isEnabled) {
                            Button(
                                onClick = { viewModel.onSave() },
                                shape = MaterialTheme.shapes.small,
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
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = {
                        Text(
                            "Заголовок (необов'язково)",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    placeholder = {
                        Text(
                            "Наприклад: Ідеї для проекту",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    ),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    MarkdownEditorViewer(
                        value = uiState.content,
                        onValueChange = viewModel::onContentChange,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (uiState.content.text.isEmpty()) {
                        Text(
                            text = "Почніть писати тут... Підтримується Markdown: **жирний**, *курсив*, списки тощо.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            // --- ПОЧАТОК ЗМІН ---
                            modifier = Modifier
                                // .fillMaxSize() // 1. Видалено: цей модифікатор не потрібен для плейсхолдера
                                .align(Alignment.TopStart)
                                // 2. Змінено padding: додано значний верхній відступ,
                                // щоб змістити плейсхолдер нижче кнопок "Редактор"/"Перегляд".
                                .padding(start = 16.dp, top = 52.dp, end = 16.dp, bottom = 16.dp),
                            // --- КІНЕЦЬ ЗМІН ---
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AnimatedVisibility(visible = uiState.error != null) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .animateContentSize()
                                .weight(1f, fill = false),
                        )
                    }

                    Text(
                        text = "${uiState.content.text.length}/5000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}