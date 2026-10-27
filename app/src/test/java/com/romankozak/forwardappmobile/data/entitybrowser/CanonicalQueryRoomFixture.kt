package com.romankozak.forwardappmobile.data.entitybrowser

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository

internal fun canonicalQueryCatalog(db: AppDatabase) = CanonicalEntityQueryRepository(
    CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao()), CanonicalWorkspaceTagRepository(db), db.orientationDao(),
    com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticTagRepository(db),
)
