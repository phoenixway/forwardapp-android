package com.romankozak.forwardappmobile.data.database.models

import com.romankozak.forwardappmobile.features.contexts.data.models.Goal

data class GoalWithProjectName(
    val goal: Goal,
    val projectName: String?
)
