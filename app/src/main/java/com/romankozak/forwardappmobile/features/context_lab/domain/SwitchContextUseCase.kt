package com.romankozak.forwardappmobile.features.context_lab.domain

import android.util.Log
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.context.*
import com.romankozak.forwardappmobile.core.navigation.capability.Navigator
import com.romankozak.forwardappmobile.core.navigation.capability.ScreenId
import com.romankozak.forwardappmobile.core.navigation.capability.ViewResolver
import com.romankozak.forwardappmobile.features.context_lab.ContextLabController
import javax.inject.Inject

private const val TAG = "SwitchContextUseCase"

class SwitchContextUseCase
    @Inject
    constructor(
        private val labController: ContextLabController,
        private val systemController: ContextController,
        private val viewResolver: ViewResolver,
        private val navigator: Navigator,
    ) {
        fun execute(contextId: ContextId) {
            try {
                Log.d(TAG, "Executing context switch for contextId: ${contextId.raw}")

                val context =
                    labController.getAllContexts().find { it.id == contextId }
                        ?: error("Context with id ${contextId.raw} not found")

                Log.d(TAG, "Found context: ${context.role.label}")
                Log.d(TAG, "Context config activeCapabilities: ${context.config.activeCapabilities}")
                Log.d(TAG, "Context config activeViews: ${context.config.activeViews}")
                Log.d(TAG, "Context config currentView: ${context.config.currentView}")

                val newState =
                    object : ContextState {
                        override val id: ContextId = context.id
                        override val features: CapabilitySet = CapabilitySet(active = context.config.activeCapabilities)
                        override val views: ViewSet = ViewSet(available = context.config.activeViews, start = context.config.currentView)
                    }

                systemController.update { newState }
                Log.d(TAG, "Updated systemController with new state")

                labController.activate(contextId)
                Log.d(TAG, "Activated context in labController")

                val startViewId = newState.views.start
                Log.d(TAG, "Attempting to navigate to start viewId: ${startViewId.raw}")

                val screenId = resolveValidScreen(startViewId, newState.views.available)

                if (screenId != null) {
                    Log.d(TAG, "Resolved valid screenId: ${screenId.raw}")
                    navigator.navigateTo(screenId)
                    Log.d(TAG, "Successfully navigated to screenId: ${screenId.raw}")
                } else {
                    val warnMsg = "No accessible screen found for context ${context.id.raw} with capabilities ${context.config.activeCapabilities}. No navigation will occur."
                    Log.w(TAG, warnMsg)
                    // Do not throw an exception, just stay on the current screen.
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during context switch for contextId: ${contextId.raw}", e)
                throw e
            }
        }

        private fun resolveValidScreen(
            preferredView: ViewId,
            availableViews: Set<ViewId>,
        ): ScreenId? {
            // 1. Try the preferred view first
            runCatching {
                viewResolver.resolve(preferredView)
            }.onSuccess { screenId ->
                Log.d(TAG, "Preferred view '${preferredView.raw}' is accessible.")
                return screenId
            }.onFailure {
                Log.w(TAG, "Preferred view '${preferredView.raw}' is not accessible: ${it.message}")
            }

            // 2. If preferred fails, iterate through available views
            Log.d(TAG, "Falling back to other available views.")
            for (viewId in availableViews) {
                if (viewId == preferredView) continue // Already tried
                runCatching {
                    viewResolver.resolve(viewId)
                }.onSuccess { screenId ->
                    Log.d(TAG, "Found accessible fallback view '${viewId.raw}'.")
                    return screenId
                }
            }

            // 3. If no view is accessible
            Log.e(TAG, "No accessible view found in the available set.")
            return null
        }
    }
