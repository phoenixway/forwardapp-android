package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import java.net.URLEncoder

import androidx.navigation.NavController
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.handleRelatedLinkClick

internal fun handleRelatedLinkClick(
    link: RelatedLink,
    context: Context,
    navController: NavController,
    globalObsidianVaultName: String?,
) {
    com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.handleRelatedLinkClick(
        link = link,
        context = context,
        navController = navController,
        globalObsidianVaultName = globalObsidianVaultName,
    )
}
