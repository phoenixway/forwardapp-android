package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.navigation.ContextRouteResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder

class NavigationActions(
    private val contextRepository: ContextRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
    handleLinkClickRoute: String,
) {
    private val routeResolver = ContextRouteResolver(handleLinkClickRoute)

    data class GoalDetailNavigation(
        val contextId: String,
        val contextName: String,
    )

    sealed class HandleLinkClickResult {
        data class ExistingLink(val link: RelatedLink) : HandleLinkClickResult()

        data class OpenObsidianNote(val noteTarget: String) : HandleLinkClickResult()

        data class OpenUri(val uri: String) : HandleLinkClickResult()

        data class NavigateToContext(val contextId: String, val contextName: String) : HandleLinkClickResult()

        data class UnknownTarget(val target: String) : HandleLinkClickResult()
    }

    sealed class OpenObsidianNoteResult {
        data class OpenUri(val uri: String) : OpenObsidianNoteResult()

        data object VaultNotConfigured : OpenObsidianNoteResult()
    }

    sealed class LinkItemClickResult {
        data class NavigateToContext(val contextId: String, val contextName: String) : LinkItemClickResult()

        data class OpenUri(val uri: String) : LinkItemClickResult()

        data object VaultNotConfigured : LinkItemClickResult()

        data class DelegateToUi(val link: RelatedLink) : LinkItemClickResult()
    }

    sealed class RouteOutcome {
        data object NavigateBack : RouteOutcome()

        data class NavigateToProject(
            val contextId: String,
            val contextName: String,
        ) : RouteOutcome()

        data class Navigate(val target: NavTarget) : RouteOutcome()

        data class OpenUri(val uri: String) : RouteOutcome()

        data class ShowMessage(val message: String) : RouteOutcome()

        data class HandleExistingLink(val link: RelatedLink) : RouteOutcome()

        data class UnknownRoute(val route: String) : RouteOutcome()
    }

    suspend fun resolveRoute(
        route: String,
        links: List<RelatedLink>,
    ): RouteOutcome {
        return when (val result = routeResolver.resolve(route)) {
            is ContextRouteResolver.ResolveResult.Back -> RouteOutcome.NavigateBack

            is ContextRouteResolver.ResolveResult.GoalDetail -> {
                val goalDetail = resolveGoalDetail(result.contextId)
                RouteOutcome.NavigateToProject(goalDetail.contextId, goalDetail.contextName)
            }

            is ContextRouteResolver.ResolveResult.HandleLinkClick -> {
                when (val linkResult = resolveHandleLinkClick(result.rawTarget, links)) {
                    is HandleLinkClickResult.ExistingLink -> RouteOutcome.HandleExistingLink(linkResult.link)
                    is HandleLinkClickResult.OpenObsidianNote -> {
                        when (val noteResult = resolveObsidianNoteOpen(linkResult.noteTarget)) {
                            is OpenObsidianNoteResult.OpenUri -> RouteOutcome.OpenUri(noteResult.uri)
                            is OpenObsidianNoteResult.VaultNotConfigured ->
                                RouteOutcome.ShowMessage(
                                    "Назву Obsidian сховища не встановлено.",
                                )
                        }
                    }

                    is HandleLinkClickResult.OpenUri -> RouteOutcome.OpenUri(linkResult.uri)
                    is HandleLinkClickResult.NavigateToContext ->
                        RouteOutcome.NavigateToProject(linkResult.contextId, linkResult.contextName)
                    is HandleLinkClickResult.UnknownTarget ->
                        RouteOutcome.ShowMessage("Unknown link: ${linkResult.target}")
                }
            }

            is ContextRouteResolver.ResolveResult.Navigate -> RouteOutcome.Navigate(result.target)
            is ContextRouteResolver.ResolveResult.Unknown -> RouteOutcome.UnknownRoute(result.route)
        }
    }

    suspend fun resolveHandleLinkClick(
        rawTarget: String,
        links: List<RelatedLink>,
    ): HandleLinkClickResult {
        val target = runCatching { URLDecoder.decode(rawTarget, "UTF-8") }.getOrDefault(rawTarget)
        val link =
            links.find {
                it.target == target || runCatching { URLEncoder.encode(it.target, "UTF-8") }.getOrNull() == rawTarget
            }
        val obsidianNoteTarget = extractObsidianNoteTarget(target)
        val context = withContext(ioDispatcher) { contextRepository.getContextById(target) }

        return when {
            link != null -> HandleLinkClickResult.ExistingLink(link)
            obsidianNoteTarget != null -> HandleLinkClickResult.OpenObsidianNote(obsidianNoteTarget)
            target.startsWith("obsidian://") -> HandleLinkClickResult.OpenUri(target)
            target.startsWith("http://") || target.startsWith("https://") ->
                HandleLinkClickResult.OpenUri(target)
            context != null -> HandleLinkClickResult.NavigateToContext(context.id, context.name)
            else -> HandleLinkClickResult.UnknownTarget(target)
        }
    }

    suspend fun resolveGoalDetail(contextId: String): GoalDetailNavigation {
        val contextName =
            withContext(ioDispatcher) {
                contextRepository.getContextById(contextId)?.name ?: "Context"
            }
        return GoalDetailNavigation(contextId = contextId, contextName = contextName)
    }

    suspend fun resolveObsidianNoteOpen(noteTarget: String): OpenObsidianNoteResult {
        val vaultName = settingsRepository.obsidianVaultNameFlow.first()
        if (vaultName.isBlank()) return OpenObsidianNoteResult.VaultNotConfigured
        val encodedVault = URLEncoder.encode(vaultName, "UTF-8")
        val encodedNoteName = URLEncoder.encode(noteTarget, "UTF-8")
        return OpenObsidianNoteResult.OpenUri("obsidian://open?vault=$encodedVault&file=$encodedNoteName")
    }

    suspend fun resolveLinkItemClick(link: RelatedLink): LinkItemClickResult {
        return when (link.type) {
            com.romankozak.forwardappmobile.core.data.models.entities.LinkType.CONTEXT -> {
                LinkItemClickResult.NavigateToContext(
                    contextId = link.target,
                    contextName = link.displayName ?: "Context",
                )
            }

            com.romankozak.forwardappmobile.core.data.models.entities.LinkType.OBSIDIAN -> {
                recentItemsRepository.logObsidianLinkAccess(link)
                when (val noteResult = resolveObsidianNoteOpen(link.target)) {
                    is OpenObsidianNoteResult.OpenUri -> LinkItemClickResult.OpenUri(noteResult.uri)
                    is OpenObsidianNoteResult.VaultNotConfigured -> LinkItemClickResult.VaultNotConfigured
                }
            }

            com.romankozak.forwardappmobile.core.data.models.entities.LinkType.NOTE_DOCUMENT,
            com.romankozak.forwardappmobile.core.data.models.entities.LinkType.JOURNAL_DOCUMENT,
            com.romankozak.forwardappmobile.core.data.models.entities.LinkType.CHECKLIST,
            com.romankozak.forwardappmobile.core.data.models.entities.LinkType.MUSIC_NOTE -> {
                LinkItemClickResult.DelegateToUi(link)
            }
            else -> LinkItemClickResult.DelegateToUi(link)
        }
    }

    private fun extractObsidianNoteTarget(target: String): String? {
        val trimmed = target.trim()
        val wikiLinkTarget =
            if (trimmed.startsWith(WIKI_LINK_PREFIX) &&
                trimmed.endsWith(WIKI_LINK_SUFFIX) &&
                trimmed.length > MIN_WIKI_LINK_LENGTH
            ) {
                trimmed
                    .substring(WIKI_LINK_PREFIX.length, trimmed.length - WIKI_LINK_SUFFIX.length)
                    .trim()
                    .takeIf { it.isNotBlank() }
            } else {
                null
            }

        val encodedFile =
            if (trimmed.startsWith("obsidian://")) {
                Regex("""[?&]file=([^&]+)""").find(trimmed)?.groupValues?.get(1)
                    ?: Regex("""[?&]name=([^&]+)""").find(trimmed)?.groupValues?.get(1)
            } else {
                null
            }

        return wikiLinkTarget
            ?: encodedFile
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }
}

private const val WIKI_LINK_PREFIX = "[["
private const val WIKI_LINK_SUFFIX = "]]"
private const val MIN_WIKI_LINK_LENGTH = 4
