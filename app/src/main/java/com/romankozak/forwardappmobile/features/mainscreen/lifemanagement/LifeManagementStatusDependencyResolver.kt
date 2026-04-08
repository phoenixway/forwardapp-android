package com.romankozak.forwardappmobile.features.mainscreen.lifemanagement

import com.romankozak.forwardappmobile.core.data.models.entities.FreshnessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeManagementStatusDependencyResolver
    @Inject
    constructor() {
        fun descendantsOf(levelId: LifeManagementLevelId): List<LifeManagementLevelId> =
            LifeManagementLevelId.entries.filter { it.order > levelId.order }

        fun freshnessForDescendantsOf(levelId: LifeManagementLevelId): FreshnessStatus? =
            descendantsOf(levelId)
                .takeIf { it.isNotEmpty() }
                ?.let { FreshnessStatus.OUTDATED_BY_PARENT }
    }
