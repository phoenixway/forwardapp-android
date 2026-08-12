package com.romankozak.forwardappmobile.desktop.features.workbench

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.desktop.features.contexts.DesktopContextExplorerScreen
import com.romankozak.forwardappmobile.desktop.features.contexts.DesktopWorkspaceDependencies
import com.romankozak.forwardappmobile.desktop.features.contexts.rememberDesktopWorkspaceDependencies

@Composable
fun DesktopWorkbenchScreen(
    dependencies: DesktopWorkspaceDependencies = rememberDesktopWorkspaceDependencies(),
    initialContextId: String? = null,
    refreshKey: Long = 0L,
) {
    DesktopContextExplorerScreen(
        dependencies = dependencies,
        initialContextId = initialContextId,
        refreshKey = refreshKey,
    )
}
