package com.romankozak.forwardappmobile.data.planning.strategic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalPlanningActivationReport
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalStrategicPlanningAuthorityActivatorRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `activation materializes exact Strategic aggregate writes marker last and reruns inert`() = runBlocking {
        withDatabase { db ->
            seedManualQuest(db)
            val activator = activator(db)

            val first = activator.ensureActivated(now = 100L)
            val second = activator.ensureActivated(now = 200L)

            assertTrue(first.performed)
            assertFalse(second.performed)
            assertEquals(1, first.scopes)
            assertEquals(1, first.commitments)
            assertEquals(0, first.deferredParticipations)
            assertEquals(1, db.planningDao().getAllScopes().count { it.kind == "STRATEGIC_ARC" })
            assertEquals(1, db.strategicPlanningScopeExtensionDao().getAll().size)
            assertEquals(1, db.planningDao().getAllCommitments().count { it.role == "FOCUS" })
            assertEquals(100L, requireNotNull(db.strategicPlanningAuthorityStateDao().get()).activatedAt)
        }
    }

    @Test
    fun `legitimate unresolved source remains deferred and does not block activation`() = runBlocking {
        withDatabase { db ->
            db.arcQuestDao().insert(sourceQuest("deferred", ArcQuestSourceType.CONTEXT, "context-missing"))

            val report = activator(db).ensureActivated(now = 100L)

            assertTrue(report.performed)
            assertEquals(1, report.scopes)
            assertEquals(0, report.commitments)
            assertEquals(1, report.deferredParticipations)
            assertEquals(1, db.strategicPlanningScopeExtensionDao().getAll().size)
            assertTrue(db.planningDao().getAllCommitments().none { it.role == "FOCUS" })
        }
    }

    @Test
    fun `integrity failure blocks activation and leaves canonical Strategic state absent`() = runBlocking {
        withDatabase { db ->
            db.arcQuestDao().insert(manualQuest().copy(sourceType = "UNKNOWN"))

            assertFailsActivation { activator(db).ensureActivated() }

            assertInactiveAndEmpty(db)
        }
    }

    @Test
    fun `semantic preparation failure rolls back before planning writes`() = runBlocking {
        withDatabase { db ->
            db.arcQuestDao().insert(manualQuest())
            val tactical = tacticalPrerequisite()
            val questBridge = mockk<QuestOrientationBridge>()
            coEvery { questBridge.ensureCutOver() } throws IllegalStateException("forced semantic failure")

            assertFailsActivation {
                CanonicalStrategicPlanningAuthorityActivator(db, questBridge, tactical).ensureActivated()
            }

            assertInactiveAndEmpty(db)
        }
    }

    @Test
    fun `real QUEST preparation rolls back when later Strategic preflight fails`() = runBlocking {
        withDatabase { db ->
            val quest = manualQuest()
            db.arcQuestDao().insert(quest)
            db.planningDao().upsertScope(expectedScope(quest).copy(title = "Divergent").toPlanningScopeEntity())

            assertFailsActivation {
                CanonicalStrategicPlanningAuthorityActivator(
                    database = db,
                    questOrientationBridge = realQuestBridge(db),
                    tacticalPlanningAuthorityActivator = tacticalPrerequisite(),
                ).ensureActivated(now = 100L)
            }

            assertNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.ARC_QUEST.name,
                    quest.id,
                ),
            )
            assertNull(db.orientationDao().getManagedSubject(manualSubjectId(quest.id)))
            assertNull(db.strategicPlanningAuthorityStateDao().get())
            assertEquals("Divergent", requireNotNull(db.planningDao().getScope(expectedScope(quest).id.value)).title)
            assertTrue(db.strategicPlanningScopeExtensionDao().getAll().isEmpty())
            assertTrue(db.planningDao().getAllCommitments().isEmpty())
        }
    }

    @Test
    fun `parity failure after materialization rolls back every Strategic write`() = runBlocking {
        withDatabase { db ->
            seedManualQuest(db)
            db.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER add_extra_strategic_commitment
                AFTER INSERT ON planning_commitments
                WHEN NEW.provenanceSourceType = 'ARC_QUEST'
                BEGIN
                    INSERT INTO planning_commitments(
                        id, scopeId, orientationId, role, commitmentOrder, priorityLevel,
                        status, provenanceKind, provenanceSourceType, provenanceSourceId,
                        createdAt, updatedAt, syncedAt, version, isDeleted
                    ) VALUES(
                        'extra-' || NEW.id, NEW.scopeId, NEW.orientationId, NEW.role,
                        NEW.commitmentOrder + 1, NEW.priorityLevel, NEW.status,
                        NEW.provenanceKind, NEW.provenanceSourceType, 'extra',
                        NEW.createdAt, NEW.updatedAt, NEW.syncedAt, NEW.version, NEW.isDeleted
                    );
                END
                """.trimIndent(),
            )

            assertFailsActivation { activator(db).ensureActivated(now = 100L) }

            assertInactiveAndEmpty(db)
        }
    }

    @Test
    fun `marker insert failure rolls back materialized Strategic planning`() = runBlocking {
        withDatabase { db ->
            seedManualQuest(db)
            db.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_strategic_planning_marker
                BEFORE INSERT ON strategic_planning_authority_state
                BEGIN
                    SELECT RAISE(ABORT, 'forced Strategic marker failure');
                END
                """.trimIndent(),
            )

            assertFailsActivation { activator(db).ensureActivated(now = 100L) }

            assertInactiveAndEmpty(db)
        }
    }

    @Test
    fun `equivalent preexisting deterministic rows are accepted without overwrite`() = runBlocking {
        withDatabase { db ->
            val quest = seedManualQuest(db)
            val plan = expectedPlan(quest)
            db.planningDao().upsertScopes(plan.scopes.map { it.toPlanningScopeEntity() })
            db.strategicPlanningScopeExtensionDao().upsertAll(plan.extensions.map { it.toEntity() })
            db.planningDao().upsertCommitments(plan.commitments.map { it.toPlanningCommitmentEntity() })

            val report = activator(db).ensureActivated(now = 100L)

            assertTrue(report.performed)
            assertEquals(100L, requireNotNull(db.strategicPlanningAuthorityStateDao().get()).activatedAt)
            assertEquals(plan.scopes.map { it.id.value }, db.planningDao().getAllScopes().map { it.id })
        }
    }

    @Test
    fun `divergent deterministic scope or extension fails closed`() = runBlocking {
        withDatabase { db ->
            val quest = seedManualQuest(db)
            val plan = expectedPlan(quest)
            db.planningDao().upsertScope(plan.scopes.single().toPlanningScopeEntity())
            db.strategicPlanningScopeExtensionDao().upsert(
                plan.extensions.single().copy(legacyArcKey = "other-key").toEntity(),
            )

            assertFailsActivation { activator(db).ensureActivated(now = 100L) }

            assertNull(db.strategicPlanningAuthorityStateDao().get())
            assertEquals("other-key", db.strategicPlanningScopeExtensionDao().getAll().single().legacyArcKey)
            assertTrue(db.planningDao().getAllCommitments().isEmpty())
        }
    }

    @Test
    fun `divergent deterministic commitment id fails closed`() = runBlocking {
        withDatabase { db ->
            val quest = seedManualQuest(db)
            val plan = expectedPlan(quest)
            db.planningDao().upsertScope(plan.scopes.single().toPlanningScopeEntity())
            db.strategicPlanningScopeExtensionDao().upsert(plan.extensions.single().toEntity())
            db.planningDao().upsertCommitment(
                plan.commitments.single().copy(order = 999L).toPlanningCommitmentEntity(),
            )

            assertFailsActivation { activator(db).ensureActivated(now = 100L) }

            assertNull(db.strategicPlanningAuthorityStateDao().get())
            assertEquals(999L, db.planningDao().getAllCommitments().single().commitmentOrder)
        }
    }

    @Test
    fun `unexpected Strategic scope extension and FOCUS commitment each fail set exact preflight`() = runBlocking {
        withDatabase { db ->
            val quest = seedManualQuest(db)
            val expected = expectedPlan(quest)
            val extraScope =
                PlanningScope(
                    id = PlanningScopeId("extra-strategic"),
                    kind = PlanningScopeKind.STRATEGIC_ARC,
                    lifecycle = PlanningScopeLifecycle.ACTIVE,
                    title = null,
                    startsAt = null,
                    endsAt = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    version = 1L,
                    isDeleted = false,
                )
            db.planningDao().upsertScope(extraScope.toPlanningScopeEntity())

            assertFailsActivation { activator(db).ensureActivated(now = 100L) }
            assertNull(db.strategicPlanningAuthorityStateDao().get())

            db.openHelper.writableDatabase.execSQL("DELETE FROM planning_scopes WHERE id = 'extra-strategic'")
            db.planningDao().upsertScope(expected.scopes.single().toPlanningScopeEntity())
            db.strategicPlanningScopeExtensionDao().upsert(expected.extensions.single().toEntity())
            val extraOrientation = "extra-orientation"
            seedOrientation(db, extraOrientation, OrientationKind.INTENT)
            db.planningDao().upsertCommitment(
                extraFocus(expected.scopes.single().id, extraOrientation).toPlanningCommitmentEntity(),
            )

            assertFailsActivation { activator(db).ensureActivated(now = 100L) }
            assertNull(db.strategicPlanningAuthorityStateDao().get())
        }
    }

    @Test
    fun `marker present rerun admits canonical evolution but rejects extra owned scope`() = runBlocking {
        withDatabase { db ->
            val quest = seedManualQuest(db)
            val activator = activator(db)
            activator.ensureActivated(now = 100L)
            val scope = expectedScope(quest)
            db.planningDao().upsertScope(scope.copy(title = "Canonical evolution").toPlanningScopeEntity())

            val admitted = activator.ensureActivated(now = 150L)
            assertFalse(admitted.performed)

            db.planningDao().upsertScope(
                scope.copy(id = PlanningScopeId("unexpected-strategic-scope")).toPlanningScopeEntity(),
            )

            assertFailsActivation { activator.ensureActivated(now = 200L) }

            assertEquals(100L, requireNotNull(db.strategicPlanningAuthorityStateDao().get()).activatedAt)
            assertEquals(
                "Canonical evolution",
                requireNotNull(db.planningDao().getScope(scope.id.value)).title,
            )
        }
    }

    @Test
    fun `canonical ingress establishes local marker without resurrecting stale ArcQuest planning facts`() = runBlocking {
        withDatabase { db ->
            seedManualQuest(db)
            val activator = activator(db)
            activator.ensureActivated(now = 100L)
            val commitment =
                db.planningDao().getAllCommitments().single { it.role == PlanningRoleCode.FOCUS.value }
            db.planningDao().upsertCommitment(
                commitment.copy(
                    commitmentOrder = 88L,
                    status = "PAUSED",
                    version = commitment.version + 1L,
                ),
            )
            db.openHelper.writableDatabase.execSQL("DELETE FROM strategic_planning_authority_state")

            val report = activator.admitCanonicalIngress(now = 200L)

            assertTrue(report.performed)
            val canonical = requireNotNull(db.planningDao().getCommitment(commitment.id))
            assertEquals(88L, canonical.commitmentOrder)
            assertEquals("PAUSED", canonical.status)
            assertEquals(200L, requireNotNull(db.strategicPlanningAuthorityStateDao().get()).activatedAt)
        }
    }

    @Test
    fun `compatibility projection preserves deleted FOCUS and deleted Orientation`() =
        runBlocking {
            withDatabase { db ->
                val (quest, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = true,
                        focusDeleted = true,
                        subjectDeleted = true,
                    )
                val runtime = projectionAdapter(db, authority)

                runtime.projectCompatibilityRows()
                val first = requireNotNull(db.arcQuestDao().getById(quest.id))
                runtime.projectCompatibilityRows()
                val second = requireNotNull(db.arcQuestDao().getById(quest.id))

                val subjectId = manualSubjectId(quest.id)
                val mapping =
                    requireNotNull(
                        db.orientationDao().getLegacyMapping(
                            LegacyOrientationSourceType.ARC_QUEST.name,
                            quest.id,
                        )
                    )
                val subject =
                    requireNotNull(
                        db.orientationDao().getManagedSubject(subjectId)
                    )
                val focus =
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value }

                assertEquals(first, second)
                assertTrue(second.isDeleted)
                assertTrue(mapping.isDeleted)
                assertTrue(subject.isDeleted)
                assertTrue(focus.isDeleted)
                assertEquals("DROPPED", focus.status)
                assertEquals(subject.title, second.title)
            }
        }

    @Test
    fun `compatibility projection repairs stale live ArcQuest carrier from deleted FOCUS`() =
        runBlocking {
            withDatabase { db ->
                val (quest, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = false,
                        focusDeleted = true,
                        subjectDeleted = true,
                    )
                val runtime = projectionAdapter(db, authority)

                assertFalse(
                    requireNotNull(db.arcQuestDao().getById(quest.id)).isDeleted
                )

                runtime.projectCompatibilityRows()

                val projected =
                    requireNotNull(db.arcQuestDao().getById(quest.id))
                assertTrue(projected.isDeleted)
                assertTrue(
                    requireNotNull(
                        db.orientationDao().getManagedSubject(
                            manualSubjectId(quest.id)
                        )
                    ).isDeleted
                )
                assertTrue(
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value }
                        .isDeleted
                )
            }
        }

    @Test
    fun `live FOCUS cannot project from tombstoned Orientation`() =
        runBlocking {
            withDatabase { db ->
                val (quest, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = false,
                        focusDeleted = false,
                        subjectDeleted = true,
                    )

                val before =
                    requireNotNull(db.arcQuestDao().getById(quest.id))

                val failure =
                    runCatching {
                        projectionAdapter(db, authority)
                            .projectCompatibilityRows()
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals(
                    before,
                    requireNotNull(db.arcQuestDao().getById(quest.id)),
                )
                assertFalse(
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value }
                        .isDeleted
                )
            }
        }

    @Test
    fun `marker rerun admits aligned deleted Strategic FOCUS without resurrection`() =
        runBlocking {
            withDatabase { db ->
                val (quest, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = true,
                        focusDeleted = true,
                        subjectDeleted = true,
                    )

                val markerBefore =
                    requireNotNull(db.strategicPlanningAuthorityStateDao().get())
                val focusBefore =
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value }

                val rerun = authority.ensureActivated(now = 200L)
                val ingress = authority.admitCanonicalIngress(now = 300L)
                authority.revalidateIfActivated()

                assertFalse(rerun.performed)
                assertFalse(ingress.performed)
                assertEquals(
                    markerBefore,
                    db.strategicPlanningAuthorityStateDao().get(),
                )
                assertEquals(
                    focusBefore,
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value },
                )
                assertTrue(
                    requireNotNull(db.arcQuestDao().getById(quest.id)).isDeleted
                )
                assertTrue(
                    requireNotNull(
                        db.orientationDao().getManagedSubject(
                            manualSubjectId(quest.id)
                        )
                    ).isDeleted
                )
            }
        }

    @Test
    fun `marker rerun rejects live FOCUS for deleted Strategic source`() =
        runBlocking {
            withDatabase { db ->
                val (_, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = true,
                        focusDeleted = false,
                        subjectDeleted = true,
                    )

                val before =
                    requireNotNull(db.strategicPlanningAuthorityStateDao().get())

                val failure =
                    runCatching {
                        authority.ensureActivated(now = 200L)
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals(
                    before,
                    db.strategicPlanningAuthorityStateDao().get(),
                )
            }
        }

    @Test
    fun `marker rerun rejects deleted FOCUS for live Strategic source`() =
        runBlocking {
            withDatabase { db ->
                val (_, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = false,
                        focusDeleted = true,
                        subjectDeleted = false,
                    )

                val before =
                    requireNotNull(db.strategicPlanningAuthorityStateDao().get())

                val failure =
                    runCatching {
                        authority.ensureActivated(now = 200L)
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals(
                    before,
                    db.strategicPlanningAuthorityStateDao().get(),
                )
            }
        }

    @Test
    fun `marker rerun rejects deleted FOCUS without DROPPED status`() =
        runBlocking {
            withDatabase { db ->
                val (_, authority) =
                    seedProjectionScenario(
                        db,
                        questDeleted = true,
                        focusDeleted = true,
                        subjectDeleted = true,
                    )

                val focus =
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value }

                db.planningDao().upsertCommitment(
                    focus.copy(status = "ACTIVE")
                )

                val failure =
                    runCatching {
                        authority.ensureActivated(now = 200L)
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(
                    db.planningDao().getAllCommitments()
                        .single { it.role == PlanningRoleCode.FOCUS.value }
                        .isDeleted
                )
            }
        }

    private suspend fun seedProjectionScenario(
        db: AppDatabase,
        questDeleted: Boolean,
        focusDeleted: Boolean,
        subjectDeleted: Boolean,
    ): Pair<ArcQuestEntity, CanonicalStrategicPlanningAuthorityActivator> {
        val quest = seedManualQuest(db)
        val authority = activator(db)
        authority.ensureActivated(now = 100L)

        if (focusDeleted) {
            val focus =
                db.planningDao().getAllCommitments()
                    .single { it.role == PlanningRoleCode.FOCUS.value }

            db.planningDao().upsertCommitment(
                focus.copy(
                    status = "DROPPED",
                    isDeleted = true,
                )
            )
        }

        if (subjectDeleted) {
            val subjectId = manualSubjectId(quest.id)
            val subject =
                requireNotNull(
                    db.orientationDao().getManagedSubject(subjectId)
                )

            db.orientationDao().upsertManagedSubjects(
                listOf(subject.copy(isDeleted = true))
            )
        }

        if (questDeleted) {
            db.arcQuestDao().update(quest.copy(isDeleted = true))

            val mapping =
                requireNotNull(
                    db.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.ARC_QUEST.name,
                        quest.id,
                    )
                )

            db.orientationDao().upsertLegacyMappings(
                listOf(mapping.copy(isDeleted = true))
            )
        }

        return quest to authority
    }

    private fun projectionAdapter(
        db: AppDatabase,
        authority: CanonicalStrategicPlanningAuthorityActivator,
    ): CanonicalStrategicPlanningAdapter =
        CanonicalStrategicPlanningAdapter(
            db,
            authority,
            mockk(),
            mockk(),
            mockk(),
        )

    private fun activator(db: AppDatabase): CanonicalStrategicPlanningAuthorityActivator {
        val bridge = mockk<QuestOrientationBridge>()
        coEvery { bridge.ensureCutOver() } returns Unit
        return CanonicalStrategicPlanningAuthorityActivator(db, bridge, tacticalPrerequisite())
    }

    private fun tacticalPrerequisite(): CanonicalTacticalPlanningAuthorityActivator {
        val tactical = mockk<CanonicalTacticalPlanningAuthorityActivator>()
        coEvery { tactical.ensureActivated(any()) } returns TacticalPlanningActivationReport(false, 0, 0, 0)
        return tactical
    }

    private fun realQuestBridge(db: AppDatabase): QuestOrientationBridge {
        val dayThemeBootstrapper =
            CanonicalDayThemeBootstrapper(db, db.dayThemeDocumentDao(), db.canonicalDayThemeDao())
        val bootstrapper =
            CanonicalOrientationBootstrapper(
                database = db,
                orientationDao = db.orientationDao(),
                goalDao = db.goalDao(),
                mainBeaconDao = db.mainBeaconDao(),
                arcQuestDao = db.arcQuestDao(),
                canonicalDayThemeDao = db.canonicalDayThemeDao(),
                canonicalDayThemeBootstrapper = dayThemeBootstrapper,
            )
        return QuestOrientationBridge(db.orientationDao(), bootstrapper)
    }

    private suspend fun seedManualQuest(db: AppDatabase): ArcQuestEntity {
        val quest = manualQuest()
        db.arcQuestDao().insert(quest)
        val subjectId = manualSubjectId(quest.id)
        seedOrientation(db, subjectId, OrientationKind.QUEST)
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "ARC_QUEST:${quest.id}",
                    sourceType = LegacyOrientationSourceType.ARC_QUEST.name,
                    sourceId = quest.id,
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        return quest
    }

    private suspend fun seedOrientation(db: AppDatabase, id: String, kind: OrientationKind) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = id,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.orientationDao().upsertOrientations(
            listOf(OrientationEntity(id, kind.name, null, ValueOrigin.UNSET.name)),
        )
    }

    private fun expectedPlan(quest: ArcQuestEntity): StrategicPlanningMapping {
        val subjectId = manualSubjectId(quest.id)
        return mapCanonicalStrategicPlanning(
            arcQuests = listOf(quest),
            snapshot =
                StrategicPlanningResolutionSnapshot(
                    mappings =
                        listOf(
                            LegacySubjectMappingEntity(
                                id = "ARC_QUEST:${quest.id}",
                                sourceType = LegacyOrientationSourceType.ARC_QUEST.name,
                                sourceId = quest.id,
                                subjectId = subjectId,
                                migrationVersion = 1,
                                state = LegacySubjectMappingState.CUT_OVER.name,
                                createdAt = 1L,
                                updatedAt = 1L,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1L,
                            ),
                        ),
                    subjects = listOf(subject(subjectId)),
                    orientations = listOf(OrientationEntity(subjectId, "QUEST", null, "UNSET")),
                    tactical =
                        com.romankozak.forwardappmobile.data.planning.tactical
                            .TacticalMissionOrientationResolutionSnapshot(
                                missions = emptyList(),
                                backlogEntries = emptyList(),
                                arcQuests = listOf(quest),
                                mappings = emptyList(),
                                subjects = emptyList(),
                                orientations = emptyList(),
                            ),
                ),
        )
    }

    private fun expectedScope(quest: ArcQuestEntity) = expectedPlan(quest).scopes.single()

    private fun extraFocus(scopeId: PlanningScopeId, orientationId: String) =
        PlanningCommitment(
            id = PlanningCommitmentId("extra-focus"),
            scopeId = scopeId,
            orientationId = orientationId,
            role = PlanningRoleCode.FOCUS,
            order = 1L,
            priorityLevel = null,
            status = com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus.ACTIVE,
            provenance = PlanningProvenance(PlanningProvenanceKind.MIGRATED, ARC_QUEST_PROVENANCE_SOURCE, "extra"),
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )

    private fun manualQuest() =
        ArcQuestEntity(
            id = "quest-1",
            arcKey = " Exact/Arc Key ",
            title = "Quest",
            description = "Description",
            sourceType = ArcQuestSourceType.MANUAL.name,
            status = "ACTIVE",
            order = 7L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 21L,
            version = 2L,
            isDeleted = false,
        )

    private fun sourceQuest(id: String, type: ArcQuestSourceType, sourceId: String) =
        manualQuest().copy(id = id, sourceType = type.name, sourceId = sourceId)

    private fun manualSubjectId(questId: String): String =
        LegacySubjectUuid.resolve(LegacySubjectRef(LegacyOrientationSourceType.ARC_QUEST, questId))

    private fun subject(id: String) =
        ManagedSubjectEntity(
            id = id,
            subjectType = ManagedSubjectType.ORIENTATION.name,
            title = id,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private suspend fun assertInactiveAndEmpty(db: AppDatabase) {
        assertNull(db.strategicPlanningAuthorityStateDao().get())
        assertTrue(db.planningDao().getAllScopes().none { it.kind == "STRATEGIC_ARC" })
        assertTrue(db.strategicPlanningScopeExtensionDao().getAll().isEmpty())
        assertTrue(db.planningDao().getAllCommitments().none { it.role == "FOCUS" })
    }

    private suspend fun assertFailsActivation(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Activation should fail", failure != null)
    }

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }
}
