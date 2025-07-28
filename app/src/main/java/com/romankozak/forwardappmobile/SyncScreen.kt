package com.romankozak.forwardappmobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.romankozak.forwardappmobile.ServerInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    syncDataViewModel: SyncDataViewModel,
    onSyncComplete: () -> Unit
) {
    val context = LocalContext.current

    // --- ВИПРАВЛЕНО: Створюємо SyncRepository і передаємо його у фабрику ---
    // Оскільки SyncRepository тепер також є Hilt-класом (@Singleton),
    // в ідеалі ми мали б його ін'єктувати, але для швидкого виправлення
    // створимо його тут вручну.
    val db = AppDatabase.getDatabase(context)
    val goalRepository = GoalRepository(db.goalDao(), db.goalListDao())
    val syncRepo = SyncRepository(goalRepository)

    val viewModel: SyncViewModel = viewModel(factory = SyncViewModelFactory(syncRepo))

    val report by viewModel.report.collectAsState()
    val approvedIds by viewModel.approvedChangeIds.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.createReport(syncDataViewModel)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Схвалення Синхронізації") }) },
        bottomBar = {
            Button(
                onClick = { viewModel.applyChanges(onSyncComplete) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = report != null && error == null
            ) {
                Text("Застосувати схвалені зміни")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Помилка синхронізації",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.clearError()
                            onSyncComplete()
                        }) {
                            Text("OK")
                        }
                    }
                }

                report == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                report!!.changes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Не знайдено змін для синхронізації.")
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
                        items(report!!.changes, key = { it.id }) { change ->
                            SyncChangeItem(
                                change = change,
                                isChecked = change.id in approvedIds,
                                onToggle = { viewModel.toggleApproval(change.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncChangeItem(change: SyncChange, isChecked: Boolean, onToggle: () -> Unit) {
    val (icon, color) = when (change) {
        is SyncChange.Add -> Icons.Default.AddCircle to MaterialTheme.colorScheme.primary
        is SyncChange.Update -> Icons.Default.Refresh to MaterialTheme.colorScheme.secondary
        is SyncChange.Remove -> Icons.Default.Delete to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggle),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = change.type, tint = color)
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${change.type} ${change.entityType}", style = MaterialTheme.typography.titleMedium)
                Text(text = change.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Checkbox(checked = isChecked, onCheckedChange = { onToggle() })
        }
    }
}