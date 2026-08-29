package com.romankozak.forwardappmobile.data.workspace

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
    }
