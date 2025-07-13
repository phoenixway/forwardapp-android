package com.romankozak.forwardappmobile

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel
) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val settingsRepo = SettingsRepository(context.applicationContext)
    val viewModel: GoalListViewModel = viewModel(
        factory = GoalListViewModelFactory(context.applicationContext as Application, db.goalListDao(), db.goalDao(), settingsRepo)
    )

    val hierarchy by viewModel.listHierarchy.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is GoalListUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navController.navigate("sync_screen")
                }
                is GoalListUiEvent.NavigateToDetails -> navController.navigate("goal_detail_screen/${event.listId}")
                is GoalListUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is GoalListUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backlogs") },
                actions = {
                    // --- ДОДАНО: Кнопка "Додати новий список" ---
                    IconButton(onClick = { viewModel.onAddNewListRequest() }) {
                        Icon(Icons.Default.Add, contentDescription = "Додати новий список")
                    }

                    // Кнопка глобального пошуку
                    IconButton(onClick = { viewModel.onShowSearchDialog() }) {
                        Icon(Icons.Default.Search, contentDescription = "Глобальний пошук")
                    }

                    // Меню "три крапки"
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Запустити Wi-Fi сервер") }, onClick = {
                            viewModel.onShowWifiServerDialog()
                            menuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Імпорт з Wi-Fi") }, onClick = {
                            viewModel.onShowWifiImportDialog()
                            menuExpanded = false
                        })
                    }
                }
            )
        },
        // --- ВИДАЛЕНО: Плаваюча кнопка (FAB) ---
        // floatingActionButton = { ... }
    ) { paddingValues ->
        if (hierarchy.topLevelLists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Створіть свій перший список")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                hierarchy.topLevelLists.forEach { list ->
                    item(key = list.id) {
                        GoalListRow(
                            list = list,
                            level = 0,
                            hasChildren = hierarchy.childMap.containsKey(list.id),
                            onListClick = { viewModel.onListClicked(list.id) },
                            onToggleExpanded = { viewModel.onToggleExpanded(it) },
                            onMenuRequested = { viewModel.onMenuRequested(it) }
                        )
                    }
                    if (list.isExpanded) {
                        val children = hierarchy.childMap[list.id] ?: emptyList()
                        items(children, key = { "child-${it.id}" }) { child ->
                            GoalListRow(
                                list = child,
                                level = 1,
                                hasChildren = hierarchy.childMap.containsKey(child.id),
                                onListClick = { viewModel.onListClicked(child.id) },
                                onToggleExpanded = { viewModel.onToggleExpanded(it) },
                                onMenuRequested = { viewModel.onMenuRequested(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Обробка всіх діалогових вікон ---
    when (val state = dialogState) {
        is DialogState.Hidden -> {}
        is DialogState.AddList -> {
            val title = if (state.parentId == null) "Створити новий список" else "Створити підсписок"
            AddListDialog(
                title = title,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name -> viewModel.onAddList(name, state.parentId) }
            )
        }
        is DialogState.ContextMenu -> {
            ContextMenuDialog(
                list = state.list,
                onDismissRequest = { viewModel.dismissDialog() },
                onMoveRequest = { viewModel.onMoveListRequest(it) },
                onAddSublistRequest = { viewModel.onAddSublistRequest(it) },
                onDeleteRequest = { viewModel.onDeleteRequest(it) },
                onRenameRequest = { viewModel.onRenameRequest(it) }
            )
        }
        is DialogState.MoveList -> {
            MoveListDialog(
                listToMove = state.list,
                allListsFlat = hierarchy.allLists,
                topLevelLists = hierarchy.topLevelLists,
                childMap = hierarchy.childMap,
                onDismiss = { viewModel.dismissDialog() },
                onConfirmMove = { newParentId -> viewModel.onMoveListConfirmed(state.list, newParentId) }
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Видалити список?") },
                text = { Text("Ви впевнені, що хочете видалити '${state.list.name}' та всі його підсписки і цілі? Цю дію неможливо буде скасувати.") },
                confirmButton = { Button(onClick = { viewModel.onDeleteListConfirmed(state.list) }) { Text("Видалити") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Скасувати") } }
            )
        }
        is DialogState.RenameList -> {
            RenameListDialog(
                currentName = state.list.name,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName -> viewModel.onRenameListConfirmed(state.list, newName) }
            )
        }
    }

    if (showWifiServerDialog) {
        WifiServerDialog(address = wifiServerAddress, onDismiss = { viewModel.onDismissWifiServerDialog() })
    }

    if (showWifiImportDialog) {
        WifiImportDialog(onDismiss = { viewModel.onDismissWifiImportDialog() }, onConfirm = { address -> viewModel.performWifiImport(address) })
    }

    if (showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { viewModel.onDismissSearchDialog() },
            onConfirm = { query -> viewModel.onPerformGlobalSearch(query) }
        )
    }
}

@Composable
fun WifiServerDialog(address: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wi-Fi Сервер") },
        text = {
            if (address != null) {
                ServerInfo(address = address)
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
fun WifiImportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val viewModel: GoalListViewModel = viewModel()
    val desktopAddress by viewModel.desktopAddress.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Імпорт з Wi-Fi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text("Введіть IP-адресу та порт десктоп-додатку:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desktopAddress,
                    onValueChange = { viewModel.onDesktopAddressChange(it) },
                    placeholder = { Text("Напр. 192.168.1.5:8080") },
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Скасувати")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(desktopAddress) },
                        enabled = desktopAddress.isNotBlank()
                    ) {
                        Text("Отримати дані")
                    }
                }
            }
        }
    }
}

@Composable
fun RenameListDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейменувати список") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Нова назва") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != currentName
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun GlobalSearchDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Глобальний пошук") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Що шукати?") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("Знайти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}