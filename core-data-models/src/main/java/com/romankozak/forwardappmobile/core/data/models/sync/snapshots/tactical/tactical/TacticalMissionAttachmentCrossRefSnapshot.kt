package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical

import com.google.gson.annotations.SerializedName

data class TacticalMissionAttachmentCrossRefSnapshot(
    @SerializedName("missionId") val missionId: Long,
    @SerializedName("attachmentId") val attachmentId: String
)
