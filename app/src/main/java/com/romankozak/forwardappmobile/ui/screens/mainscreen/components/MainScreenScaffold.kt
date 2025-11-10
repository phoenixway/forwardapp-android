package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold() {
    Scaffold { paddingValues ->
        MainScreenContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}
