package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityBrowserList

/** Thin shell adapter: typed rows and routes; all predicates remain in the shared engine. */
@Composable
fun MagicBoxCanonicalResults(
    input: String,
    navController: NavController,
    onLegacyResults: () -> Unit,
    viewModel: MagicBoxCanonicalQueryViewModel = hiltViewModel(),
) {
    LaunchedEffect(input) { viewModel.setInput(input) }
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сутності · ${if (state.input == input) state.results.size else "…"}", modifier = Modifier.padding(12.dp))
            if (!hasCanonicalQueryOperators(input)) TextButton(onClick = onLegacyResults) { Text("Інші результати") }
        }
        if (state.input != input) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (state.resolution.issue != null) {
            state.resolution.messages.forEach { Text(it, Modifier.padding(12.dp)) }
            LazyColumn {
                items(state.resolution.choices, key = { it.id }) { item ->
                    TextButton(onClick = {
                        viewModel.selectSaved(item.id)
                    }) { Text("${item.name} · ${item.id.takeLast(8)}") }
                }
            }
        } else {
            CanonicalEntityBrowserList(items = state.results, emptyMessage = "Нічого не знайдено",
                onSelect = { navController.navigate(NavTargetRouter.routeOf(magicBoxCanonicalDestination(it))) })
        }
    }
}
