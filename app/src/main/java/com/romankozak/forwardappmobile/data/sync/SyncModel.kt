package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.*

data class DesktopGoal(
    val id: String,
    val text: String,
    val description: String?,
    val completed: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val associatedListIds: List<String>?,
    val tags: List<String>? = null,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
)

data class DesktopGoalList(
    val id: String,
    val name: String,
    val description: String?,
    val itemInstanceIds: List<String>,
    val createdAt: String,
    val updatedAt: String?,
    val parentId: String?,
    val isExpanded: Boolean? = true,
    val order: Long? = 0,
    val tags: List<String>? = null,
)

data class DesktopGoalInstance(
    val id: String,
    val goalId: String,
)

data class DesktopNote(
    val id: String,
    val title: String?,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
)

data class DesktopBackupData(
    val goals: Map<String, DesktopGoal>?,
    val goalLists: Map<String, DesktopGoalList>?,
    val goalInstances: Map<String, DesktopGoalInstance>?,
    val notes: Map<String, DesktopNote>? = null,
)

data class DesktopBackupFile(
    val version: Int,
    val exportedAt: String,
    val data: DesktopBackupData?,
)

data class BackupData(
    val goalLists: List<Project>,
    val goals: List<Goal>,
    val listItems: List<ListItem>,
)

data class BackupFile(
    val version: Int = 5,
    val exportedAt: Long = System.currentTimeMillis(),
    val data: BackupData,
)
