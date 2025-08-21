// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/sync/SyncModel.kt ---
package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.*

// --- Класи для розбору/створення десктопного бекапу ---

// ✨ ОНОВЛЕНО: DesktopGoal тепер має relatedLinks, але для сумісності ми будемо
// конвертувати його в associatedListIds під час експорту.
data class DesktopGoal(
    val id: String,
    val text: String,
    val description: String?,
    val completed: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val associatedListIds: List<String>?, // Поле для сумісності
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
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED
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
    val tags: List<String>? = null
)

data class DesktopGoalInstance(
    val id: String,
    val goalId: String
)

// ✨ ДОДАНО: Модель для нотаток у форматі десктопу
data class DesktopNote(
    val id: String,
    val title: String?,
    val content: String,
    val createdAt: String,
    val updatedAt: String?
)

// ✨ ОНОВЛЕНО: DesktopBackupData тепер містить нотатки
data class DesktopBackupData(
    val goals: Map<String, DesktopGoal>?,
    val goalLists: Map<String, DesktopGoalList>?,
    val goalInstances: Map<String, DesktopGoalInstance>?,
    val notes: Map<String, DesktopNote>? = null // ✨ ДОДАНО
)

data class DesktopBackupFile(
    val version: Int,
    val exportedAt: String,
    val data: DesktopBackupData?
)

// --- Класи для ЕКСПОРТУ з мобільного додатку (внутрішній формат) ---
// ✨ ОНОВЛЕНО: BackupData тепер використовує ListItem та Note
data class BackupData(
    val goalLists: List<GoalList>,
    val goals: List<Goal>,
    val notes: List<Note>,
    val listItems: List<ListItem>
)

data class BackupFile(
    val version: Int = 5, // ✨ Оновлено версію
    val exportedAt: Long = System.currentTimeMillis(),
    val data: BackupData
)