package com.romankozak.forwardappmobile.data.workspace

/** Canonical Workspace identity wins; legacy aliases require explicit CONTEXT_BACKED provenance. */
internal const val CONNECTIONS_OWNER_READ_SQL = """
    COALESCE(
        (SELECT id FROM workspaces WHERE id=:contextId AND
            ((sourceContextId IS NULL AND provenance IN ('CANONICAL_ONLY', 'STANDALONE')) OR
             (sourceContextId IS NOT NULL AND provenance='CONTEXT_BACKED')) LIMIT 1),
        (SELECT id FROM workspaces WHERE sourceContextId=:contextId AND provenance='CONTEXT_BACKED' LIMIT 1)
    )
"""
