package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextCommand
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextData
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ContextDataApplyActions(
    private val stateManager: ContextStateManager,
    private val contextSessionStore: ContextSessionStore,
    private val recentItemsRepository: RecentItemsRepository,
    private val scope: CoroutineScope,
) {
    private var lastSyncKey: Triple<String, String?, String>? = null

    fun applyLoaded(
        data: ContextData.Loaded,
        setListContent: (List<com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent>) -> Unit,
        setAttachmentItems: (List<com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent>) -> Unit,
    ) {
        setListContent(data.items)
        setAttachmentItems(data.attachmentItems)
        stateManager.updateContext(data)

        data.context?.let { project ->
            scope.launch {
                recentItemsRepository.logProjectAccess(project)
            }
        }

        stateManager.updateState { currentState ->
            val contextId = data.context?.id ?: currentState.context?.id.orEmpty()
            val preferredViewName = data.context?.defaultViewModeName
            val configFingerprint = buildStableConfigFingerprint(data.config)
            val syncKey = Triple(contextId, preferredViewName, configFingerprint)
            val session =
                if (lastSyncKey == syncKey) {
                    contextSessionStore.state.value
                } else {
                    lastSyncKey = syncKey
                    contextSessionStore.dispatch(
                        ContextCommand.SyncFromConfig(
                            contextId = contextId,
                            config = data.config,
                            preferredViewName = preferredViewName,
                            currentView = currentState.currentViewMode,
                        ),
                    )
                }

            val enableInbox = session.enabledCapabilities.contains(CapabilityId("inbox"))
            val enableLog = session.enabledCapabilities.contains(CapabilityId("log"))
            val enableArtifact = session.enabledCapabilities.contains(CapabilityId("advanced"))
            val enableBacklog = session.enabledCapabilities.contains(CapabilityId("backlog"))
            val enableDashboard = session.enabledCapabilities.contains(CapabilityId("dashboard"))
            val enableAttachments = session.enabledCapabilities.contains(CapabilityId("attachments"))
            val isProjectManagementEnabled = session.enabledCapabilities.contains(CapabilityId("advanced"))

            if (
                currentState.enableInbox == enableInbox &&
                currentState.enableLog == enableLog &&
                currentState.enableArtifact == enableArtifact &&
                currentState.enableBacklog == enableBacklog &&
                currentState.enableDashboard == enableDashboard &&
                currentState.enableAttachments == enableAttachments &&
                currentState.isProjectManagementEnabled == isProjectManagementEnabled &&
                currentState.experimentalCapabilityIds == data.config.experimentalCapabilityIds &&
                currentState.currentViewMode == session.currentView &&
                !currentState.isContextSwitching
            ) {
                currentState
            } else {
                currentState.copy(
                    enableInbox = enableInbox,
                    enableLog = enableLog,
                    enableArtifact = enableArtifact,
                    enableBacklog = enableBacklog,
                    enableDashboard = enableDashboard,
                    enableAttachments = enableAttachments,
                    isProjectManagementEnabled = isProjectManagementEnabled,
                    experimentalCapabilityIds = data.config.experimentalCapabilityIds,
                    currentViewMode = session.currentView,
                    isContextSwitching = false,
                )
            }
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
}
