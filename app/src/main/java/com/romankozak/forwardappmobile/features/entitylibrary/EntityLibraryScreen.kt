package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserList
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserSearch
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityLibraryScreen(
    onBack: () -> Unit,
    onOpenSemanticEditor: (String) -> Unit,
    onOpenWorkspace: (String) -> Unit,
    viewModel: EntityLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            EntityLibraryUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is EntityLibraryUiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(current.message)
                }

            is EntityLibraryUiState.Ready ->
                EntityLibraryReadyContent(
                    state = current,
                    onSearchChange = viewModel::setSearchText,
                    onFilterChange = viewModel::setFilter,
                    onSortChange = viewModel::setSort,
                    onSelect = { ref ->
                        when (val destination = entityLibraryDestination(ref)) {
                            is EntityLibraryDestination.SemanticEditor ->
                                onOpenSemanticEditor(destination.subjectId)
                            is EntityLibraryDestination.Workspace ->
                                onOpenWorkspace(destination.workspaceId)
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }
}

@Composable
private fun EntityLibraryReadyContent(
    state: EntityLibraryUiState.Ready,
    onSearchChange: (String) -> Unit,
    onFilterChange: (EntityLibraryFilter) -> Unit,
    onSortChange: (EntityLibrarySort) -> Unit,
    onSelect: (CanonicalEntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CanonicalEntityBrowserSearch(
            initialSearchText = state.searchText,
            onSearchChange = onSearchChange,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EntityLibraryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == state.filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.displayLabel()) },
                )
            }
        }
        Box(Modifier.padding(horizontal = 12.dp)) {
            var sortMenuExpanded by remember { mutableStateOf(false) }
            TextButton(onClick = { sortMenuExpanded = true }) { Text(state.sort.label) }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                EntityLibrarySort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label) },
                        onClick = { onSortChange(sort); sortMenuExpanded = false },
                    )
                }
            }
        }
        CanonicalEntityBrowserList(
            items = state.items,
            emptyMessage = if (state.isCatalogEmpty) "Бібліотека порожня" else "Нічого не знайдено",
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun EntityLibraryFilter.displayLabel(): String =
    when (this) {
        EntityLibraryFilter.ALL -> "Усі"
        EntityLibraryFilter.ORIENTATIONS -> "Орієнтири"
        EntityLibraryFilter.ASPECTS -> "Аспекти"
        EntityLibraryFilter.WORKSPACES -> "Робочі простори"
        EntityLibraryFilter.UNPLACED -> "Не розміщені"
    }
