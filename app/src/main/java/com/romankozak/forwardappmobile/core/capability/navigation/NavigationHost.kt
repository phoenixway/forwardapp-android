package com.romankozak.forwardappmobile.core.capability.navigation

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.gate.CapabilityGate

@Composable
fun NavigationHost(
    navState: NavState,
    featureGate: CapabilityGate,
    screenRegistry: ScreenRegistry,
    contextId: ContextId
) {
    val screenId = navState.currentScreen

    val screen = screenRegistry.get(screenId)

    if (
        screen != null &&
        featureGate.isCapabilityEnabled(
            contextId,
            screenId.capabilityId
        )
    ) {
        screen.Render()
    } else {
        AccessDeniedScreen()
    }
}
