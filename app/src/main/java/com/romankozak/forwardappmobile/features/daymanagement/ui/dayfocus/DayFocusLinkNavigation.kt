package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter

/** Focus Workspace links use the canonical destination's availability gate, not the legacy operational screen. */
internal fun dayFocusWorkspaceRoute(link: RelatedLink): String? =
    if (link.type == LinkType.CONTEXT) NavTargetRouter.routeOf(NavTarget.Workspace(link.target)) else null
