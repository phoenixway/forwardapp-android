package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.UiEvent

class NavigationEffectDispatcherActions(
    private val navigationEventActions: NavigationEventActions,
) {
    suspend fun dispatch(
        effects: List<NavigationEventActions.Effect>,
        navigateToProject: suspend (contextId: String, contextName: String) -> Unit,
        emitUiEvent: (UiEvent) -> Unit,
        resolveLinkClick: suspend (RelatedLink) -> NavigationActions.LinkItemClickResult,
        logUnknownRoute: (String) -> Unit,
    ) {
        effects.forEach { effect ->
            when (effect) {
                is NavigationEventActions.Effect.NavigateToProject ->
                    navigateToProject(effect.contextId, effect.contextName)
                is NavigationEventActions.Effect.EmitUiEvent ->
                    emitUiEvent(effect.event)
                is NavigationEventActions.Effect.ResolveLink -> {
                    val linkResult = resolveLinkClick(effect.link)
                    dispatch(
                        effects = navigationEventActions.fromLinkClickResult(linkResult),
                        navigateToProject = navigateToProject,
                        emitUiEvent = emitUiEvent,
                        resolveLinkClick = resolveLinkClick,
                        logUnknownRoute = logUnknownRoute,
                    )
                }
                is NavigationEventActions.Effect.LogUnknownRoute ->
                    logUnknownRoute(effect.route)
            }
        }
    }
}
