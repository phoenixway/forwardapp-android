package com.romankozak.forwardappmobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.goallist.PlanningSettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    planningSettings: PlanningSettingsState,
    initialVaultName: String,
    reservedContextCount: Int,
    onManageContextsClick: () -> Unit,
    onBack: () -> Unit,
    onSave: (
        showModes: Boolean, dailyTag: String, mediumTag: String, longTag: String,
        vaultName: String
    ) -> Unit
) {
    var tempShowModes by remember(planningSettings.showModes) { mutableStateOf(planningSettings.showModes) }
    var tempDailyTag by remember(planningSettings.dailyTag) { mutableStateOf(planningSettings.dailyTag) }
    var tempMediumTag by remember(planningSettings.mediumTag) { mutableStateOf(planningSettings.mediumTag) }
    var tempLongTag by remember(planningSettings.longTag) { mutableStateOf(planningSettings.longTag) }
    var tempVaultName by remember(initialVaultName) { mutableStateOf(initialVaultName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    // ✨ ОНОВЛЕНО: Замінено TextButton на IconButton зі стрілкою
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            tempShowModes,
                            tempDailyTag,
                            tempMediumTag,
                            tempLongTag,
                            tempVaultName
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- Planning Modes Card ---
            SettingsCard(title = "Planning Modes") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Show planning scale modes", modifier = Modifier.weight(1f))
                    Switch(checked = tempShowModes, onCheckedChange = { tempShowModes = it })
                }
                AnimatedTextField(
                    value = tempDailyTag,
                    onValueChange = { tempDailyTag = it },
                    label = "Daily Mode Tag",
                    helper = "Tag used for daily planning mode"
                )
                AnimatedTextField(
                    value = tempMediumTag,
                    onValueChange = { tempMediumTag = it },
                    label = "Medium Mode Tag",
                    helper = "Tag used for medium planning mode"
                )
                AnimatedTextField(
                    value = tempLongTag,
                    onValueChange = { tempLongTag = it },
                    label = "Long Mode Tag",
                    helper = "Tag used for long planning mode"
                )
            }

            // --- Integrations Card ---
            SettingsCard(title = "Integrations") {
                Text("Specify the exact name of your Obsidian Vault for link integration.")
                AnimatedTextField(
                    value = tempVaultName,
                    onValueChange = { tempVaultName = it },
                    label = "Obsidian Vault Name",
                    helper = "Exact vault name for link integration",
                    singleLine = true
                )
            }

            // --- Contexts Card ---
            SettingsCard(title = "Contexts") {
                OutlinedButton(
                    onClick = onManageContextsClick,
                    modifier = Modifier.fillMaxWidth(),
                    content = { Text("Manage Reserved Contexts ($reservedContextCount)") },
                    colors = ButtonDefaults.outlinedButtonColors()
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String,
    singleLine: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else Color.Transparent, label = ""
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(helper) },
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
    )
}
