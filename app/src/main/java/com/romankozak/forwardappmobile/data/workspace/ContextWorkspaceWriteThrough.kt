package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/** Atomic compatibility boundary for legacy Context mutations and their Workspace shadow. */
@Singleton
class ContextWorkspaceWriteThrough
    @Inject
    constructor(
        private val bootstrapper: CanonicalWorkspaceBootstrapper,
    ) {
        suspend fun <T> mutate(
            now: Long = System.currentTimeMillis(),
            mutation: suspend () -> T,
        ): T = bootstrapper.mutateAndRefresh(now, mutation)

        /**
         * Explicit pre-canonical backup ingress. Normal Context writes and
         * startup refresh never use legacy System capability projection.
         */
        suspend fun ingestLegacySystemCapabilityProjection(
            importedContextIds: Set<String>,
            now: Long = System.currentTimeMillis(),
        ) = bootstrapper.ingestLegacySystemCapabilityProjection(
            importedContextIds = importedContextIds,
            now = now,
        )

        suspend fun ingestLegacySystemCapabilityProjection(
            legacyContextEvidence: List<SystemWorkspaceLegacyContextEvidence>,
            legacyConfigurationEvidence: List<ContextConfiguration> = emptyList(),
            now: Long = System.currentTimeMillis(),
        ) = bootstrapper.ingestLegacySystemCapabilityProjection(
            legacyContextEvidence = legacyContextEvidence,
            legacyConfigurationEvidence = legacyConfigurationEvidence,
            now = now,
        )

        /** Runs owner-dependent work after the Context-to-Workspace projection is current. */
        suspend fun <T> mutateAndAfterWorkspaceRefresh(
            now: Long = System.currentTimeMillis(),
            mutation: suspend () -> T,
            afterRefresh: suspend (T) -> Unit,
        ): T = bootstrapper.mutateAndAfterRefresh(now, mutation, afterRefresh)
    }
