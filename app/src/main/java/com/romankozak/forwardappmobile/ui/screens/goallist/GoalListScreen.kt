package com.romankozak.forwardappmobile.ui.screens.goallist

import android.content.pm.PackageInfo
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.dialogs.MoveListDialog
import com.romankozak.forwardappmobile.ui.dialogs.RenameListDialog
import com.romankozak.forwardappmobile.ui.dialogs.SettingsDialog
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.components.GoalListRow
import com.romankozak.forwardappmobile.ui.dialogs.AddListDialog
import com.romankozak.forwardappmobile.ui.dialogs.ContextMenuDialog
import com.romankozak.forwardappmobile.ui.dialogs.GlobalSearchDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    // Тепер ми отримуємо ViewModel, створену Hilt, як параметр
    viewModel: GoalListViewModel = hiltViewModel()
) {
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
                is GoalListUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is GoalListUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forward ") },
                actions = {
                    IconButton(onClick = { viewModel.onAddNewListRequest() }) {
                        Icon(Icons.Default.Add, contentDescription = "Додати новий список")
                    }
                    IconButton(onClick = { viewModel.onShowSearchDialog() }) {
                        Icon(Icons.Default.Search, contentDescription = "Глобальний пошук")
                    }
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
                        Divider()
                        DropdownMenuItem(text = { Text("Налаштування") }, onClick = {
                            viewModel.onShowSettingsDialog()
                            menuExpanded = false
                        })
                        // --- ДОДАНО: Новий пункт меню ---
                        DropdownMenuItem(text = { Text("Про додаток") }, onClick = {
                            viewModel.onShowAboutDialog()
                            menuExpanded = false
                        })
                    }
                }
            )
        }
    ) { paddingValues ->
        if (hierarchy.topLevelLists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Створіть свій перший список")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                hierarchy.topLevelLists.forEach { list ->
                    renderListRecursively(
                        list = list,
                        level = 0,
                        hierarchyData = hierarchy,
                        onListClick = { viewModel.onListClicked(it) },
                        onToggleExpanded = { viewModel.onToggleExpanded(it) },
                        onMenuRequested = { viewModel.onMenuRequested(it) }
                    )
                }
            }
        }
    }

    HandleDialogs(
        dialogState = dialogState,
        hierarchy = hierarchy,
        viewModel = viewModel,
        showWifiServerDialog = showWifiServerDialog,
        wifiServerAddress = wifiServerAddress,
        showWifiImportDialog = showWifiImportDialog,
        showSearchDialog = showSearchDialog
    )
}

private fun LazyListScope.renderListRecursively(
    list: GoalList,
    level: Int,
    hierarchyData: ListHierarchyData,
    onListClick: (String) -> Unit,
    onToggleExpanded: (GoalList) -> Unit,
    onMenuRequested: (GoalList) -> Unit
) {
    item(key = list.id) {
        GoalListRow(
            list = list,
            level = level,
            hasChildren = hierarchyData.childMap.containsKey(list.id),
            onListClick = onListClick,
            onToggleExpanded = onToggleExpanded,
            onMenuRequested = onMenuRequested
        )
    }
    if (list.isExpanded) {
        val children = hierarchyData.childMap[list.id] ?: emptyList()
        children.forEach { child ->
            renderListRecursively(
                list = child,
                level = level + 1,
                hierarchyData = hierarchyData,
                onListClick = onListClick,
                onToggleExpanded = onToggleExpanded,
                onMenuRequested = onMenuRequested
            )
        }
    }
}

@Composable
private fun HandleDialogs(
    dialogState: DialogState,
    hierarchy: ListHierarchyData,
    viewModel: GoalListViewModel,
    showWifiServerDialog: Boolean,
    wifiServerAddress: String?,
    showWifiImportDialog: Boolean,
    showSearchDialog: Boolean
) {
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()

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
                onConfirmMove = { newParentId ->
                    viewModel.onMoveListConfirmed(
                        state.list,
                        newParentId
                    )
                }
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
        is DialogState.AppSettings -> {
            SettingsDialog(
                initialVaultName = obsidianVaultName,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { newName -> viewModel.onSaveSettings(newName) }
            )
        }
        // --- ДОДАНО: Обробка діалогу "Про додаток" ---
        is DialogState.AboutApp -> {
            AboutAppDialog(onDismiss = { viewModel.dismissDialog() })
        }
    }

    if (showWifiServerDialog) {
        WifiServerDialog(
            address = wifiServerAddress,
            onDismiss = { viewModel.onDismissWifiServerDialog() })
    }

    if (showWifiImportDialog) {
        val desktopAddress by viewModel.desktopAddress.collectAsState()
        WifiImportDialog(
            desktopAddress = desktopAddress,
            onAddressChange = { viewModel.onDesktopAddressChange(it) },
            onDismiss = { viewModel.onDismissWifiImportDialog() },
            onConfirm = { address -> viewModel.performWifiImport(address) }
        )
    }

    if (showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { viewModel.onDismissSearchDialog() },
            onConfirm = { query -> viewModel.onPerformGlobalSearch(query) }
        )
    }
}

// --- ДОДАНО: Новий Composable для діалогу "Про додаток" ---
@Composable
private fun AboutAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val packageInfo: PackageInfo? = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Про додаток Forward") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Версія: ${packageInfo?.versionName ?: "N/A"}")
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: -1
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toLong() ?: -1
                }
                if (versionCode != -1L) {
                    Text("Збірка: $versionCode")
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
