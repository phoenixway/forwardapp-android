package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrientationPlanningScreen(orientationId: String, onBack: () -> Unit, model: PlanningCompositionViewModel = hiltViewModel()) {
    val frame by model.projection.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val entity = frame.orientations.singleOrNull { it.ref == CanonicalEntityRef.Orientation(orientationId) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable(orientationId) { mutableStateOf<String?>(null) }
    val participants = frame.forOrientation(orientationId)
    Scaffold(topBar = { TopAppBar(title = { Text("Planning") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entity == null) item { Text("Orientation недоступний або завантажується") }
            else {
                item {
                    Text(entity.title, style = MaterialTheme.typography.headlineSmall)
                    Text(entity.orientationKind?.name.orEmpty(), style = MaterialTheme.typography.labelMedium)
                }
                if (participants.isEmpty()) item { Text("Not currently planned", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                listOf(PlanningScopeKind.STRATEGIC_ARC, PlanningScopeKind.TACTICAL_CYCLE, PlanningScopeKind.DAY).forEach { horizon ->
                    val items = participants.filter { it.scope.kind == horizon }
                    if (items.isNotEmpty()) {
                        item { Text(horizon.horizonLabel(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                        items(items, key = { it.commitment.id.value }) { participant ->
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    PlanningCompositionRow(participant, { model.open(participant) })
                                    TextButton(onClick = {
                                        expanded = participant.commitment.id.value.takeUnless { it == expanded }
                                    }) { Text(if (expanded == participant.commitment.id.value) "Hide planning details" else "Part of / Components") }
                                    if (expanded == participant.commitment.id.value) {
                                        PlanningParticipationActions(participant, model)
                                        PlanningCompositionContent(participant, frame, model)
                                    }
                                }
                            }
                        }
                    }
                }
                item { TextButton(enabled = !busy, onClick = { adding = true }) { Text("Add to planning") } }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
        }
    }
    if (adding && entity != null) AddOrientationToPlanningDialog({ adding = false }) { horizon ->
        adding = false
        model.plan(orientationId, horizon)
    }
}

@Composable
internal fun AddOrientationToPlanningDialog(onDismiss: () -> Unit, onChoose: (PlanningScopeKind) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to planning") }, text = {
        Column {
            TextButton(onClick = { onChoose(PlanningScopeKind.STRATEGIC_ARC) }) { Text("Current Strategic Arc · Focus") }
            TextButton(onClick = { onChoose(PlanningScopeKind.TACTICAL_CYCLE) }) { Text("Current Tactical Cycle · Mission") }
            TextButton(onClick = { onChoose(PlanningScopeKind.DAY) }) { Text("Today · Task") }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } })
}
