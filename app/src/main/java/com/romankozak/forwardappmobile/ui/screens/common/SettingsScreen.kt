package com.romankozak.forwardappmobile.ui.screens.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.AdaptiveSegmentedControl
import com.romankozak.forwardappmobile.ui.components.SegmentedTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    title: String,
    navController: NavController,
    tabs: List<SegmentedTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    content: @Composable (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsTopAppBar(
                title = title,
                navController = navController,
                onSave = onSave,
                isSaveEnabled = isSaveEnabled
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .imePadding()
        ) {
            AdaptiveSegmentedControl(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content(selectedTabIndex)
        }
    }
}
