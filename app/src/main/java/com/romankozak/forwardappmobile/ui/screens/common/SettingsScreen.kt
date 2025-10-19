package com.romankozak.forwardappmobile.ui.screens.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    title: String,
    navController: NavController,
    tabs: List<String>,
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
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) },
                        text = { Text(tabTitle) }
                    )
                }
            }
            content(selectedTabIndex)
        }
    }
}
