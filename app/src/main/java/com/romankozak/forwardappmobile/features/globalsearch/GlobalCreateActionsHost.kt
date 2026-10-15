package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semanticcreation.SemanticEntityCreationDialog
import com.romankozak.forwardappmobile.features.semanticcreation.SemanticEntityCreationEvent
import com.romankozak.forwardappmobile.features.semanticcreation.SemanticEntityCreationViewModel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.connectionspanel.ConnectionsCreateActionsDialog
import kotlinx.coroutines.launch

@Composable
fun GlobalCreateActionsHost(
    showCreateSheet: Boolean,
    onDismissCreateSheet: () -> Unit,
    onCreateContext: (String) -> Unit,
    onCreateDocument: suspend (NewDocumentDraft) -> String?,
    onCreateCanonicalNote: () -> Unit,
    onSemanticEntityCreated: (String) -> Unit,
    onCreateReminder: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    semanticEntityCreationViewModel: SemanticEntityCreationViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    var newContextName by remember { mutableStateOf("") }
    var showCreateContextDialog by remember { mutableStateOf(false) }
    var showDocumentCreateActionsDialog by remember { mutableStateOf(false) }
    var pendingDocumentCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    var showCreateReminderDialog by remember { mutableStateOf(false) }
    var showSemanticEntityDialog by remember { mutableStateOf(false) }
    val semanticEntityState by semanticEntityCreationViewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(semanticEntityCreationViewModel) {
        semanticEntityCreationViewModel.events.collect { event ->
            when (event) {
                is SemanticEntityCreationEvent.Created -> {
                    showSemanticEntityDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message =
                                when (event.entityClass) {
                                    SemanticEntityClass.ORIENTATION -> "Орієнтир створено"
                                    SemanticEntityClass.ASPECT -> "Аспект створено"
                                },
                            duration = SnackbarDuration.Short,
                        )
                    }
                    onSemanticEntityCreated(event.id)
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateFromSearchBottomSheet(
            onCreateContext = {
                onDismissCreateSheet()
                newContextName = ""
                showCreateContextDialog = true
            },
            onCreateDocument = {
                onDismissCreateSheet()
                showDocumentCreateActionsDialog = true
            },
            onCreateReminder = {
                onDismissCreateSheet()
                showCreateReminderDialog = true
            },
            onCreateSemanticEntity = {
                onDismissCreateSheet()
                semanticEntityCreationViewModel.reset()
                showSemanticEntityDialog = true
            },
            onDismiss = onDismissCreateSheet,
        )
    }
    if (showDocumentCreateActionsDialog) {
        ConnectionsCreateActionsDialog(
            onDismiss = { showDocumentCreateActionsDialog = false },
            onActionSelected = { type ->
                showDocumentCreateActionsDialog = false
                when (type) {
                    CreateConnectionType.NOTE_DOCUMENT -> onCreateCanonicalNote()
                    CreateConnectionType.CONTEXT -> {
                        newContextName = ""
                        showCreateContextDialog = true
                    }
                    CreateConnectionType.SCRIPT -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Скрипт створюється з ConnectionsPanel",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    else -> pendingDocumentCreateAction = type.toGlobalCreatePickerAction()
                }
            },
        )
    }
    pendingDocumentCreateAction?.let { action ->
        LinkedTargetsPickerDialog(
            contextOptions = emptyList(),
            attachmentOptions = emptyList(),
            preselectedContextIds = emptySet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.ATTACHMENTS,
            allowedTabs = setOf(LinkPickerTab.ATTACHMENTS),
            initialCreateAction = action,
            onDismiss = { pendingDocumentCreateAction = null },
            onContextSelected = {},
            onAttachmentSelected = {
                pendingDocumentCreateAction = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Документ створено",
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            onCreateDocument = onCreateDocument,
        )
    }
    if (showCreateReminderDialog) {
        ReminderPropertiesDialog(
            onDismiss = { showCreateReminderDialog = false },
            onSetReminder = { timestamp ->
                onCreateReminder(timestamp)
                showCreateReminderDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Нагадування додано",
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            currentReminders = emptyList(),
        )
    }
    if (showCreateContextDialog) {
        AlertDialog(
            onDismissRequest = { showCreateContextDialog = false },
            title = { Text("Новий контекст") },
            text = {
                OutlinedTextField(
                    value = newContextName,
                    onValueChange = { newContextName = it },
                    label = { Text("Назва") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newContextName.isNotBlank(),
                    onClick = {
                        val contextName = newContextName
                        showCreateContextDialog = false
                        newContextName = ""
                        onCreateContext(contextName)
                    },
                ) {
                    Text("Створити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateContextDialog = false }) {
                    Text("Скасувати")
                }
            },
        )
    }
    if (showSemanticEntityDialog) {
        SemanticEntityCreationDialog(
            state = semanticEntityState,
            onNameChange = semanticEntityCreationViewModel::setName,
            onEntityClassChange = semanticEntityCreationViewModel::setEntityClass,
            onOrientationKindChange = semanticEntityCreationViewModel::setOrientationKind,
            onCreate = semanticEntityCreationViewModel::create,
            onDismiss = {
                semanticEntityCreationViewModel.reset()
                showSemanticEntityDialog = false
            },
        )
    }
}

private fun CreateConnectionType.toGlobalCreatePickerAction(): PickerCreateAction =
    when (this) {
        CreateConnectionType.NOTE_DOCUMENT -> PickerCreateAction.NOTE
        CreateConnectionType.MUSIC_NOTE -> PickerCreateAction.MUSIC_NOTE
        CreateConnectionType.CHECKLIST -> PickerCreateAction.CHECKLIST
        CreateConnectionType.EXTERNAL_LINK -> PickerCreateAction.WEB_LINK
        CreateConnectionType.OBSIDIAN_NOTE -> PickerCreateAction.OBSIDIAN
        CreateConnectionType.CONTEXT -> PickerCreateAction.CONTEXT
        CreateConnectionType.SCRIPT -> PickerCreateAction.NOTE
    }
