package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.ui.common.components.FullScreenTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomListScreen(viewModel: CustomListViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    FullScreenTextEditor(
        title = uiState.list?.name ?: "Custom List",
        initialText = uiState.list?.content ?: "",
        onSave = {
            viewModel.onSaveContent(it)
            onNavigateBack()
        },
        onCancel = onNavigateBack
    )
}