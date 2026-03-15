package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import java.net.URLEncoder

internal fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
) {
    try {
        when (link.type) {
            LinkType.URL -> context.startActivity(Intent(Intent.ACTION_VIEW, link.target.toUri()))
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    context.startActivity(Intent(Intent.ACTION_VIEW, obsidianUri.toUri()))
                } else {
                    Toast.makeText(context, "Назву Obsidian сховища не встановлено.", Toast.LENGTH_LONG).show()
                }
            }
            else -> Unit
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Не вдалося відкрити посилання.", Toast.LENGTH_LONG).show()
    }
}
