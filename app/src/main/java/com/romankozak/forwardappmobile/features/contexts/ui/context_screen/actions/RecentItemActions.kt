package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.net.URLEncoder

class RecentItemActions(
    private val settingsRepository: SettingsRepository,
) {
    sealed class Result {
        data class NavigateToProject(
            val contextId: String,
            val contextName: String,
        ) : Result()

        data class Navigate(val target: NavTarget) : Result()

        data class OpenUri(val uri: String) : Result()

        data class ShowMessage(val message: String) : Result()

        data object None : Result()
    }

    suspend fun resolve(item: RecentItem): Result {
        return when (item.type) {
            RecentItemType.PROJECT -> {
                Result.NavigateToProject(
                    contextId = item.target,
                    contextName = item.displayName ?: "Context",
                )
            }

            RecentItemType.NOTE -> Result.None
            RecentItemType.NOTE_DOCUMENT -> Result.Navigate(NavTarget.NoteDocument(id = item.target))
            RecentItemType.CHECKLIST -> Result.Navigate(NavTarget.Checklist(id = item.target))
            RecentItemType.MUSIC_NOTE -> Result.Navigate(NavTarget.MusicNote(id = item.target))
            RecentItemType.OBSIDIAN_LINK -> {
                val vaultName = settingsRepository.obsidianVaultNameFlow.first()
                if (vaultName.isBlank()) {
                    Result.ShowMessage("Obsidian vault name is not configured.")
                } else {
                    val encodedNoteName = URLEncoder.encode(item.target, "UTF-8")
                    Result.OpenUri("obsidian://open?vault=$vaultName&file=$encodedNoteName")
                }
            }
        }
    }
}
