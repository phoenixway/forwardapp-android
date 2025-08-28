// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/noteedit/NoteEditScreen.kt
package com.romankozak.forwardappmobile.ui.screens.noteedit

import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.notesEditors.StyledTextFieldWrapper
import com.romankozak.forwardappmobile.ui.components.notesEditors.WebViewMarkdownViewer
import com.romankozak.forwardappmobile.ui.shared.NavigationResultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavController,
    viewModel: NoteEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isEditMode by remember { mutableStateOf(true) }

    // ПОВЕРНУЛИ: Необхідні елементи для обробки подій (Snackbar та CoroutineScope)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ПОВЕРНУЛИ: Надійний обробник подій, який ми втратили
    val navGraphEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry("app_graph")
    }
    val resultViewModel: NavigationResultViewModel = viewModel(navGraphEntry)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is NoteEditEvent.NavigateBack -> {
                        event.message?.let {
                            scope.launch { snackbarHostState.showSnackbar(it) }
                        }
                        // Сигнал для оновлення батьківського екрана
                        resultViewModel.setResult("refresh_needed", true)
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    Scaffold(
        // ПОВЕРНУЛИ: Підключення SnackbarHost до Scaffold
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewNote) "Нова нотатка" else "Редагувати") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Режим перегляду" else "Режим редагування"
                        )
                    }
                    if (uiState.isSaveButtonEnabled) {
                        Button(onClick = viewModel::onSave) {
                            Text("Зберегти")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Цей Column керує загальною розміткою і відступом від клавіатури.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            if (!uiState.isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Поле для заголовка
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Заголовок") },
                    singleLine = true
                )

                // Повноцінний редактор, який займає весь залишок місця
                FinalMarkdownEditor(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Лічильник символів
                Text(
                    text = "${uiState.content.text.length}/5000",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Фінальна версія редактора, яка поєднує надійний StyledTextFieldWrapper
 * для редагування та WebView для перегляду.
 */
@Composable
fun FinalMarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isEditMode,
        label = "FinalEditor",
        modifier = modifier
    ) { isEditing ->
        if (isEditing) {
            StyledTextFieldWrapper(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        } else {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                AndroidView(
                    factory = { ctx ->
                        WebViewMarkdownViewer(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { viewer ->
                        viewer.renderMarkdown(value.text)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}