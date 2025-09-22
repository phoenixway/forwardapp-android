package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CustomListScreen(
    viewModel: CustomListViewModel = hiltViewModel()
) {
    Text("Custom List Screen")
}
