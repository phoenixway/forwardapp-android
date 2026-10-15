package com.romankozak.forwardappmobile.features.entitypicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserContent
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanonicalEntityPickerScreen(
    onBack: () -> Unit,
    onResult: (Set<CanonicalEntityRef>) -> Unit,
    viewModel: CanonicalEntityPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is CanonicalEntityPickerEvent.Complete) onResult(event.selected)
        }
    }
    val ready = state as? CanonicalEntityPickerUiState.Ready
    val title =
        ready?.request?.allowedClasses?.singleOrNull()?.let { "Оберіть: ${it.displayLabel()}" }
            ?: "Оберіть сутності"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            CanonicalEntityPickerUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is CanonicalEntityPickerUiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(current.message)
                }
            is CanonicalEntityPickerUiState.Ready ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CanonicalEntityBrowserContent(
                        items = current.items,
                        searchText = current.searchText,
                        availableClasses = current.request.allowedClasses,
                        allowedClasses = current.visibleClasses,
                        selectedRefs = current.selectedRefs,
                        multiSelect = current.request.selectionMode == CanonicalEntitySelectionMode.MULTI,
                        onSearchChange = viewModel::setSearch,
                        onToggleClass = viewModel::toggleClass,
                        onSelect = viewModel::select,
                        modifier = Modifier.weight(1f),
                    )
                    if (current.request.selectionMode == CanonicalEntitySelectionMode.MULTI) {
                        Button(
                            onClick = viewModel::confirm,
                            modifier = Modifier.align(Alignment.End).padding(16.dp),
                        ) {
                            Text("Готово (${current.selectedRefs.size})")
                        }
                    }
                }
        }
    }
}
