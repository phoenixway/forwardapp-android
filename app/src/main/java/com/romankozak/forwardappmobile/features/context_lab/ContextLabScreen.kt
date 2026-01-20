package com.romankozak.forwardappmobile.features.context_lab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.* // Використовуємо Material 3
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // КРИТИЧНО для роботи 'by'
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextLabScreen(viewModel: ContextLabViewModel) {
    // collectAsState потребує імпорту getValue для делегування 'by'
    val contexts by viewModel.uiState.collectAsState()
    val activeId by viewModel.activeContextId.collectAsState()
    val allCaps = viewModel.allCapabilities

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лабораторія Контекстів") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Експериментальне керування поліморфними контекстами",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(contexts) { context ->
                ContextItemCard(
                    context = context,
                    isActive = context.id == activeId,
                    allCapabilities = allCaps,
                    onToggle = { capId -> viewModel.onToggleCapability(context.id, capId) },
                    onActivate = { viewModel.onActivateContext(context.id) }
                )
            }
        }
    }
}

@Composable
fun ContextItemCard(
    context: com.romankozak.forwardappmobile.core.context.Context,
    isActive: Boolean,
    allCapabilities: Set<CapabilityDescriptor>,
    onToggle: (CapabilityId) -> Unit,
    onActivate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = context.id.raw, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Роль: ${context.role.label}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color.Green
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "Можливості (Capabilities):", style = MaterialTheme.typography.titleSmall)

            allCapabilities.forEach { cap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = context.config.activeCapabilities.contains(cap.id),
                        onCheckedChange = { onToggle(cap.id) }
                    )
                    Text(text = cap.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive
            ) {
                Text(if (isActive) "АКТИВНИЙ" else "АКТИВУВАТИ КОНТЕКСТ")
            }
        }
    }
}