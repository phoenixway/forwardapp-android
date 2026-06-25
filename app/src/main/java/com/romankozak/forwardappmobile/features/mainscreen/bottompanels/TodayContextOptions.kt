package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.LinkOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

internal fun buildTodayContextOptions(options: List<LinkOption>): List<ProjectOption> =
    options.map { option ->
        ProjectOption(
            id = option.id,
            name = option.name,
            parentId = null,
        )
    }
