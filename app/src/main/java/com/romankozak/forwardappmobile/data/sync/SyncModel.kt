// SyncModel.kt
package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList

// --- Класи для розбору десктопного бекапу ---
data class DesktopGoal(
    val id: String,
    val text: String,
    val completed: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val associatedListIds: List<String>?
)

data class DesktopGoalList(
    val id: String,
    val name: String,
    val description: String?,
    val itemInstanceIds: List<String>,
    val createdAt: String,
    val updatedAt: String?,
    val parentId: String?,
    val childListIds: List<String>?,
    val isExpanded: Boolean? = true,
    val order: Long? = 0
)

data class DesktopGoalInstance(
    val id: String,
    val goalId: String
)

data class DesktopBackupData(
    val goals: Map<String, DesktopGoal>,
    val goalLists: Map<String, DesktopGoalList>,
    val goalInstances: Map<String, DesktopGoalInstance>,
    val rootListIds: List<String>?
)

data class DesktopBackupFile(
    val version: Int,
    val exportedAt: String,
    val data: DesktopBackupData
)

// --- Класи для ЕКСПОРТУ з мобільного додатку ---
data class BackupData(
    val goalLists: List<GoalList>,
    val goals: List<Goal>,
    val goalInstances: List<GoalInstance>
)

data class BackupFile(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val data: BackupData
)

// --- Класи для екрану схвалення змін ---
data class SyncReport(
    val changes: List<SyncChange>
)