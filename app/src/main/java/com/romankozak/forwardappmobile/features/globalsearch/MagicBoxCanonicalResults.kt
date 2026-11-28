package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

/** Thin shell adapter: typed rows and routes; all predicates remain in the shared engine. */
@Composable
fun MagicBoxCanonicalResults(
    input: String,
    navController: NavController,
    onLegacyResults: () -> Unit,
    attachmentsEnabled: Boolean = true,
    viewModel: MagicBoxCanonicalQueryViewModel = hiltViewModel(),
    attachmentsViewModel: MagicBoxAttachmentViewModel = hiltViewModel(),
    globalSearch: GlobalSearchViewModel = hiltViewModel(),
) {
    val attachmentsAvailable = attachmentsEnabled && !hasCanonicalQueryOperators(input)
    LaunchedEffect(input, attachmentsAvailable) {
        viewModel.setInput(input)
        attachmentsViewModel.setInput(if (attachmentsAvailable) input else "")
    }
    val state by viewModel.state.collectAsState()
    val attachmentState by attachmentsViewModel.state.collectAsState()
    MagicBoxCanonicalResultContent(
        input = input,
        state = state,
        attachmentState = attachmentState,
        attachmentsAvailable = attachmentsAvailable,
        onLegacyResults = onLegacyResults,
        onSelectSaved = viewModel::selectSaved,
        onEntity = { navController.navigate(NavTargetRouter.routeOf(magicBoxCanonicalDestination(it))) },
        onAttachment = globalSearch::openAttachmentResult,
    )
}

@Composable
internal fun MagicBoxCanonicalResultContent(
    input: String,
    state: MagicBoxCanonicalState,
    attachmentState: MagicBoxAttachmentState,
    attachmentsAvailable: Boolean,
    onLegacyResults: () -> Unit,
    onSelectSaved: (String) -> Unit,
    onEntity: (CanonicalEntityRef) -> Unit,
    onAttachment: (GlobalSearchResultItem.AttachmentItem) -> Unit,
) {
    var selectedType by rememberSaveable { mutableStateOf(MagicBoxResultType.All) }
    var orientationKind by rememberSaveable { mutableStateOf<OrientationKind?>(null) }
    val type = selectedType.takeUnless { it == MagicBoxResultType.Attachments && !attachmentsAvailable }
        ?: MagicBoxResultType.All
    val attachmentsOnly = type == MagicBoxResultType.Attachments
    val showAttachments = attachmentsAvailable && (type == MagicBoxResultType.All || attachmentsOnly)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Результати", modifier = Modifier.padding(12.dp))
            if (!hasCanonicalQueryOperators(input)) TextButton(onClick = onLegacyResults) { Text("Інші результати") }
        }
        MagicBoxResultFilters(
            type = type,
            onTypeChange = { selectedType = it },
            orientationKind = orientationKind,
            onOrientationKindChange = { orientationKind = it },
            attachmentsAvailable = attachmentsAvailable,
        )
        if (!attachmentsOnly && state.input != input) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (!attachmentsOnly && state.resolution.issue != null) {
            state.resolution.messages.forEach { Text(it, Modifier.padding(12.dp)) }
            LazyColumn {
                items(state.resolution.choices, key = { it.id }) { item ->
                    TextButton(onClick = { onSelectSaved(item.id) }) { Text("${item.name} · ${item.id.takeLast(8)}") }
                }
            }
        } else {
            val entities = remember(state.results, type, orientationKind) {
                state.results.filter { type.matches(it, orientationKind) }
            }
            key(type, orientationKind) {
                MagicBoxCombinedResultList(
                    entities = entities,
                    attachments = attachmentState.results.takeIf { attachmentState.input == input }.orEmpty(),
                    onEntity = onEntity,
                    onAttachment = onAttachment,
                    showAttachments = showAttachments,
                    attachmentsLoading = showAttachments && attachmentState.input != input,
                    attachmentError = attachmentState.error.takeIf { attachmentState.input == input },
                )
            }
        }
    }
}
