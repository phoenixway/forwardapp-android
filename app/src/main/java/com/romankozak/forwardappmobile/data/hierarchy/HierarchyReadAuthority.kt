package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.currentHierarchyPlacementAuthorityMode

/**
 * Single dormant read-authority seam for the future P2 switch.
 *
 * H4.0d deliberately leaves production at CURRENT_PRE_CUTOVER. V2_AUTHORITY is
 * reachable only when explicitly supplied by readiness tests or the later P2
 * orchestration.
 *
 * There is deliberately no V2 -> CURRENT fallback.
 */
class HierarchyReadAuthorityRouter(
    private val authorityMode: HierarchyPlacementAuthorityMode =
        currentHierarchyPlacementAuthorityMode(),
) {
    fun <T> route(
        currentPreCutover: () -> T,
        v2Authority: () -> T,
    ): T =
        when (authorityMode) {
            HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER ->
                currentPreCutover()

            HierarchyPlacementAuthorityMode.V2_AUTHORITY ->
                v2Authority()
        }

    suspend fun <T> routeSuspend(
        currentPreCutover: suspend () -> T,
        v2Authority: suspend () -> T,
    ): T =
        when (authorityMode) {
            HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER ->
                currentPreCutover()

            HierarchyPlacementAuthorityMode.V2_AUTHORITY ->
                v2Authority()
        }
}
