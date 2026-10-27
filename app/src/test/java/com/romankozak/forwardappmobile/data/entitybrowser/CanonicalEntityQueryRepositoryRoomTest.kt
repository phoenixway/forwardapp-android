package com.romankozak.forwardappmobile.data.entitybrowser

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.orientation.toCanonicalAssessment
import androidx.room.withTransaction
import com.google.gson.Gson
import java.util.UUID
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitylibrary.canonicalPlacedRefs
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.shared.core.domain.orientation.DEFAULT_ORIENTATION_SCORING_POLICY_V1
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalEntityQueryRepositoryRoomTest {
    private suspend fun scored(f: SemanticDeletionRoomFixture, id: String, importance: String = "HIGH") {
        val old = f.dao.getAllAssessments().single { it.orientationId == id }
        val now = maxOf(old.updatedAt + 1, 30)
        val next = old.copy(revisionId = UUID.randomUUID().toString(), importanceValue = importance, importanceOrigin = "EXPLICIT",
            impactValue = "LARGE", impactOrigin = "EXPLICIT", effortValue = "LOW", effortOrigin = "EXPLICIT",
            costValue = "NONE", costOrigin = "EXPLICIT", riskValue = "MINIMAL", riskOrigin = "EXPLICIT",
            evaluationState = "ASSESSED", version = old.version + 1, updatedAt = now, syncedAt = null)
        f.db.withTransaction {
            f.dao.upsertAssessmentRevisions(listOf(OrientationAssessmentRevisionEntity(next.revisionId, id, now, now,
                "USER", "Query test assessment", Gson().toJson(next.toCanonicalAssessment()), now, now, null, false, 1)))
            f.dao.upsertAssessments(listOf(next))
        }
    }
    private suspend fun current(repo: CanonicalEntityQueryRepository, predicate: (List<CanonicalEntitySummary>) -> Boolean = { true }) =
        withTimeout(10_000) { repo.observeCatalog().first(predicate) }

    @Test fun `canonical owners typed collision tombstones and read-only isolation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); val aspect = f.aspect(); scored(f, id)
            f.db.workspaceDao().upsert(listOf(WorkspaceEntity(id, "Health Workspace", null, null, 1, 1, null, false, 1, "STANDALONE", null)))
            CanonicalWorkspaceTagRepository(f.db).replaceTags(id, listOf("#HEALTH", "finance"), 20)
            f.bindings.bind(id, id, com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType.EMBODIES, 25)
            val before = f.rows()
            val catalog = current(canonicalQueryCatalog(f.db))
            val semantic = catalog.single { it.ref == CanonicalEntityRef.Orientation(id) }
            val workspace = catalog.single { it.ref == CanonicalEntityRef.Workspace(id) }
            assertThat(semantic.scores[CanonicalScoreDimension.IMPORTANCE]).isEqualTo(7.0)
            assertThat(semantic.tags).isEmpty()
            assertThat(workspace.tags).containsExactly("health", "finance")
            assertThat(workspace.scores).isEmpty()
            assertThat(catalog.single { it.ref == CanonicalEntityRef.Aspect(aspect) }.scores).isEmpty()
            assertThat(executeCanonicalEntityQuery(catalog, CanonicalEntityQuery(tags = setOf("health"), scoreFilters = mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(min = 7.0))))).isEmpty()
            assertThat(f.rows()).isEqualTo(before)
            f.delete(id)
            assertThat(current(canonicalQueryCatalog(f.db)).map { it.ref }).doesNotContain(semantic.ref)
            assertThat(current(canonicalQueryCatalog(f.db)).map { it.ref }).contains(workspace.ref)
        }
    }

    @Test fun `tag additions and removals update one active subscription`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val repo = canonicalQueryCatalog(f.db)
            val tags = CanonicalWorkspaceTagRepository(f.db)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repo.observeCatalog().stateIn(scope)
                withTimeout(10_000) { state.first { it.size == 1 } }
                tags.replaceTags("workspace", listOf("health"), 20)
                val added = withTimeout(10_000) { state.first { it.single().tags == setOf("health") } }
                assertThat(executeCanonicalEntityQuery(added, CanonicalEntityQuery(tags = setOf("health")))).hasSize(1)
                tags.replaceTags("workspace", emptyList(), 30)
                val removed = withTimeout(10_000) { state.first { it.single().tags.isEmpty() } }
                assertThat(executeCanonicalEntityQuery(removed, CanonicalEntityQuery(tags = setOf("health")))).isEmpty()
            } finally { scope.cancel() }
        }
    }

    @Test fun `assessment and override updates are reactive and use current policy coordinates`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val repo = canonicalQueryCatalog(f.db)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = repo.observeCatalog().stateIn(scope)
                assertThat(state.value.single().scores).isEmpty()
                scored(f, id)
                withTimeout(10_000) { state.first { it.single().scores[CanonicalScoreDimension.IMPORTANCE] == 7.0 } }
                val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
                f.dao.upsertScoringOverrides(listOf(OrientationScoringOverrideEntity(id, policy.id, policy.version, policy.formulaVersion,
                    11f, null, null, null, null, 1, 40, null, false, 1)))
                val overridden = withTimeout(10_000) { state.first { it.single().scores[CanonicalScoreDimension.IMPORTANCE] == 11.0 } }
                assertThat(overridden.single().scores[CanonicalScoreDimension.SCORE]).isNotNull()
                f.dao.upsertScoringOverrides(listOf(f.dao.getScoringOverride(id)!!.copy(importanceCoordinate = 7.1f, version = 2, updatedAt = 45)))
                val decimal = withTimeout(10_000) { state.first { it.single().scores[CanonicalScoreDimension.IMPORTANCE] == 7.1 } }
                assertThat(executeCanonicalEntityQuery(decimal, CanonicalEntityQuery(scoreFilters = mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(min = 7.1))))).hasSize(1)
                scored(f, id, "CRITICAL")
                f.dao.upsertScoringOverrides(listOf(f.dao.getScoringOverride(id)!!.copy(importanceCoordinate = null, version = 3, updatedAt = 50)))
                withTimeout(10_000) { state.first { it.single().scores[CanonicalScoreDimension.IMPORTANCE] == 10.0 } }
            } finally { scope.cancel() }
        }
    }

    @Test fun `unassessed unscorable missing and unknown values do not become numeric zero`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val initial = f.dao.getAllAssessments().single()
            assertThat(canonicalQueryScores(initial, null)).isEmpty()
            scored(f, id)
            val assessed = f.dao.getAllAssessments().single()
            assertThat(canonicalQueryScores(assessed.copy(evaluationState = "UNSCORABLE"), null)).isEmpty()
            assertThat(canonicalQueryScores(assessed.copy(evaluationState = "FUTURE"), null)).isEmpty()
            val partial = canonicalQueryScores(assessed.copy(importanceValue = "UNKNOWN"), null)
            assertThat(partial).doesNotContainKey(CanonicalScoreDimension.IMPORTANCE)
            assertThat(partial).doesNotContainKey(CanonicalScoreDimension.SCORE)
        }
    }

    @Test fun `unknown scoring policy fails closed without hiding canonical identity`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); scored(f, id)
            val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
            f.dao.upsertScoringOverrides(listOf(OrientationScoringOverrideEntity(id, "future-policy", policy.version, policy.formulaVersion,
                null, null, null, null, null, 1, 40, null, false, 1)))
            val catalog = current(canonicalQueryCatalog(f.db))
            assertThat(catalog.single().ref).isEqualTo(CanonicalEntityRef.Orientation(id))
            assertThat(catalog.single().scores).isEmpty()
        }
    }

    @Test fun `Unplaced tags and scores use canonical H1 presence independently`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); scored(f, id); f.workspace()
            CanonicalWorkspaceTagRepository(f.db).replaceTags("workspace", listOf("health"), 20)
            val repo = canonicalQueryCatalog(f.db)
            val catalog = current(repo)
            val presence = CanonicalHierarchyPlacementPresenceRepository(f.db.hierarchyPlacementDao())
            val query = CanonicalEntityQuery(unplaced = true, scoreFilters = mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(min = 7.0)))
            assertThat(executeCanonicalEntityQuery(catalog, query)).hasSize(1)
            f.appearance(id)
            val placed = canonicalPlacedRefs(catalog, presence.observeVisiblePlacedTargets().first())
            assertThat(executeCanonicalEntityQuery(catalog, query, placed)).isEmpty()
            assertThat(executeCanonicalEntityQuery(catalog, CanonicalEntityQuery(unplaced = true, tags = setOf("health")), placed).single().ref)
                .isEqualTo(CanonicalEntityRef.Workspace("workspace"))
        }
    }

    @Test fun `rename and canonical tombstone update the observed inventory`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val state = canonicalQueryCatalog(f.db).observeCatalog().stateIn(scope)
                val subject = f.dao.getAllManagedSubjects().single()
                f.dao.upsertManagedSubjects(listOf(subject.copy(title = "Renamed", version = subject.version + 1, updatedAt = 30)))
                withTimeout(10_000) { state.first { it.single().title == "Renamed" } }
                f.delete(id, version = subject.version + 1)
                withTimeout(10_000) { state.first { it.isEmpty() } }
            } finally { scope.cancel() }
        }
    }

    @Test fun `semantic tags combine with scoring and old saved specification without changing Workspace authority`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); val aspect = f.aspect(); scored(f, id); f.workspace()
            val tags = com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticTagRepository(f.db)
            val orientation = com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef(
                com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType.ORIENTATION, id)
            val facet = com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef(
                com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType.ASPECT, aspect)
            f.bindings.bind(subjectId = id, workspaceId = "workspace", bindingType = com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType.EMBODIES, now = 25)
            CanonicalWorkspaceTagRepository(f.db).replaceTags("workspace", listOf("operational"), 20)
            val query = CanonicalEntityQuery(entityTypes = setOf(CanonicalEntityClass.ORIENTATION), tags = setOf("health"),
                scoreFilters = mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(min = 7.0)))
            val saved = CanonicalEntityQueryCodec.encode(query)
            assertThat(executeCanonicalEntityQuery(current(canonicalQueryCatalog(f.db)), CanonicalEntityQueryCodec.decode(saved).query!!)).isEmpty()
            tags.addTag(orientation, "#HEALTH", 30); tags.addTag(orientation, "finance", 31); tags.addTag(facet, "health", 30)
            val catalog = current(canonicalQueryCatalog(f.db)) { it.single { s -> s.ref == CanonicalEntityRef.Orientation(id) }.tags.size == 2 }
            assertThat(executeCanonicalEntityQuery(catalog, CanonicalEntityQueryCodec.decode(saved).query!!).single().ref).isEqualTo(CanonicalEntityRef.Orientation(id))
            assertThat(executeCanonicalEntityQuery(catalog, CanonicalEntityQuery(tags = setOf("health"))).map { it.ref })
                .containsExactly(CanonicalEntityRef.Orientation(id), CanonicalEntityRef.Aspect(aspect))
            assertThat(executeCanonicalEntityQuery(catalog, CanonicalEntityQuery(tags = setOf("health", "finance"))).single().ref).isEqualTo(CanonicalEntityRef.Orientation(id))
            assertThat(executeCanonicalEntityQuery(catalog, CanonicalEntityQuery(tags = setOf("operational"))).single().ref).isEqualTo(CanonicalEntityRef.Workspace("workspace"))
            assertThat(catalog.single { it.ref == CanonicalEntityRef.Workspace("workspace") }.tags).containsExactly("operational")
            tags.removeTag(orientation, "health", 40)
            val after = current(canonicalQueryCatalog(f.db)) { "health" !in it.single { s -> s.ref == CanonicalEntityRef.Orientation(id) }.tags }
            assertThat(executeCanonicalEntityQuery(after, CanonicalEntityQueryCodec.decode(saved).query!!)).isEmpty()
            assertThat(CanonicalEntityQueryCodec.encode(query)).isEqualTo(saved)
        }
    }
}
