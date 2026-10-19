package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.capability.*
import com.romankozak.forwardappmobile.database.AppDatabase

/** Real operational owners, not mocked deletion companions. */
internal fun workspaceDeletionRepository(db: AppDatabase): CanonicalWorkspaceRepository {
    val instances = CanonicalCapabilityInstanceStore(db, db.workspaceDao(), db.orientationDao())
    return CanonicalWorkspaceRepository(
        database = db, workspaceDao = db.workspaceDao(), orientationDao = db.orientationDao(),
        executionLogRepository = CanonicalExecutionLogRepository(db, db.workspaceDao(), db.contextManagementDao(), instances),
        keyProblemsRepository = CanonicalKeyProblemsRepository(db, instances, db.workspaceProblemDao(), db.workspaceDao(), db.attachmentDao()),
        directionRepository = CanonicalDirectionRepository(db, instances, db.workspaceDirectionEntryDao(), db.workspaceDao(), db.orientationDao(),
            CanonicalOrientationRepository(db, db.orientationDao())),
        inboxRepository = CanonicalInboxRepository(db, instances, db.workspaceInboxRecordDao()),
        connectionsRepository = CanonicalConnectionsRepository(db, instances, db.workspaceConnectionDao()),
        backlogRepository = CanonicalBacklogRepository(db, instances, db.workspaceBacklogEntryDao(), CanonicalBacklogTargetValidator(db)),
        hierarchyPlacementLifecycleCoordinator = HierarchyPlacementLifecycleCoordinator(db),
        hierarchyPlacementRepository = CanonicalHierarchyPlacementRepository(db),
        h1StructuralMutationCoordinator = H1StructuralMutationCoordinator(db),
    )
}
