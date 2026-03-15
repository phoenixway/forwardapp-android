package com.romankozak.forwardappmobile.features.missions.presentation

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus

data class MissionDialogConfig(
    val title: String,
    val initialTitle: String,
    val initialDescription: String,
    val initialDeadline: String,
    val initialProjectLinks: List<String>,
    val initialAttachmentLinks: List<String>,
    val attachmentOptions: List<AttachmentOption>,
    val initialStatus: MissionStatus = MissionStatus.ACTIVE,
    val confirmText: String,
)
