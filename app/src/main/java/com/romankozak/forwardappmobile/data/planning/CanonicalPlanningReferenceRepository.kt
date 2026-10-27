package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.database.AppDatabase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Owns references only. Does not create subjects, commitments, bindings or appearances. */
@Singleton
class CanonicalPlanningReferenceRepository @Inject constructor(private val db: AppDatabase) {
    fun observeAll() = db.planningReferenceDao().observeAll()
    fun observeWorkspaceRelations(workspaceId: String) = db.planningReferenceDao().observeWorkspaceRelations(workspaceId)
    fun observeAspectRelations(aspectId: String) = db.planningReferenceDao().observeAspectRelations(aspectId)

    suspend fun attach(
        owner: PlanningReferenceOwner,
        target: PlanningReferenceTarget,
        role: PlanningReferenceRole = PlanningReferenceRole.RELATED,
        now: Long = System.currentTimeMillis(),
    ): String = db.withTransaction {
        requireOwner(owner, live = true)
        requireTarget(target, live = true)
        val rows = db.planningReferenceDao().getForOwner(owner.kind.name, owner.id)
        val previous = rows.singleOrNull {
            it.targetKind == target.kind.name && it.targetId == target.id && it.role == role.name
        }
        if (previous != null && !previous.isDeleted) return@withTransaction previous.id
        if (role == PlanningReferenceRole.SOURCE) {
            require(rows.none { it.role == role.name && !it.isDeleted }) { "Planning item already has a source" }
        }
        val key = listOf(owner.kind.name, owner.id, target.kind.name, target.id, role.name)
            .joinToString("") { "${it.length}:$it" }
        val id = previous?.id ?: UUID.nameUUIDFromBytes("planning-reference:v1:$key".toByteArray(Charsets.UTF_8)).toString()
        db.planningReferenceDao().upsert(
            PlanningReferenceEntity(id, owner.kind.name, owner.id, target.kind.name, target.id, role.name,
                previous?.createdAt ?: now, now, null, (previous?.version ?: 0) + 1, false),
        )
        id
    }

    suspend fun remove(id: String, expectedVersion: Long, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val row = requireNotNull(db.planningReferenceDao().get(id)) { "Missing Planning reference" }
        require(row.version == expectedVersion) { "Stale Planning reference" }
        if (!row.isDeleted) db.planningReferenceDao().upsert(
            row.copy(isDeleted = true, version = row.version + 1, updatedAt = now, syncedAt = null),
        )
    }

    suspend fun retireOwner(owner: PlanningReferenceOwner, now: Long) = db.withTransaction {
        db.planningReferenceDao().getForOwner(owner.kind.name, owner.id).filterNot { it.isDeleted }.forEach {
            remove(it.id, it.version, now)
        }
    }

    internal fun requireOwner(owner: PlanningReferenceOwner, live: Boolean) {
        val (table, deleted) = when (owner.kind) {
            PlanningReferenceOwnerKind.SCOPE -> "planning_scopes" to "isDeleted"
            PlanningReferenceOwnerKind.TACTICAL_MISSION -> "tactical_missions" to "is_deleted"
            PlanningReferenceOwnerKind.ARC_QUEST -> "arc_quests" to "is_deleted"
            PlanningReferenceOwnerKind.DAY_TASK -> "day_tasks" to "isDeleted"
            PlanningReferenceOwnerKind.DAY_FOCUS -> "day_focus_items" to "isDeleted"
        }
        requireExists(table, "id", owner.id, if (live) "$deleted=0" else "1")
    }

    internal fun requireTarget(target: PlanningReferenceTarget, live: Boolean) {
        if (target.kind == PlanningReferenceTargetKind.WORKSPACE) {
            requireExists("workspaces", "id", target.id, if (live) "isDeleted=0" else "1")
        } else {
            val subtype = if (target.kind == PlanningReferenceTargetKind.ORIENTATION) "orientations" else "aspects"
            requireExists("managed_subjects", "id", target.id,
                "subjectType='${target.kind.name}'" + if (live) " AND isDeleted=0" else "")
            requireExists(subtype, "subjectId", target.id, "1")
        }
    }

    private fun requireExists(table: String, column: String, id: String, predicate: String) {
        // Identifiers/predicates are closed constants; IDs are always query parameters.
        db.openHelper.readableDatabase.query("SELECT 1 FROM $table WHERE $column=? AND $predicate", arrayOf(id)).use {
            require(it.moveToFirst()) { "Unavailable typed Planning endpoint: $table/$id" }
        }
    }
}
