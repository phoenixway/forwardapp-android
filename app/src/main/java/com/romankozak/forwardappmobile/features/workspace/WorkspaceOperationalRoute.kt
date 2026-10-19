package com.romankozak.forwardappmobile.features.workspace

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Canonical route identity wins even over stale restored legacy arguments. */
internal class WorkspaceOperationalRoute(savedState: SavedStateHandle) {
    val isCanonicalWorkspace = savedState.contains("workspaceId")
    private val identityKey = if (isCanonicalWorkspace) "workspaceId" else "listId"
    init {
        if (savedState.get<String>(identityKey) == null) savedState[identityKey] = ""
    }
    val id: StateFlow<String> = savedState.getStateFlow(identityKey, "")
}

internal fun workspacePropertiesDestination(workspaceId: String?, legacyContextId: String?): NavTarget =
    workspaceId?.let { NavTarget.WorkspaceProperties(it) }
        ?: NavTarget.ProjectSettings(projectId = legacyContextId)

/** Opening a canonical Workspace is read-only; legacy auto-linking stays a legacy entry policy. */
internal fun observeLegacyDirectionLinksOnOpen(
    route: WorkspaceOperationalRoute,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    repository: ContextRepository,
) {
    if (route.isCanonicalWorkspace) return
    scope.launch(dispatcher) {
        route.id.filter { it.isNotBlank() }.distinctUntilChanged().collectLatest { contextId ->
            try {
                repository.ensureDirectionFrontLinksForExistingChildren(contextId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("WorkspaceOperationalRoute", "Legacy Direction auto-link failed for $contextId", error)
            }
        }
    }
}
