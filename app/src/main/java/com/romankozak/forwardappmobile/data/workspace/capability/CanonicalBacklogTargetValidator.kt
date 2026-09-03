package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import javax.inject.Inject
import javax.inject.Singleton

/** External-domain validation boundary for live canonical BACKLOG placements. */
@Singleton
class CanonicalBacklogTargetValidator
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun requireLive(target: WorkspaceBacklogTargetRef) {
            require(target.id.isNotBlank()) { "Backlog target id must not be blank" }
            when (target.kind) {
                WorkspaceBacklogTargetKind.ORIENTATION -> requireLiveOrientation(target.id)
                WorkspaceBacklogTargetKind.WORKSPACE -> {
                    val workspace = requireNotNull(database.workspaceDao().getById(target.id)) {
                        "Backlog target Workspace does not exist"
                    }
                    require(!workspace.isDeleted) { "Backlog target Workspace is deleted" }
                }
                WorkspaceBacklogTargetKind.LINK_ITEM -> {
                    val item = requireNotNull(database.linkItemDao().getLinkItemById(target.id)) {
                        "Backlog target LinkItem does not exist"
                    }
                    require(!item.isDeleted) { "Backlog target LinkItem is deleted" }
                }
                WorkspaceBacklogTargetKind.LEGACY_NOTE -> {
                    val note = requireNotNull(database.legacyNoteDao().getNoteById(target.id)) {
                        "Backlog target legacy Note does not exist"
                    }
                    require(!note.isDeleted) { "Backlog target legacy Note is deleted" }
                }
                WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
                -> {
                    val document = requireNotNull(database.noteDocumentDao().getDocumentById(target.id)) {
                        "Backlog target document does not exist"
                    }
                    require(!document.isDeleted) { "Backlog target document is deleted" }
                }
                WorkspaceBacklogTargetKind.CHECKLIST -> {
                    val checklist = requireNotNull(database.checklistDao().getChecklistById(target.id)) {
                        "Backlog target Checklist does not exist"
                    }
                    require(!checklist.isDeleted) { "Backlog target Checklist is deleted" }
                }
                WorkspaceBacklogTargetKind.MUSIC_NOTE -> {
                    val music = requireNotNull(database.musicNoteDao().getById(target.id)) {
                        "Backlog target MusicNote does not exist"
                    }
                    require(!music.isDeleted) { "Backlog target MusicNote is deleted" }
                }
            }
        }

        private suspend fun requireLiveOrientation(id: String) {
            val subject = requireNotNull(database.orientationDao().getManagedSubject(id)) {
                "Backlog target Orientation subject does not exist"
            }
            require(!subject.isDeleted) { "Backlog target Orientation subject is deleted" }
            require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                "Backlog ORIENTATION target must reference an Orientation subject"
            }
            require(database.orientationDao().getAllOrientations().any { it.subjectId == id }) {
                "Backlog target Orientation node does not exist"
            }
        }
    }
