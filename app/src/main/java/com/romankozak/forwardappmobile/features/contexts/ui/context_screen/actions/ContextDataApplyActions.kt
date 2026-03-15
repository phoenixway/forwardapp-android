package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextCommand
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextData
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private typealias BacklogItemSetter =
    (List<com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent>) -> Unit

private fun fallbackInputMode(currentViewMode: ContextViewMode): InputMode =
    when (currentViewMode) {
        ContextViewMode.DIRECTION -> InputMode.AddDirection
        ContextViewMode.INBOX, ContextViewMode.ADVANCED -> InputMode.AddQuickRecord
        else -> InputMode.AddGoal
    }

private fun defaultInputMode(currentViewMode: ContextViewMode): InputMode =
    when (currentViewMode) {
        ContextViewMode.CONNECTIONS -> InputMode.AddConnectionNote
        ContextViewMode.DIRECTION -> InputMode.AddDirection
        ContextViewMode.INBOX, ContextViewMode.ADVANCED -> InputMode.AddQuickRecord
        else -> InputMode.AddGoal
    }

class ContextDataApplyActions(
    private val stateManager: ContextStateManager,
    private val contextSessionStore: ContextSessionStore,
    private val recentItemsRepository: RecentItemsRepository,
    private val scope: CoroutineScope,
) {
    private var lastSyncKey: Triple<String, String?, String>? = null

    private data class CapabilityState(
        val enableInbox: Boolean,
        val enableLog: Boolean,
        val enableArtifact: Boolean,
        val enableBacklog: Boolean,
        val enableDashboard: Boolean,
        val enableAttachments: Boolean,
        val isProjectManagementEnabled: Boolean,
    )

    fun applyLoaded(
        data: ContextData.Loaded,
        setListContent: BacklogItemSetter,
        setAttachmentItems: BacklogItemSetter,
    ) {
        setListContent(data.items)
        setAttachmentItems(data.attachmentItems)
        stateManager.updateContext(data)
        logProjectAccess(data)

        stateManager.updateState { currentState ->
            val session = syncSession(data, currentState)
            val capabilityState = session.toCapabilityState()
            val resolvedInputMode = resolveInputMode(currentState, session.currentView)
            currentState.updatedFrom(data, session.currentView, resolvedInputMode, capabilityState)
        }
    }

    fun applyEmpty(
        clearListContent: () -> Unit,
        clearAttachmentItems: () -> Unit,
    ) {
        clearListContent()
        clearAttachmentItems()
        stateManager.clear()
        stateManager.updateState { it.copy(isContextSwitching = false) }
    }

    private fun buildStableConfigFingerprint(
        config: com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration,
    ): String {
        val experimentalIds =
            config.experimentalCapabilityIds
                .map { it.raw.trim() }
                .filter { it.isNotEmpty() }
                .sorted()
                .joinToString(",")

        return listOf(
            config.contextId,
            config.basePresetCode.orEmpty(),
            config.applyMode,
            config.enableInbox.toString(),
            config.enableLog.toString(),
            config.enableArtifact.toString(),
            config.enableAdvanced.toString(),
            config.enableDashboard.toString(),
            config.enableBacklog.toString(),
            config.enableAttachments.toString(),
            config.enableAutoLinkSubprojects.toString(),
            experimentalIds,
        ).joinToString("|")
    }

    private fun logProjectAccess(data: ContextData.Loaded) {
        data.context?.let { project ->
            scope.launch {
                recentItemsRepository.logProjectAccess(project)
            }
        }
    }

    private fun syncSession(
        data: ContextData.Loaded,
        currentState: ContextUiState,
    ) = with(data) {
        val contextId = context?.id ?: currentState.context?.id.orEmpty()
        val preferredViewName = context?.defaultViewModeName
        val configFingerprint = buildStableConfigFingerprint(config)
        val syncKey = Triple(contextId, preferredViewName, configFingerprint)
        if (lastSyncKey == syncKey) {
            contextSessionStore.state.value
        } else {
            lastSyncKey = syncKey
            contextSessionStore.dispatch(
                ContextCommand.SyncFromConfig(
                    contextId = contextId,
                    config = config,
                    preferredViewName = preferredViewName,
                    currentView = currentState.currentViewMode,
                ),
            )
        }
    }

    private fun com.romankozak.forwardappmobile.core.context.ContextSessionState.toCapabilityState(): CapabilityState =
        CapabilityState(
            enableInbox = enabledCapabilities.contains(CapabilityId("inbox")),
            enableLog = enabledCapabilities.contains(CapabilityId("log")),
            enableArtifact = enabledCapabilities.contains(CapabilityId("advanced")),
            enableBacklog = enabledCapabilities.contains(CapabilityId("backlog")),
            enableDashboard = enabledCapabilities.contains(CapabilityId("dashboard")),
            enableAttachments = enabledCapabilities.contains(CapabilityId("connections")),
            isProjectManagementEnabled = enabledCapabilities.contains(CapabilityId("advanced")),
        )

    private fun resolveInputMode(
        currentState: ContextUiState,
        currentViewMode: ContextViewMode,
    ): InputMode =
        when {
            currentState.inputMode == InputMode.AddConnectionNote &&
                currentViewMode != ContextViewMode.CONNECTIONS -> {
                fallbackInputMode(currentViewMode)
            }
            currentState.inputMode != InputMode.AddGoal -> currentState.inputMode
            else -> defaultInputMode(currentViewMode)
        }

    private fun ContextUiState.updatedFrom(
        data: ContextData.Loaded,
        currentViewMode: ContextViewMode,
        inputMode: InputMode,
        capabilityState: CapabilityState,
    ): ContextUiState =
        if (
            matches(
                capabilityState = capabilityState,
                experimentalCapabilityIds = data.config.experimentalCapabilityIds,
                currentViewMode = currentViewMode,
                inputMode = inputMode,
            )
        ) {
            this
        } else {
            copy(
                enableInbox = capabilityState.enableInbox,
                enableLog = capabilityState.enableLog,
                enableArtifact = capabilityState.enableArtifact,
                enableBacklog = capabilityState.enableBacklog,
                enableDashboard = capabilityState.enableDashboard,
                enableAttachments = capabilityState.enableAttachments,
                isProjectManagementEnabled = capabilityState.isProjectManagementEnabled,
                experimentalCapabilityIds = data.config.experimentalCapabilityIds,
                currentViewMode = currentViewMode,
                inputMode = inputMode,
                isContextSwitching = false,
            )
        }

    private fun ContextUiState.matches(
        capabilityState: CapabilityState,
        experimentalCapabilityIds: List<CapabilityId>,
        currentViewMode: ContextViewMode,
        inputMode: InputMode,
    ): Boolean =
        enableInbox == capabilityState.enableInbox &&
            enableLog == capabilityState.enableLog &&
            enableArtifact == capabilityState.enableArtifact &&
            enableBacklog == capabilityState.enableBacklog &&
            enableDashboard == capabilityState.enableDashboard &&
            enableAttachments == capabilityState.enableAttachments &&
            isProjectManagementEnabled == capabilityState.isProjectManagementEnabled &&
            this.experimentalCapabilityIds == experimentalCapabilityIds &&
            this.currentViewMode == currentViewMode &&
            this.inputMode == inputMode &&
            !isContextSwitching
}
