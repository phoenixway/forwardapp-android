@file:Suppress("MatchingDeclarationName")

package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.scopelinks.DayScopeLinksActions
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.scopelinks.DayScopeLinksSheet
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.orderToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class DayPlanConnectionDeps(
    val viewModel: DayPlanViewModel,
    val scope: CoroutineScope,
    val context: Context,
)

private fun buildExternalTarget(
    linkType: LinkType?,
    target: String,
    vault: String? = null,
    globalObsidianVaultName: String? = null,
): String {
    val trimmed = target.trim()
    if (linkType == LinkType.OBSIDIAN && !trimmed.startsWith("obsidian://", ignoreCase = true)) {
        val vaultName = vault?.takeIf { it.isNotBlank() } ?: globalObsidianVaultName?.takeIf { it.isNotBlank() }
        val encodedFile = URLEncoder.encode(trimmed, "UTF-8")
        return if (vaultName != null) {
            "obsidian://open?vault=${URLEncoder.encode(vaultName, "UTF-8")}&file=$encodedFile"
        } else {
            "obsidian://open?file=$encodedFile"
        }
    }
    return trimmed
}

@Composable
fun DayPlanConnectionsHost(
    state: DayPlanContentState,
    dialogState: DayPlanDialogState,
    overlayState: DayPlanOverlayState,
    deps: DayPlanConnectionDeps,
) {
    DayPlanScopeLinksSheetHost(
        state = state,
        dialogState = dialogState,
        overlayState = overlayState,
        deps = deps,
    )
    DayPlanLinkPickerDialogHost(
        state = state,
        overlayState = overlayState,
        viewModel = deps.viewModel,
    )
}

@Composable
private fun DayPlanScopeLinksSheetHost(
    state: DayPlanContentState,
    dialogState: DayPlanDialogState,
    overlayState: DayPlanOverlayState,
    deps: DayPlanConnectionDeps,
) {
    val obsidianVaultName by deps.viewModel.obsidianVaultName.collectAsState()
    DayScopeLinksSheet(
        isVisible = dialogState.isScopeLinksSheetVisible,
        uiState = state.uiState,
        actions =
            DayScopeLinksActions(
                onDismiss = deps.viewModel::dismissScopeLinksSheet,
                onAddContextClick = {
                    openPickerAfterDismiss(
                        viewModel = deps.viewModel,
                        pendingCreateAction = overlayState.pendingCreateAction,
                        activeLinkPickerTab = overlayState.activeLinkPickerTab,
                        scope = deps.scope,
                        tab = LinkPickerTab.CONTEXTS,
                    )
                },
                onAddAttachmentClick = {
                    openPickerAfterDismiss(
                        viewModel = deps.viewModel,
                        pendingCreateAction = overlayState.pendingCreateAction,
                        activeLinkPickerTab = overlayState.activeLinkPickerTab,
                        scope = deps.scope,
                        tab = LinkPickerTab.ATTACHMENTS,
                    )
                },
                onAddExternalClick = { overlayState.showAddUrlDialog.value = true },
                onAddObsidianClick = { overlayState.showAddObsidianDialog.value = true },
                onCreateConnectionClick = { type ->
                    handleCreateConnectionClick(
                        type = type,
                        viewModel = deps.viewModel,
                        pendingCreateAction = overlayState.pendingCreateAction,
                        activeLinkPickerTab = overlayState.activeLinkPickerTab,
                        scope = deps.scope,
                    )
                },
                onContextClick = { contextId ->
                    state.navigator.navigationManager.navigateOrFallback(
                        navController = state.navigator.navController,
                        target = NavTarget.ContextDetail(contextId = contextId),
                        recordInHistory = true,
                    )
                },
                onAttachmentClick = { attachmentId ->
                    handleAttachmentClick(
                        attachmentId = attachmentId,
                        contentState = state,
                        context = deps.context,
                        globalObsidianVaultName = obsidianVaultName,
                    )
                },
                onContextRemove = deps.viewModel::removePlanProjectLink,
                onAttachmentRemove = deps.viewModel::removePlanAttachmentLink,
                onConnectionsReordered = { reordered ->
                    deps.viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
                },
            ),
        connectionOrder = dialogState.connectionsOrder,
    )
}

private fun handleCreateConnectionClick(
    type: CreateConnectionType,
    viewModel: DayPlanViewModel,
    pendingCreateAction: MutableState<PickerCreateAction?>,
    activeLinkPickerTab: MutableState<LinkPickerTab?>,
    scope: CoroutineScope,
) {
    viewModel.dismissScopeLinksSheet()
    pendingCreateAction.value = type.toPickerCreateAction()
    scope.launch {
        delay(LINK_PICKER_OPEN_DELAY_MILLIS)
        activeLinkPickerTab.value =
            if (type == CreateConnectionType.CONTEXT) {
                LinkPickerTab.CONTEXTS
            } else {
                LinkPickerTab.ATTACHMENTS
            }
    }
}

@Composable
private fun DayPlanLinkPickerDialogHost(
    state: DayPlanContentState,
    overlayState: DayPlanOverlayState,
    viewModel: DayPlanViewModel,
) {
    val availableProjectIds = state.uiState.availableProjects.map { it.id }.toSet()
    val availableAttachmentIds = state.uiState.availableAttachments.map { it.id }.toSet()

    overlayState.activeLinkPickerTab.value?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions =
                state.uiState.availableProjects.map { project ->
                    ProjectOption(id = project.id, name = project.name)
                },
            attachmentOptions =
                state.uiState.availableAttachments.map { attachment ->
                    AttachmentOption(
                        id = attachment.id,
                        name = attachment.name,
                        linkType = attachment.linkType,
                        attachmentType = attachment.attachmentType,
                        entityId = attachment.entityId,
                        target = attachment.target,
                        vault = attachment.vault,
                    )
                },
            preselectedContextIds =
                state.uiState.todayScopeLinkedProjectIds
                    .orEmpty()
                    .filter { it in availableProjectIds }
                    .toSet(),
            preselectedAttachmentIds =
                state.uiState.todayScopeLinkedAttachmentIds
                    .orEmpty()
                    .filter { it in availableAttachmentIds }
                    .toSet(),
            initialTab = initialTab,
            initialCreateAction = overlayState.pendingCreateAction.value,
            onDismiss = {
                overlayState.activeLinkPickerTab.value = null
                overlayState.pendingCreateAction.value = null
            },
            onContextSelected = { id ->
                viewModel.addPlanProjectLink(id)
                overlayState.activeLinkPickerTab.value = null
                overlayState.pendingCreateAction.value = null
            },
            onAttachmentSelected = { id ->
                viewModel.addPlanAttachmentLink(id)
                overlayState.activeLinkPickerTab.value = null
                overlayState.pendingCreateAction.value = null
            },
            onCreateRootContext = viewModel::createRootContextForPicker,
            onCreateDocument = viewModel::createPlanDocumentForPicker,
        )
    }
}

private fun openPickerAfterDismiss(
    viewModel: DayPlanViewModel,
    pendingCreateAction: MutableState<PickerCreateAction?>,
    activeLinkPickerTab: MutableState<LinkPickerTab?>,
    scope: CoroutineScope,
    tab: LinkPickerTab,
) {
    viewModel.dismissScopeLinksSheet()
    pendingCreateAction.value = null
    scope.launch {
        delay(LINK_PICKER_OPEN_DELAY_MILLIS)
        activeLinkPickerTab.value = tab
    }
}

private fun handleAttachmentClick(
    attachmentId: String,
    contentState: DayPlanContentState,
    context: Context,
    globalObsidianVaultName: String,
) {
    val option = contentState.uiState.availableAttachments.firstOrNull { it.id == attachmentId }
    when {
        option?.attachmentType == "NOTE_DOCUMENT" && !option.entityId.isNullOrBlank() ->
            contentState.navigator.navigationManager.navigateOrFallback(
                navController = contentState.navigator.navController,
                target = NavTarget.NoteDocument(id = option.entityId),
            )
        option?.attachmentType == "JOURNAL_DOCUMENT" && !option.entityId.isNullOrBlank() ->
            contentState.navigator.navigationManager.navigateOrFallback(
                navController = contentState.navigator.navController,
                target = NavTarget.JournalDocument(id = option.entityId),
            )

        option?.attachmentType == "MUSIC_NOTE" && !option.entityId.isNullOrBlank() ->
            contentState.navigator.navigationManager.navigateOrFallback(
                navController = contentState.navigator.navController,
                target = NavTarget.MusicNote(id = option.entityId),
            )

        option?.attachmentType == "CHECKLIST" && !option.entityId.isNullOrBlank() ->
            contentState.navigator.navigationManager.navigateOrFallback(
                navController = contentState.navigator.navController,
                target = NavTarget.Checklist(id = option.entityId),
            )

        option?.linkType == LinkType.CONTEXT && !option.target.isNullOrBlank() ->
            contentState.navigator.navigationManager.navigateOrFallback(
                navController = contentState.navigator.navController,
                target = NavTarget.ContextDetail(contextId = option.target),
                recordInHistory = true,
            )

        (option?.linkType == LinkType.URL || option?.linkType == LinkType.OBSIDIAN) &&
            !option.target.isNullOrBlank() -> {
            val resolvedTarget =
                buildExternalTarget(
                    option.linkType,
                    option.target,
                    option.vault,
                    globalObsidianVaultName,
                )
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(resolvedTarget)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }.onFailure {
                Log.e(TAG, "Cannot open link: ${option.target}", it)
                openAttachmentsLibrary(contentState = contentState, attachmentId = attachmentId)
            }
        }

        else -> openAttachmentsLibrary(contentState = contentState, attachmentId = attachmentId)
    }
}

private fun openAttachmentsLibrary(
    contentState: DayPlanContentState,
    attachmentId: String,
) {
    contentState.navigator.navigationManager.navigateOrFallback(
        navController = contentState.navigator.navController,
        target = NavTarget.AttachmentsLibrary,
    ) {
        launchSingleTop = true
        restoreState = true
    }
    runCatching {
        contentState.navigator.navController.getBackStackEntry("attachments_library_screen")
            .savedStateHandle["attachment_library_query"] = attachmentId
    }
}
