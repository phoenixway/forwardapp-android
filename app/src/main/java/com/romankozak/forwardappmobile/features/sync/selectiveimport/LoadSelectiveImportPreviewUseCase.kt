package com.romankozak.forwardappmobile.features.sync.selectiveimport

import android.net.Uri
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportSourceMode
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.sync.SyncRepository
import timber.log.Timber
import javax.inject.Inject

class LoadSelectiveImportPreviewUseCase
    @Inject
    constructor(
        private val syncRepository: SyncRepository,
    ) {
        suspend operator fun invoke(fileUriString: String?): Result<SelectiveImportPreview> {
            if (fileUriString == null) {
                return Result.failure(IllegalArgumentException("File URI not provided."))
            }

            val fileUri = Uri.parse(fileUriString)
            Timber.tag(TAG).d("Loading selective import preview from URI: $fileUri")

            return syncRepository.loadSelectiveImportPreview(fileUri).map { previewBundle ->
                SelectiveImportPreview(
                    backupContent =
                        previewBundle.legacyDiff?.toSelectable()
                            ?: previewBundle.snapshotDiff?.toSelectable()
                            ?: error("Preview bundle does not contain a diff."),
                    sourceSnapshotBundle = previewBundle.sourceSnapshotBundle,
                    sourceMode = previewBundle.descriptor.sourceMode,
                    sourceFormat = previewBundle.descriptor.format,
                )
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to build selective import preview")
            }
        }

        companion object {
            private const val TAG = "IMPORT_SELECTIVE"
        }
    }

data class SelectiveImportPreview(
    val backupContent: SelectableDatabaseContent,
    val sourceSnapshotBundle: com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle?,
    val sourceMode: WorkspaceImportSourceMode,
    val sourceFormat: WorkspaceSnapshotFormat,
)
