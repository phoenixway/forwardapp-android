package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder

class NavigationActions(
    private val contextRepository: ContextRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
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

    suspend fun resolveHandleLinkClick(
        rawTarget: String,
        links: List<RelatedLink>,
    ): HandleLinkClickResult {
        val target = runCatching { URLDecoder.decode(rawTarget, "UTF-8") }.getOrDefault(rawTarget)
        val link =
            links.find {
                it.target == target || runCatching { URLEncoder.encode(it.target, "UTF-8") }.getOrNull() == rawTarget
            }
        if (link != null) return HandleLinkClickResult.ExistingLink(link)

        val obsidianNoteTarget = extractObsidianNoteTarget(target)
        if (obsidianNoteTarget != null) return HandleLinkClickResult.OpenObsidianNote(obsidianNoteTarget)
        if (target.startsWith("obsidian://")) return HandleLinkClickResult.OpenUri(target)
        if (target.startsWith("http://") || target.startsWith("https://")) return HandleLinkClickResult.OpenUri(target)

        val context = withContext(ioDispatcher) { contextRepository.getContextById(target) }
        if (context != null) {
            return HandleLinkClickResult.NavigateToContext(context.id, context.name)
        }

        return HandleLinkClickResult.UnknownTarget(target)
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

            else -> LinkItemClickResult.DelegateToUi(link)
        }
    }

    private fun extractObsidianNoteTarget(target: String): String? {
        val trimmed = target.trim()
        if (trimmed.startsWith("[[") && trimmed.endsWith("]]") && trimmed.length > 4) {
            return trimmed.substring(2, trimmed.length - 2).trim().takeIf { it.isNotBlank() }
        }
        if (!trimmed.startsWith("obsidian://")) return null
        val encodedFile =
            Regex("""[?&]file=([^&]+)""").find(trimmed)?.groupValues?.get(1)
                ?: Regex("""[?&]name=([^&]+)""").find(trimmed)?.groupValues?.get(1)
        return encodedFile
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }
}
