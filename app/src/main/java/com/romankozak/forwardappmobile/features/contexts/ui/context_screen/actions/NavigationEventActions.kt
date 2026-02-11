package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.UiEvent

class NavigationEventActions {
    sealed class Effect {
        data class NavigateToProject(
            val contextId: String,
            val contextName: String,
        ) : Effect()

        data class EmitUiEvent(val event: UiEvent) : Effect()

        data class ResolveLink(val link: RelatedLink) : Effect()

        data class LogUnknownRoute(val route: String) : Effect()
    }

    fun fromRouteOutcome(outcome: NavigationActions.RouteOutcome): List<Effect> =
        when (outcome) {
            is NavigationActions.RouteOutcome.NavigateBack ->
                listOf(Effect.EmitUiEvent(UiEvent.NavigateBack))
            is NavigationActions.RouteOutcome.NavigateToProject ->
                listOf(Effect.NavigateToProject(outcome.contextId, outcome.contextName))
            is NavigationActions.RouteOutcome.Navigate ->
                listOf(Effect.EmitUiEvent(UiEvent.Navigate(outcome.target)))
            is NavigationActions.RouteOutcome.OpenUri ->
                listOf(Effect.EmitUiEvent(UiEvent.OpenUri(outcome.uri)))
            is NavigationActions.RouteOutcome.ShowMessage ->
                listOf(Effect.EmitUiEvent(UiEvent.ShowSnackbar(outcome.message)))
            is NavigationActions.RouteOutcome.HandleExistingLink ->
                listOf(Effect.ResolveLink(outcome.link))
            is NavigationActions.RouteOutcome.UnknownRoute ->
                listOf(Effect.LogUnknownRoute(outcome.route))
        }

    fun fromLinkClickResult(result: NavigationActions.LinkItemClickResult): List<Effect> =
        when (result) {
            is NavigationActions.LinkItemClickResult.NavigateToContext ->
                listOf(Effect.NavigateToProject(result.contextId, result.contextName))
            is NavigationActions.LinkItemClickResult.OpenUri ->
                listOf(Effect.EmitUiEvent(UiEvent.OpenUri(result.uri)))
            is NavigationActions.LinkItemClickResult.VaultNotConfigured ->
                listOf(Effect.EmitUiEvent(UiEvent.ShowSnackbar("Obsidian vault name is not configured.")))
            is NavigationActions.LinkItemClickResult.DelegateToUi ->
                listOf(Effect.EmitUiEvent(UiEvent.HandleLinkClick(result.link)))
        }

    fun fromRecentItemResult(result: RecentItemActions.Result): List<Effect> =
        when (result) {
            is RecentItemActions.Result.NavigateToProject ->
                listOf(Effect.NavigateToProject(result.contextId, result.contextName))
            is RecentItemActions.Result.Navigate ->
                listOf(Effect.EmitUiEvent(UiEvent.Navigate(result.target)))
            is RecentItemActions.Result.OpenUri ->
                listOf(Effect.EmitUiEvent(UiEvent.OpenUri(result.uri)))
            is RecentItemActions.Result.ShowMessage ->
                listOf(Effect.EmitUiEvent(UiEvent.ShowSnackbar(result.message)))
            RecentItemActions.Result.None -> emptyList()
        }
}
