package com.romankozak.forwardappmobile.desktop.features.contexts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerState
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

data class DesktopContextCapability(
    val id: String,
    val title: String,
    val view: SharedContextView,
    val isAvailable: (SharedContextSummary) -> Boolean = { true },
    val render: @Composable (
        state: WorkspaceExplorerState,
        onIntent: (WorkspaceExplorerIntent) -> Unit,
        modifier: Modifier,
    ) -> Unit,
)

class DesktopContextCapabilityRegistry(
    private val capabilities: List<DesktopContextCapability>,
) {
    fun availableFor(context: SharedContextSummary?): List<DesktopContextCapability> =
        if (context == null) {
            emptyList()
        } else {
            val enabledCapabilities = context.resolvedCapabilityIds()
            capabilities.filter { capability ->
                capability.id in enabledCapabilities && capability.isAvailable(context)
            }
        }

    fun capabilityFor(view: SharedContextView): DesktopContextCapability? =
        capabilities.firstOrNull { capability -> capability.view == view }

    companion object {
        fun default(): DesktopContextCapabilityRegistry =
            DesktopContextCapabilityRegistry(
                listOf(
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Backlog),
                        title = SharedContextView.Backlog.title,
                        view = SharedContextView.Backlog,
                        render = { state, onIntent, modifier ->
                            DesktopBacklogReader(
                                title = state.selectedContextName,
                                items = state.backlogItems,
                                savingItemId = state.savingBacklogItemId,
                                deletingItemId = state.deletingBacklogItemId,
                                editingItemId = state.editingBacklogItemId,
                                isCreatingItem = state.isCreatingBacklogItem,
                                draftTitle = state.backlogDraftTitle,
                                draftDetails = state.backlogDraftDetails,
                                draftPriority = state.backlogDraftPriority,
                                onIntent = onIntent,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Inbox),
                        title = SharedContextView.Inbox.title,
                        view = SharedContextView.Inbox,
                        render = { state, _, modifier ->
                            DesktopContextInboxView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Connections),
                        title = SharedContextView.Connections.title,
                        view = SharedContextView.Connections,
                        render = { state, onIntent, modifier ->
                            DesktopContextConnectionsView(
                                state = state,
                                onIntent = onIntent,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Dashboard),
                        title = SharedContextView.Dashboard.title,
                        view = SharedContextView.Dashboard,
                        render = { state, _, modifier ->
                            DesktopContextDashboardView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Direction),
                        title = SharedContextView.Direction.title,
                        view = SharedContextView.Direction,
                        render = { state, _, modifier ->
                            DesktopContextDirectionView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Log),
                        title = SharedContextView.Log.title,
                        view = SharedContextView.Log,
                        render = { state, _, modifier ->
                            DesktopContextLogView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.JournalLog),
                        title = SharedContextView.JournalLog.title,
                        view = SharedContextView.JournalLog,
                        render = { state, _, modifier ->
                            DesktopContextJournalLogView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.Artifact),
                        title = SharedContextView.Artifact.title,
                        view = SharedContextView.Artifact,
                        render = { state, _, modifier ->
                            DesktopContextArtifactView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                    DesktopContextCapability(
                        id = SharedContextCapabilityCatalog.capabilityIdFor(SharedContextView.KeyProblems),
                        title = SharedContextView.KeyProblems.title,
                        view = SharedContextView.KeyProblems,
                        render = { state, _, modifier ->
                            DesktopContextKeyProblemsView(
                                state = state,
                                modifier = modifier,
                            )
                        },
                    ),
                ),
            )
    }
}

private fun SharedContextSummary.resolvedCapabilityIds(): Set<String> {
    val explicitIds =
        SharedContextCapabilityCatalog.normalizeCapabilityIds(enabledCapabilityIds + experimentalCapabilityIds)
    val fallbackIds =
        if (explicitIds.any { capabilityId -> capabilityId.isNotBlank() }) {
            emptyList()
        } else {
            SharedContextCapabilityCatalog.defaultCapabilityIdsFor(defaultView)
        }
    return (explicitIds + fallbackIds + SharedContextCapabilityCatalog.capabilityIdFor(defaultView))
        .filter { capabilityId -> capabilityId.isNotBlank() }
        .toSet()
}
