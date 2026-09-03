package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import java.net.URLEncoder

fun handleRelatedLinkClick(
    link: RelatedLink,
    context: Context,
    navController: NavController,
    globalObsidianVaultName: String?,
) {
    runCatching {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, link.target.toUri())
                context.startActivity(intent)
            }
            LinkType.CONTEXT -> {
                navController.navigate(NavigationRoutes.contextDetail(contextId = link.target))
            }
            LinkType.NOTE_DOCUMENT,
            -> {
                navController.navigate(NavigationRoutes.noteDocument(link.target, false))
            }
            LinkType.CHECKLIST -> {
                navController.navigate("checklist_screen?checklistId=${link.target}")
            }
            LinkType.MUSIC_NOTE -> {
                navController.navigate("music_note_screen/${link.target}")
            }
            LinkType.OBSIDIAN -> {
                val vaultName = link.vault?.takeIf { it.isNotBlank() } ?: globalObsidianVaultName?.takeIf { it.isNotBlank() }
                if (vaultName != null) {
                    val encodedVault = URLEncoder.encode(vaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    val intent = Intent(Intent.ACTION_VIEW, obsidianUri.toUri())
                    context.startActivity(intent)
                } else {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.error_obsidian_vault_not_set),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            null -> {}
        }
    }.onFailure { error ->
        Log.e(TAG, "Failed to open related link", error)
        Toast
            .makeText(
                context,
                context.getString(R.string.error_link_open_failed),
                Toast.LENGTH_LONG,
            ).show()
    }
}

private const val TAG = "LinkHelpers"
