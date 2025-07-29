package com.romankozak.forwardappmobile.ui.screens.globalsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavController,
    // --- ВИПРАВЛЕНО: ViewModel створюється за допомогою Hilt ---
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query = viewModel.query

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Результати для: \"$query\"",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.results.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Нічого не знайдено.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.results, key = { it.goal.id + it.listId }) { result ->
                    SearchResultItem(
                        result = result,
                        onClick = {
                            // Навігуємо на екран деталей, передаючи ID списку та ID цілі для виділення
                            navController.navigate("goal_detail_screen/${result.listId}?goalId=${result.goal.id}") {
                                // Уникаємо накопичення екранів пошуку в стеку
                                popUpTo("goal_lists_screen")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: GlobalSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = result.goal.text,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "в списку: ${result.listName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}