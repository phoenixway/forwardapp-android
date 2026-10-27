package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
    placements: EntityLibraryPlacementViewModel = hiltViewModel(),
    savedQueries: SavedEntityQueriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val placement by placements.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var searchResetKey by rememberSaveable { mutableStateOf(0) }
    var resetSearchText by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(placement.message) {
        placement.message?.let { snackbar.showSnackbar(it); placements.clearMessage() }
    }
    if (placement.pendingRef != null) EntityLibraryPlacementDialog(
        mutation = placement, onDismiss = placements::cancel, onChoose = placements::place,
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
                    queryActions = EntityLibraryQueryActions(viewModel::applyFilters, viewModel::setUnplaced,
                        viewModel::setSort, viewModel::setScoreSort, viewModel::clearFilters),
                    savedQueries = savedQueries,
                    currentQuery = viewModel::currentQuery,
                    onApplyQuery = { query ->
                        resetSearchText = query.text
                        searchResetKey++
                        viewModel.applyQuery(query)
                    },
                    searchResetKey = searchResetKey,
                    resetSearchText = resetSearchText,
                    onAddToBigPicture = placements::begin,
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
    queryActions: EntityLibraryQueryActions,
    savedQueries: SavedEntityQueriesViewModel,
    currentQuery: () -> com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQuery,
    onApplyQuery: (com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQuery) -> Unit,
    searchResetKey: Int,
    resetSearchText: String?,
    onSelect: (CanonicalEntityRef) -> Unit,
    onAddToBigPicture: (CanonicalEntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CanonicalEntityBrowserSearch(
            initialSearchText = state.searchText,
            onSearchChange = onSearchChange,
            searchResetKey = searchResetKey,
            resetSearchText = resetSearchText ?: state.searchText,
        )
        EntityLibraryQueryControls(state, queryActions)
        SavedEntityQueriesControl(currentQuery, savedQueries, onApplyQuery)
        CanonicalEntityBrowserList(
            items = state.items,
            emptyMessage = if (state.isCatalogEmpty) "Бібліотека порожня" else "Нічого не знайдено",
            onSelect = onSelect,
            rowActions = { item -> EntityLibraryItemMenu(item.ref, onAddToBigPicture) },
            modifier = Modifier.weight(1f),
        )
    }
}
