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
import java.net.URLEncoder

fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
    navController: NavController,
) {
    runCatching {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, link.target.toUri())
                context.startActivity(intent)
            }
            LinkType.CONTEXT -> {
                navController.navigate("project_detail_screen/${link.target}")
            }
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
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
