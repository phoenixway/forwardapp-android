package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.data.workspace.SystemContextShellRetirer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceLegacyContextEvidence
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical reserved-System Workspace ownership convergence boundary.
 *
 * This initializer never creates or repairs reserved System Context rows.
 * Fresh databases materialize canonical System Workspaces directly. Existing
 * historical Context rows remain bounded upgrade/old-backup evidence only.
 */
@Singleton
class DatabaseInitializer
    @Inject
    constructor(
        private val systemWorkspaceMaterializer: SystemWorkspaceMaterializer,
        private val systemWorkspaceTagSeed: SystemWorkspaceTagSeed,
        private val systemContextShellRetirer: SystemContextShellRetirer,
    ) {
        suspend fun ensureCanonicalSystemWorkspaceOwnership(
            legacyContextEvidence: Collection<SystemWorkspaceLegacyContextEvidence> = emptyList(),
        ) {
            // Fresh identities, persisted historical evidence, and transient
            // old-backup evidence all converge directly to canonical System
            // Workspace ownership. No reserved Context shell is created.
            systemWorkspaceMaterializer.materializeAll(
                legacyContextEvidence = legacyContextEvidence,
            )

            // Establish canonical tag collections before consuming the final
            // persisted reserved-System Context compatibility evidence.
            systemWorkspaceTagSeed.seedMissingCanonicalCollections()

            // Step 11: once every exact reserved identity has a valid canonical
            // owner and established canonical tag collection, active legacy
            // Context shells are no longer authoritative or required.
            systemContextShellRetirer.retireActiveReservedShells()
        }
    }
