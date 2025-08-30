package com.romankozak.forwardappmobile.ui.screens.editlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.GoalList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListScreen(
    navController: NavController,
    listId: String,
    viewModel: EditListViewModel = hiltViewModel()
) {
    val list by viewModel.list.collectAsState()
    var name by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }
    var currentTagInput by remember { mutableStateOf("") }

    LaunchedEffect(list) {
        list?.let {
            name = it.name
            tags = it.tags?.toMutableList() ?: mutableListOf()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Edit List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.onSave(name, tags)
                            navController.popBackStack()
                        },
                        enabled = list != null
                    ) {
                        Text("Зберегти")
                    }
                }
            )
        }
    ) { paddingValues ->
        list?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Назва списку") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Додати тег", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = currentTagInput,
                            onValueChange = { currentTagInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (currentTagInput.isNotBlank()) {
                                        tags = tags + currentTagInput.trim()
                                        currentTagInput = ""
                                    }
                                }
                            ),
                            label = { Text("Назва тегу") }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (currentTagInput.isNotBlank()) {
                                    tags = tags + currentTagInput.trim()
                                    currentTagInput = ""
                                }
                            },
                            enabled = currentTagInput.isNotBlank()
                        ) {
                            Text("Додати")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (tags.isNotEmpty()) {
                    Text("Поточні теги", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        mainAxisSpacing = 4.dp,
                        crossAxisSpacing = 4.dp
                    ) {
                        tags.forEach { tag ->
                            TagChip(
                                text = tag,
                                onDismiss = { tags = tags - tag }
                            )
                        }
                    }
                } else {
                    Text("Теги ще не додані.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TagChip(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = "Видалити тег",
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onDismiss)
        )
    }
}