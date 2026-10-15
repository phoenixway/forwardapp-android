package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.ResolvedWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.shared.contracts.contexts.requireValidCanonicalPlanningTransport
import kotlinx.serialization.json.Json

class WorkspaceSnapshotResolver(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val androidImporter = AndroidWorkspaceSnapshotImporter(json = json)

    fun resolve(snapshotText: String): ResolvedWorkspaceSnapshot? =
        runCatching {
            json.decodeFromString(DesktopWorkspaceSnapshot.serializer(), snapshotText)
        }.map { snapshot -> snapshot.also { it.requireValidCanonicalPlanningTransport() } }
            .getOrNull()?.let { snapshot ->
            ResolvedWorkspaceSnapshot(
                snapshot = snapshot,
                format = WorkspaceSnapshotFormat.Desktop,
            )
        } ?: runCatching { androidImporter.parse(snapshotText) }.getOrNull()
}
