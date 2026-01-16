package com.romankozak.forwardappmobile.features.sync

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.data.repository.ChangeType
import com.romankozak.forwardappmobile.data.repository.SyncChange
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel

private object ChangeTypeMetadata {
    data class Metadata(
        val title: String,
        val icon: ImageVector,
        val color: Color,
    )

    @Composable
    fun get(changeType: ChangeType): Metadata =
        when (changeType) {
            ChangeType.Add -> Metadata("Додавання", Icons.Default.AddCircle, MaterialTheme.colorScheme.primary)
            ChangeType.Update -> Metadata("Оновлення", Icons.Default.Refresh, MaterialTheme.colorScheme.secondary)
            ChangeType.Move -> Metadata("Переміщення", Icons.Default.SwapHoriz, MaterialTheme.colorScheme.tertiary)
            ChangeType.Delete -> Metadata("Видалення", Icons.Default.Delete, MaterialTheme.colorScheme.error)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    syncDataViewModel: SyncDataViewModel,
    onSyncComplete: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsState()
    val approvedIds by viewModel.approvedChangeIds.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.createReport(syncDataViewModel)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Огляд та схвалення змін") }) },
        bottomBar = {
            Button(
                onClick = { viewModel.applyChanges(onSyncComplete) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                enabled = (report != null && error == null),
            ) {
                Text("Застосувати зміни (${approvedIds.size})")
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                error != null -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Помилка синхронізації",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
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
                    val groupedChanges =
                        remember(report) {
                            report!!
                                .changes
                                .groupBy { it.type }
                                .toSortedMap(compareBy { it.ordinal })
                        }

                    val expandedGroups =
                        remember {
                            mutableStateOf(groupedChanges.keys)
                        }

                    Column {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(onClick = { viewModel.selectAllChanges() }) { Text("Обрати все") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { viewModel.selectRecommendedChanges() }) { Text("Рекомендовані") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { viewModel.deselectAllChanges() }) { Text("Зняти вибір") }
                        }
                        HorizontalDivider()
                        LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
                            groupedChanges.forEach { (changeType, changesInGroup) ->
                                val isExpanded = changeType in expandedGroups.value
                                item {
                                    GroupHeader(
                                        changeType = changeType,
                                        count = changesInGroup.size,
                                        isExpanded = isExpanded,
                                        onToggle = {
                                            val current = expandedGroups.value.toMutableSet()
                                            if (isExpanded) current.remove(changeType) else current.add(changeType)
                                            expandedGroups.value = current
                                        },
                                    )
                                }
                                if (isExpanded) {
                                    items(changesInGroup, key = { it.id + it.type.name }) { change ->
                                        SyncChangeItem(
                                            change = change,
                                            isChecked = (change.id + change.type.name) in approvedIds,
                                            onToggle = { viewModel.toggleApproval(change.id, change.type.name) },
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
private fun GroupHeader(
    changeType: ChangeType,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val metadata = ChangeTypeMetadata.get(changeType = changeType)
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = metadata.icon, contentDescription = metadata.title, tint = metadata.color)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${metadata.title} ($count)",
            style = MaterialTheme.typography.titleMedium,
            color = metadata.color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
            modifier = Modifier.rotate(rotationAngle),
        )
    }
}

@Composable
private fun SyncChangeItem(
    change: SyncChange,
    isChecked: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp)
                .clickable(onClick = onToggle),
        elevation = CardDefaults.cardElevation(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isChecked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.3f,
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = change.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (change.longDescription != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = change.longDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
