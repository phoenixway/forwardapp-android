package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId

/** A natural details destination for one exact planning participation, including historical scopes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningContextScreen(commitmentId: String, onBack: () -> Unit) {
    val model: PlanningCompositionViewModel = hiltViewModel()
    val frame by model.projection.collectAsStateWithLifecycle()
    val current = frame.participants.singleOrNull { it.commitment.id.value == commitmentId }
    Scaffold(topBar = { TopAppBar(title = { Text("Planning context") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
    }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current == null) {
                Text("Участь недоступна або завантажується", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(current.entity.title, style = MaterialTheme.typography.headlineSmall)
                Text(current.contextLabel, style = MaterialTheme.typography.bodySmall)
                if (current.statusLabel.isNotBlank()) Text(current.statusLabel, style = MaterialTheme.typography.labelMedium)
                PlanningParticipationActions(current, model)
                if (frame.outgoing(PlanningCommitmentId(commitmentId)).isEmpty()) {
                    Text("No larger planning context", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (frame.incoming(current.commitment.id).isEmpty()) {
                    Text("No components yet", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PlanningCompositionContent(current, frame, model)
            }
        }
    }
}
