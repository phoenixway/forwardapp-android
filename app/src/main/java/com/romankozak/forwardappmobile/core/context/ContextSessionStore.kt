package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ContextSessionState(
    val enabledCapabilities: Set<CapabilityId> = emptySet(),
    val availableViews: List<ContextViewMode> = emptyList(),
    val currentView: ContextViewMode = ContextViewMode.DASHBOARD,
)

sealed interface ContextCommand {
    data class SyncFromConfig(
        val contextId: String,
        val config: ContextConfiguration,
        val preferredViewName: String?,
        val currentView: ContextViewMode,
    ) : ContextCommand

    data class SelectView(
        val requested: ContextViewMode,
    ) : ContextCommand
}

class ContextSessionStore(
    private val controller: ContextController,
    private val capabilitiesResolver: ContextCapabilitiesResolver,
) {
    private val _state = MutableStateFlow(ContextSessionState())
    val state: StateFlow<ContextSessionState> = _state.asStateFlow()

    fun dispatch(command: ContextCommand): ContextSessionState {
        return when (command) {
            is ContextCommand.SyncFromConfig ->
                syncFromConfig(
                    contextId = command.contextId,
                    config = command.config,
                    preferredViewName = command.preferredViewName,
                    currentView = command.currentView,
                )
            is ContextCommand.SelectView -> {
                val resolved = selectView(command.requested)
                _state.value.copy(currentView = resolved)
            }
        }
    }

    fun syncFromConfig(
        contextId: String,
        config: ContextConfiguration,
        preferredViewName: String?,
        currentView: ContextViewMode,
    ): ContextSessionState {
        val enabled = capabilitiesResolver.resolve(config)
        val availableViews = ContextViewPolicy.availableViews(enabled)
        val preferred = preferredViewName?.let(::parseViewMode)
        val resolved = ContextViewPolicy.resolveView(availableViews, preferred, currentView)

        val newState =
            ContextSessionState(
                enabledCapabilities = enabled,
                availableViews = availableViews,
                currentView = resolved,
            )
        _state.value = newState

        controller.set(
            DefaultContextState(
                id = ContextId(contextId),
                features = CapabilitySet(active = enabled),
                views = ViewSet(availableViews.map { ViewId(it.name.lowercase()) }.toSet(), ViewId(resolved.name.lowercase())),
                config = config,
            ),
        )
        return newState
    }

    fun selectView(requested: ContextViewMode): ContextViewMode {
        val available = _state.value.availableViews
        val resolved = ContextViewPolicy.resolveView(available, requested, _state.value.currentView)
        _state.value = _state.value.copy(currentView = resolved)
        controller.update { current ->
            current.copyViews(resolved)
        }
        return resolved
    }

    private fun parseViewMode(raw: String): ContextViewMode? {
        if (raw.equals("ATTACHMENTS", ignoreCase = true)) return ContextViewMode.CONNECTIONS
        return runCatching { ContextViewMode.valueOf(raw) }.getOrNull()
    }

    private fun ContextState.copyViews(mode: ContextViewMode): ContextState {
        val updatedViews = ViewSet(views.available, ViewId(mode.name.lowercase()))
        return DefaultContextState(
            id = id,
            features = features,
            views = updatedViews,
            config = config,
        )
    }
}

data class DefaultContextState(
    override val id: ContextId,
    override val features: CapabilitySet,
    override val views: ViewSet,
    override val config: com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration,
) : ContextState
