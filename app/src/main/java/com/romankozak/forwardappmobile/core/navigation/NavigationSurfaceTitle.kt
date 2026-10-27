package com.romankozak.forwardappmobile.core.navigation

import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.NavigationType

/** Display-only compatibility for retained history; no route or persisted entry rewrite. */
internal fun NavigationEntry.navigationSurfaceTitle(): String =
    if (type == NavigationType.PROJECT_HIERARCHY_SCREEN && title == "Big Picture") "Navigation" else title
