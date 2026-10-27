package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.managedSubjectTagId
import com.romankozak.forwardappmobile.shared.core.domain.orientation.matchesOrientationFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalSemanticTagRoomTest {
    @Test fun `Orientation owns normalized semantic membership with stable identity`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            val unrelatedBefore = f.rows() - "managed_subject_tag_refs"
            repo.addTag(ref, " #HEALTH ", 20)
            repo.addTag(ref, " # # HEALTH ", 21)
            val row = f.db.managedSubjectTagDao().getAll().single()
            assertThat(row.id).isEqualTo(managedSubjectTagId(ref, "health"))
            assertThat(row.normalizedTag).isEqualTo("health")
            assertThat(row.subjectType).isEqualTo("ORIENTATION")
            assertThat(row.version).isEqualTo(1)
            assertThat(row.syncedAt).isNull()
            assertThat(normalizeSemanticTag(row.normalizedTag)).isEqualTo(row.normalizedTag)
            assertThat(repo.observeTags(ref).first()).containsExactly("health")
            assertThat(f.rows() - "managed_subject_tag_refs").isEqualTo(unrelatedBefore)
        }
    }
    @Test fun `Aspect owns equivalent semantic membership`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ASPECT, f.aspect())
            CanonicalSemanticTagRepository(f.db).addTag(ref, "#FAMILY", 20)
            assertThat(CanonicalSemanticTagRepository(f.db).observeTags(ref).first()).containsExactly("family")
            assertThat(f.db.managedSubjectTagDao().getAll().single().subjectType).isEqualTo("ASPECT")
        }
    }
    @Test fun `duplicate normalization is idempotent and does not dirty acknowledged row`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            repo.addTag(ref, "health", 20)
            val dao = f.db.managedSubjectTagDao()
            dao.markSynced(managedSubjectTagId(ref, "health"), 1, 25)
            val before = dao.getAll()
            repo.addTag(ref, " #HEALTH ", 30)
            assertThat(dao.getAll()).isEqualTo(before)
        }
    }
    @Test fun `remove tombstones membership only and explicit readd preserves identity`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            repo.addTag(ref, "health", 20)
            val old = f.db.managedSubjectTagDao().getAll().single()
            f.db.managedSubjectTagDao().markSynced(old.id, old.version, 25)
            repo.removeTag(ref, "#HEALTH", 30)
            val removed = f.db.managedSubjectTagDao().getAll().single()
            assertThat(removed.isDeleted).isTrue()
            assertThat(removed.version).isEqualTo(2)
            assertThat(removed.syncedAt).isNull()
            assertThat(repo.observeTags(ref).first()).isEmpty()
            assertThat(f.dao.getManagedSubject(ref.id)!!.isDeleted).isFalse()
            repo.addTag(ref, "health", 40)
            val restored = f.db.managedSubjectTagDao().getAll().single()
            assertThat(restored.id).isEqualTo(old.id)
            assertThat(restored.createdAt).isEqualTo(old.createdAt)
            assertThat(restored.version).isEqualTo(3)
        }
    }
    @Test fun `Orientation and Aspect subscriptions react without refresh`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val repo = CanonicalSemanticTagRepository(f.db)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val all = repo.observeAllTags().stateIn(scope)
                for (ref in listOf(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation()), ManagedSubjectRef(ManagedSubjectType.ASPECT, f.aspect()))) {
                    repo.addTag(ref, "health", 20)
                    withTimeout(5_000) { all.first { it[ref] == setOf("health") } }
                    repo.removeTag(ref, "health", 30)
                    withTimeout(5_000) { all.first { it[ref].orEmpty().isEmpty() } }
                }
            } finally { scope.cancel() }
        }
    }
    @Test fun `same tag on different typed subjects is independent`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val repo = CanonicalSemanticTagRepository(f.db)
            val o = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val a = ManagedSubjectRef(ManagedSubjectType.ASPECT, f.aspect())
            repo.addTag(o, "shared", 20); repo.addTag(a, "shared", 20)
            repo.removeTag(o, "shared", 30)
            assertThat(repo.observeTags(a).first()).containsExactly("shared")
            assertThat(f.db.managedSubjectTagDao().getAll().map { it.id }.distinct()).hasSize(2)
        }
    }
    @Test fun `same raw id with wrong semantic class fails closed instead of guessing`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val o = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, id)
            val wrong = ManagedSubjectRef(ManagedSubjectType.ASPECT, id)
            assertThat(managedSubjectTagId(o, "health")).isNotEqualTo(managedSubjectTagId(wrong, "health"))
            val repo = CanonicalSemanticTagRepository(f.db)
            repo.addTag(o, "health", 20)
            assertThat(runCatching { repo.addTag(wrong, "health", 30) }.isFailure).isTrue()
            assertThat(repo.observeTags(wrong).first()).isEmpty()
            assertThat(f.db.managedSubjectTagDao().getAll()).hasSize(1)
        }
    }
    @Test fun `canonical source deletion retires tags for both semantic classes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            for (ref in listOf(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation()), ManagedSubjectRef(ManagedSubjectType.ASPECT, f.aspect()))) {
                val repo = CanonicalSemanticTagRepository(f.db)
                repo.addTag(ref, "health", 20)
                f.delete(ref.id, ref.type)
                assertThat(repo.observeTags(ref).first()).isEmpty()
                val row = f.db.managedSubjectTagDao().get(managedSubjectTagId(ref, "health"))!!
                assertThat(row.isDeleted).isTrue()
                assertThat(row.version).isEqualTo(2)
            }
        }
    }
    @Test fun `read excludes inactive owner even for historical unretired membership`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            repo.addTag(ref, "health", 20)
            val owner = f.dao.getManagedSubject(ref.id)!!
            f.dao.upsertManagedSubjects(listOf(owner.copy(isDeleted = true, version = 2)))
            assertThat(repo.observeTags(ref).first()).isEmpty()
            assertThat(runCatching { repo.addTag(ref, "other", 30) }.isFailure).isTrue()
        }
    }
    @Test fun `late tag retirement failure rolls back source and H1 retirement`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); f.appearance(id)
            CanonicalSemanticTagRepository(f.db).addTag(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, id), "health", 20)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_tag_retirement BEFORE UPDATE ON managed_subject_tag_refs BEGIN SELECT RAISE(ABORT, 'tag failure'); END")
            val before = f.rows()
            assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }
    @Test fun `existing shared TagFilter uses canonical tags rather than empty environment`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            val s = f.dao.getManagedSubject(ref.id)!!
            val model = EffectiveOrientation(ManagedSubject(s.id, s.createdAt, s.updatedAt, s.syncedAt, s.isDeleted, s.version,
                ManagedSubjectType.ORIENTATION, s.title, s.description),
                OrientationNode(ref.id, OrientationKind.INTENT, null, ValueOrigin.UNSET, emptyApplicableAssessment()),
                LegacySubjectRef(LegacyOrientationSourceType.GOAL, "test-fixture-only"), emptyList(), emptyList())
            assertThat(repo.observeMatchingOrientations(flowOf(listOf(model)), TagFilter(listOf("health"))).first()).isEmpty()
            repo.addTag(ref, "health", 20)
            assertThat(repo.observeMatchingOrientations(flowOf(listOf(model)), TagFilter(listOf("health"))).first()).containsExactly(model)
            val environment = repo.observeFilterEnvironment().first()
            assertThat(matchesOrientationFilter(model, TagFilter(listOf("health", "missing")), environment)).isFalse()
        }
    }
    @Test fun `empty tags rejected and clock rollback cannot decrease membership timestamp`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val ref = ManagedSubjectRef(ManagedSubjectType.ORIENTATION, f.orientation())
            val repo = CanonicalSemanticTagRepository(f.db)
            assertThat(runCatching { repo.addTag(ref, " # ", 20) }.isFailure).isTrue()
            repo.addTag(ref, "health", 20); repo.removeTag(ref, "health", 15)
            assertThat(f.db.managedSubjectTagDao().getAll().single().updatedAt).isEqualTo(20)
        }
    }
}
