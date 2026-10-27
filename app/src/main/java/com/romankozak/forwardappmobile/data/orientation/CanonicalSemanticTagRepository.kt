package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectTagRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.managedSubjectTagId
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.normalizeSemanticTag
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class CanonicalSemanticTagRepository @Inject constructor(private val db: AppDatabase) {
    fun observeAllTags() = db.managedSubjectTagDao().observeLive().map { rows ->
        rows.groupBy { ManagedSubjectRef(ManagedSubjectType.valueOf(it.subjectType), it.subjectId) }
            .mapValues { (_, refs) -> refs.map { it.normalizedTag }.toSet() }
    }
    fun observeTags(subject: ManagedSubjectRef) = observeAllTags().map { it[subject].orEmpty() }

    suspend fun addTag(subject: ManagedSubjectRef, tag: String, now: Long = System.currentTimeMillis()) = db.withTransaction {
        requireOwner(subject)
        require(now >= 0)
        val normalized = normalizeSemanticTag(tag)
        require(normalized.isNotBlank()) { "Вкажіть непорожній тег" }
        val dao = db.managedSubjectTagDao()
        val id = managedSubjectTagId(subject, normalized)
        val old = dao.get(id)
        if (old?.isDeleted == false) return@withTransaction
        dao.upsert(listOf(old?.copy(isDeleted = false, version = Math.addExact(old.version, 1),
            updatedAt = maxOf(now, old.updatedAt), syncedAt = null)
            ?: ManagedSubjectTagRefEntity(id, subject.type.name, subject.id, normalized, now, now, null, false, 1)))
    }
    suspend fun removeTag(subject: ManagedSubjectRef, tag: String, now: Long = System.currentTimeMillis()) = db.withTransaction {
        requireOwner(subject)
        val row = db.managedSubjectTagDao().get(managedSubjectTagId(subject, normalizeSemanticTag(tag))) ?: return@withTransaction
        retire(listOf(row), now)
    }
    internal suspend fun retireForSubject(subject: ManagedSubjectRef, now: Long) = db.withTransaction {
        retire(db.managedSubjectTagDao().getForSubject(subject.type.name, subject.id), now)
    }
    private suspend fun retire(rows: List<ManagedSubjectTagRefEntity>, now: Long) {
        require(now >= 0)
        db.managedSubjectTagDao().upsert(rows.filterNot { it.isDeleted }.map {
            it.copy(isDeleted = true, version = Math.addExact(it.version, 1), updatedAt = maxOf(now, it.updatedAt), syncedAt = null)
        })
    }
    internal suspend fun requireOwner(subject: ManagedSubjectRef, live: Boolean = true) {
        val dao = db.orientationDao()
        val owner = requireNotNull(dao.getManagedSubject(subject.id)) { "Semantic tag owner is missing" }
        require(owner.subjectType == subject.type.name) { "Semantic tag owner type mismatch" }
        require(!live || !owner.isDeleted) { "Semantic tag owner is deleted" }
        when (subject.type) {
            ManagedSubjectType.ORIENTATION -> requireNotNull(dao.getOrientation(subject.id))
            ManagedSubjectType.ASPECT -> requireNotNull(dao.getAspect(subject.id))
        }
    }
}
