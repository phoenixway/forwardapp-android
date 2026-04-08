package com.romankozak.forwardappmobile.features.mainscreen.lifemanagement

import com.romankozak.forwardappmobile.core.data.models.entities.FreshnessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.GeneralStatus
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelId
import com.romankozak.forwardappmobile.core.data.models.entities.TransferStatus

data class LifeManagementLevelStatus(
    val levelId: LifeManagementLevelId,
    val generalStatus: GeneralStatus,
    val transferStatus: TransferStatus,
    val freshnessStatus: FreshnessStatus,
    val blockerText: String,
    val nextActionText: String,
    val updatedAt: Long,
)

data class LifeManagementLevelStatusUpdate(
    val levelId: LifeManagementLevelId,
    val generalStatus: GeneralStatus,
    val transferStatus: TransferStatus,
    val freshnessStatus: FreshnessStatus,
    val blockerText: String,
    val nextActionText: String,
)
