package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectTagRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.managedSubjectTagId
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.*

/** Nullable additive stream: absence never becomes an inferred semantic tag. */
class CanonicalSemanticTagSyncStore(private val db: AppDatabase) {
    private val dao = db.managedSubjectTagDao()
    suspend fun merge(rows: List<ManagedSubjectTagRefEntity>?) {
        if (rows == null) return
        db.withTransaction {
            validate(rows)
            val current = dao.getAll().associateBy { it.id }
            val winners = rows.filter { incoming ->
                val old = current[incoming.id]
                require(old == null || (old.subjectType == incoming.subjectType && old.subjectId == incoming.subjectId &&
                    old.normalizedTag == incoming.normalizedTag && old.createdAt == incoming.createdAt)) { "Semantic tag identity changed" }
                when {
                    old == null || incoming.version > old.version -> true
                    incoming.version < old.version -> false
                    incoming.copy(syncedAt = null) == old.copy(syncedAt = null) -> false
                    else -> error("Equal-version semantic tag divergence: ${incoming.id}")
                }
            }
            dao.upsert(winners.map { it.copy(syncedAt = null) })
        }
    }
    suspend fun restore(rows: List<ManagedSubjectTagRefEntity>?) {
        if (rows == null) return
        db.withTransaction { validate(rows); dao.clear(); dao.upsert(rows) }
    }
    private suspend fun validate(rows: List<ManagedSubjectTagRefEntity>) {
        require(rows.map { it.id }.distinct().size == rows.size) { "Duplicate semantic tag identity" }
        val subjects = db.orientationDao().getAllManagedSubjects().associateBy { it.id }
        val orientations = db.orientationDao().getAllOrientations().mapTo(hashSetOf()) { it.subjectId }
        val aspects = db.orientationDao().getAllAspects().mapTo(hashSetOf()) { it.subjectId }
        val formerAspects = CanonicalSubjectConversionStore(db).formerAspectIds()
        rows.forEach {
            val ref = ManagedSubjectRef(ManagedSubjectType.valueOf(it.subjectType), it.subjectId)
            require(it.normalizedTag.isNotBlank() && normalizeSemanticTag(it.normalizedTag) == it.normalizedTag)
            require(it.id == managedSubjectTagId(ref, it.normalizedTag)) { "Semantic tag identity/tuple mismatch" }
            require(it.version >= 1 && it.createdAt >= 0 && it.updatedAt >= it.createdAt && it.syncedAt?.let { time -> time >= 0 } != false)
            // Retained rows under tombstoned subjects are history; every live read joins owner lifecycle.
            val historical = it.isDeleted && ref.type == ManagedSubjectType.ASPECT && ref.id in formerAspects
            require(historical || subjects[ref.id]?.subjectType == ref.type.name) { "Missing or mismatched semantic tag owner" }
            require(historical || ref.id in if (ref.type == ManagedSubjectType.ORIENTATION) orientations else aspects) { "Missing semantic subtype" }
        }
    }
}
