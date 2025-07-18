@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.romankozak.forwardappmobile

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun GoalEditScreen(
    navController: NavController,
    // --- ВИПРАВЛЕНО: ViewModel створюється за допомогою Hilt ---
    viewModel: GoalEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listHierarchy by viewModel.listHierarchy.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GoalEditEvent.NavigateBack -> {
                    event.message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewGoal) "Нова ціль" else "Редагувати ціль") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onSave() },
                        enabled = uiState.isReady && uiState.goalText.isNotBlank()
                    ) {
                        Text("Зберегти")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!uiState.isReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                item {
                    OutlinedTextField(
                        value = uiState.goalText,
                        onValueChange = viewModel::onTextChange,
                        label = { Text("Назва цілі") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.goalDescription,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Опис (необов'язково)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    )
                }

                item {
                    Text("Пов'язані списки:", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.associatedLists.forEach { list ->
                            InputChip(
                                selected = false,
                                onClick = { /* Do nothing */ },
                                label = { Text(list.name) },
                                trailingIcon = {
                                    if (uiState.associatedLists.size > 1) {
                                        Icon(
                                            Icons.Default.Cancel,
                                            contentDescription = "Видалити зі списку",
                                            modifier = Modifier
                                                .size(InputChipDefaults.IconSize)
                                                .clickable { viewModel.onRemoveListAssociation(list.id) }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.onShowListChooser() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Додати пов'язаний список")
                    }
                }
            }
        }
    }

    if (uiState.showListChooser) {
        ListChooserDialog(
            topLevelLists = listHierarchy.topLevelLists,
            childMap = listHierarchy.childMap,
            onDismiss = { viewModel.onDismissListChooser() },
            onConfirm = { listId -> viewModel.onAddListAssociation(listId) }
        )
    }
}