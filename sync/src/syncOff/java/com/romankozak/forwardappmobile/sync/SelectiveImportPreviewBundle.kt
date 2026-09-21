package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.BackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.LegacyBackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportDescriptor

data class SelectiveImportPreviewBundle(
    val descriptor: WorkspaceImportDescriptor,
    val sourceSnapshotBundle: SnapshotBundle?,
    val legacyDiff: LegacyBackupDiff? = null,
    val snapshotDiff: BackupDiff? = null,
) {
    init {
        require((legacyDiff != null) xor (snapshotDiff != null)) {
            "SelectiveImportPreviewBundle must contain exactly one diff variant."
        }
    }
}
