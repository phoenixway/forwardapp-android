package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ExpectedSpanValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import android.content.Context as AndroidContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalContextMigrationRepositoryRoomTest {
    private val androidContext: AndroidContext = ApplicationProvider.getApplicationContext()

    @Test
    fun `preflight existing Orientation is write free`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val orientationId =
                createExistingOrientation(
                    database = database,
                    id = "preflight-existing-orientation",
                    kind = OrientationKind.DIRECTION,
                    now = 15L,
                )
            val repository = repository(database, bootstrapper)

            val contextBefore =
                requireNotNull(database.contextDao().getContextById("leaf"))
            val workspacesBefore =
                database.workspaceDao().getAll()
                    .sortedBy { it.id }
            val capabilitiesBefore =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .sortedBy { it.id }
            val subjectsBefore =
                database.orientationDao().getAllManagedSubjects()
                    .sortedBy { it.id }
            val orientationsBefore =
                database.orientationDao().getAllOrientations()
                    .sortedBy { it.subjectId }
            val assessmentsBefore =
                database.orientationDao().getAllAssessments()
                    .sortedBy { it.orientationId }
            val revisionsBefore =
                database.orientationDao().getAllAssessmentRevisions()
                    .sortedBy { it.id }
            val mappingsBefore =
                database.orientationDao().getAllLegacyMappings()
                    .sortedBy { it.id }
            val bindingsBefore =
                database.orientationDao().getAllWorkspaceBindings()
                    .sortedBy { it.id }

            val result =
                repository.preflightContextMigration(
                    contextId = "leaf",
                    target =
                        ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(
                            orientationId = orientationId,
                        ),
                )

            assertTrue(result.ready)
            assertNull(result.reason)

            assertEquals(
                contextBefore,
                database.contextDao().getContextById("leaf"),
            )
            assertEquals(
                workspacesBefore,
                database.workspaceDao().getAll()
                    .sortedBy { it.id },
            )
            assertEquals(
                capabilitiesBefore,
                database.orientationDao().getAllWorkspaceCapabilities()
                    .sortedBy { it.id },
            )
            assertEquals(
                subjectsBefore,
                database.orientationDao().getAllManagedSubjects()
                    .sortedBy { it.id },
            )
            assertEquals(
                orientationsBefore,
                database.orientationDao().getAllOrientations()
                    .sortedBy { it.subjectId },
            )
            assertEquals(
                assessmentsBefore,
                database.orientationDao().getAllAssessments()
                    .sortedBy { it.orientationId },
            )
            assertEquals(
                revisionsBefore,
                database.orientationDao().getAllAssessmentRevisions()
                    .sortedBy { it.id },
            )
            assertEquals(
                mappingsBefore,
                database.orientationDao().getAllLegacyMappings()
                    .sortedBy { it.id },
            )
            assertEquals(
                bindingsBefore,
                database.orientationDao().getAllWorkspaceBindings()
                    .sortedBy { it.id },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `preflight and migrate reject the same non-leaf target`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("parent"))
            database.contextDao().insert(
                context(
                    id = "child",
                    parentId = "parent",
                ),
            )
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)
            val target =
                ContextMigrationTarget.NewAspectWithExistingWorkspace()

            val contextBefore =
                requireNotNull(database.contextDao().getContextById("parent"))
            val workspaceBefore =
                requireNotNull(database.workspaceDao().getById("parent"))

            val preflight =
                repository.preflightContextMigration(
                    contextId = "parent",
                    target = target,
                )

            assertFalse(preflight.ready)
            assertTrue(
                requireNotNull(preflight.reason)
                    .contains("requires a leaf Context"),
            )

            val failure =
                runCatching {
                    repository.migrateContext(
                        contextId = "parent",
                        target = target,
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(preflight.reason, failure?.message)

            assertEquals(
                contextBefore,
                database.contextDao().getContextById("parent"),
            )
            assertEquals(
                workspaceBefore,
                database.workspaceDao().getById("parent"),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `system Context migration rejects every target before mutation`() = runBlocking {
        val database = database()
        try {
            val systemId = SystemContexts.PERSONAL_MANAGEMENT.raw
            database.contextDao().insert(context(systemId))

            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)

            val contextBefore = database.contextDao().getContextById(systemId)
            val workspacesBefore = database.workspaceDao().getAll()
            val capabilitiesBefore = database.orientationDao().getAllWorkspaceCapabilities()
            val subjectsBefore = database.orientationDao().getAllManagedSubjects()
            val mappingsBefore = database.orientationDao().getAllLegacyMappings()
            val bindingsBefore = database.orientationDao().getAllWorkspaceBindings()

            val targets: List<ContextMigrationTarget> =
                listOf(
                    ContextMigrationTarget.WorkspaceOnly,
                    ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                    ContextMigrationTarget.NewOrientationWithExistingWorkspace(
                        kind = OrientationKind.GOAL,
                    ),
                    ContextMigrationTarget.ExistingAspectWithExistingWorkspace(
                        aspectId = "unused-aspect",
                    ),
                    ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(
                        orientationId = "unused-orientation",
                    ),
                )

            targets.forEach { target ->
                val failure =
                    runCatching {
                        repository.migrateContext(
                            contextId = systemId,
                            target = target,
                            now = 20L,
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals("System Context cannot be migrated", failure?.message)
            }

            assertEquals(contextBefore, database.contextDao().getContextById(systemId))
            assertEquals(workspacesBefore, database.workspaceDao().getAll())
            assertEquals(capabilitiesBefore, database.orientationDao().getAllWorkspaceCapabilities())
            assertEquals(subjectsBefore, database.orientationDao().getAllManagedSubjects())
            assertEquals(mappingsBefore, database.orientationDao().getAllLegacyMappings())
            assertEquals(bindingsBefore, database.orientationDao().getAllWorkspaceBindings())
        } finally {
            database.close()
        }
    }

    @Test
    fun `leaf Context cutover preserves Workspace identity and capability state`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val beforeCapabilities =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "leaf" }
                    .sortedBy { it.id }
            assertTrue(beforeCapabilities.isNotEmpty())

            val result =
                repository(database, bootstrapper).migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                    now = 20L,
                )

            assertTrue(result.changed)
            assertEquals("leaf", result.workspaceId)
            assertEquals(stableContextSubjectId("leaf"), result.subjectId)

            val retired = requireNotNull(database.contextDao().getContextById("leaf"))
            assertTrue(retired.isDeleted)
            assertEquals(1L, retired.version)

            val workspace = requireNotNull(database.workspaceDao().getById("leaf"))
            assertFalse(workspace.isDeleted)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
            assertNull(workspace.sourceContextId)

            val mapping =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "leaf",
                    ),
                )
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, mapping.state)
            assertEquals(result.subjectId, mapping.subjectId)

            val binding =
                database.orientationDao().getAllWorkspaceBindings()
                    .single {
                        !it.isDeleted &&
                            it.workspaceId == "leaf" &&
                            it.subjectId == result.subjectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
            assertTrue(binding.isPrimary)

            val afterCapabilities =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "leaf" }
                    .sortedBy { it.id }
            assertEquals(beforeCapabilities, afterCapabilities)

            val report = bootstrapper.ensureBootstrapped(now = 30L)
            assertTrue(
                report.issues.none {
                    it.contextId == "leaf" && it.code == "WORKSPACE_ID_COLLISION"
                },
            )
            assertFalse(requireNotNull(database.workspaceDao().getById("leaf")).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `ordinary cutover transfers tags to canonical Workspace ownership`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(
                context("tagged").copy(
                    tags = listOf("#Focus", " beta ", "FOCUS"),
                ),
            )
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)

            val result =
                repository.migrateContext(
                    contextId = "tagged",
                    target = ContextMigrationTarget.WorkspaceOnly,
                    now = 20L,
                )

            assertTrue(result.changed)
            assertTrue(
                requireNotNull(
                    database.contextDao().getContextById("tagged"),
                ).isDeleted,
            )

            val workspace =
                requireNotNull(database.workspaceDao().getById("tagged"))
            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                workspace.provenance,
            )
            assertNull(workspace.sourceContextId)

            val canonicalTags = CanonicalWorkspaceTagRepository(database)
            assertEquals(
                listOf("beta", "focus"),
                canonicalTags.getTags("tagged"),
            )

            // The retired Context is compatibility/history only. Mutating its
            // stale tag payload directly must not change canonical read truth.
            val retired =
                requireNotNull(database.contextDao().getContextById("tagged"))
            database.contextDao().insert(
                retired.copy(
                    tags = listOf("legacy-only"),
                    updatedAt = 30L,
                ),
            )

            assertEquals(
                listOf("beta", "focus"),
                canonicalTags.getTags("tagged"),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `retry of identical Context cutover is idempotent`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)
            val target = ContextMigrationTarget.NewAspectWithExistingWorkspace()

            val first = repository.migrateContext("leaf", target, now = 20L)
            val workspaceAfterFirst = requireNotNull(database.workspaceDao().getById("leaf"))
            val mappingAfterFirst =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "leaf",
                    ),
                )
            val bindingAfterFirst =
                database.orientationDao().getAllWorkspaceBindings()
                    .single {
                        !it.isDeleted &&
                            it.workspaceId == "leaf" &&
                            it.subjectId == first.subjectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
            val subjectAfterFirst =
                requireNotNull(
                    database.orientationDao().getManagedSubject(
                        requireNotNull(first.subjectId),
                    ),
                )

            val second = repository.migrateContext("leaf", target, now = 99L)

            assertFalse(second.changed)
            assertEquals(first.copy(changed = false), second)
            assertEquals(workspaceAfterFirst, database.workspaceDao().getById("leaf"))
            assertEquals(
                mappingAfterFirst,
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
            assertEquals(
                bindingAfterFirst,
                database.orientationDao().getAllWorkspaceBindings()
                    .single {
                        !it.isDeleted &&
                            it.workspaceId == "leaf" &&
                            it.subjectId == first.subjectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    },
            )
            assertEquals(
                subjectAfterFirst,
                database.orientationDao().getManagedSubject(requireNotNull(first.subjectId)),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `conflicting retry fails closed and preserves completed cutover`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)

            val first =
                repository.migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                    now = 20L,
                )

            val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val mappingBefore =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "leaf",
                    ),
                )
            val subjectBefore =
                requireNotNull(
                    database.orientationDao().getManagedSubject(
                        requireNotNull(first.subjectId),
                    ),
                )
            val bindingsBefore =
                database.orientationDao().getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            (it.workspaceId == "leaf" || it.subjectId == first.subjectId)
                    }

            val failure =
                runCatching {
                    repository.migrateContext(
                        contextId = "leaf",
                        target =
                            ContextMigrationTarget.NewAspectWithExistingWorkspace(
                                titleOverride = "Different target",
                            ),
                        now = 99L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(
                mappingBefore,
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
            assertEquals(
                subjectBefore,
                database.orientationDao().getManagedSubject(requireNotNull(first.subjectId)),
            )
            assertEquals(
                bindingsBefore,
                database.orientationDao().getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            (it.workspaceId == "leaf" || it.subjectId == first.subjectId)
                    },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing unmapped Aspect can adopt one Context Workspace without rewriting Aspect`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val aspects = CanonicalAspectRepository(database, database.orientationDao())
            val aspectId =
                aspects.create(
                    title = "Existing canonical Aspect",
                    description = "Keep me",
                    now = 15L,
                )
            val subjectBefore =
                requireNotNull(database.orientationDao().getManagedSubject(aspectId))
            val nodeBefore =
                requireNotNull(database.orientationDao().getAspect(aspectId))
            val capabilitiesBefore =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "leaf" }
                    .sortedBy { it.id }

            val result =
                repository(database, bootstrapper).migrateContext(
                    contextId = "leaf",
                    target =
                        ContextMigrationTarget.ExistingAspectWithExistingWorkspace(
                            aspectId = aspectId,
                        ),
                    now = 20L,
                )

            assertTrue(result.changed)
            assertEquals(aspectId, result.subjectId)
            assertEquals(subjectBefore, database.orientationDao().getManagedSubject(aspectId))
            assertEquals(nodeBefore, database.orientationDao().getAspect(aspectId))

            val mapping =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "leaf",
                    ),
                )
            assertEquals(aspectId, mapping.subjectId)
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, mapping.state)

            val workspace = requireNotNull(database.workspaceDao().getById("leaf"))
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
            assertNull(workspace.sourceContextId)
            assertFalse(workspace.isDeleted)
            assertTrue(requireNotNull(database.contextDao().getContextById("leaf")).isDeleted)

            assertEquals(
                capabilitiesBefore,
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "leaf" }
                    .sortedBy { it.id },
            )

            val binding =
                database.orientationDao().getAllWorkspaceBindings()
                    .single {
                        !it.isDeleted &&
                            it.workspaceId == "leaf" &&
                            it.subjectId == aspectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
            assertTrue(binding.isPrimary)

            val retry =
                repository(database, bootstrapper).migrateContext(
                    contextId = "leaf",
                    target =
                        ContextMigrationTarget.ExistingAspectWithExistingWorkspace(
                            aspectId = aspectId,
                        ),
                    now = 99L,
                )
            assertFalse(retry.changed)
            assertEquals(result.copy(changed = false), retry)
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing Aspect already reserved by another legacy mapping fails closed`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val aspectId =
                CanonicalAspectRepository(database, database.orientationDao())
                    .create("Reserved Aspect", now = 15L)

            database.orientationDao().upsertLegacyMappings(
                listOf(
                    LegacySubjectMappingEntity(
                        id = "reserved-mapping",
                        sourceType = LegacyOrientationSourceType.GOAL.name,
                        sourceId = "legacy-goal",
                        subjectId = aspectId,
                        migrationVersion = 1,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = 16L,
                        updatedAt = 16L,
                        syncedAt = null,
                        isDeleted = true,
                        version = 2L,
                    ),
                ),
            )

            val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val subjectBefore = requireNotNull(database.orientationDao().getManagedSubject(aspectId))

            val failure =
                runCatching {
                    repository(database, bootstrapper).migrateContext(
                        contextId = "leaf",
                        target =
                            ContextMigrationTarget.ExistingAspectWithExistingWorkspace(
                                aspectId = aspectId,
                            ),
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(subjectBefore, database.orientationDao().getManagedSubject(aspectId))
            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
            assertTrue(
                database.orientationDao().getAllWorkspaceBindings()
                    .none {
                        !it.isDeleted &&
                            it.workspaceId == "leaf" &&
                            it.subjectId == aspectId
                    },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing Aspect embodied by another Workspace fails without displacement`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            database.contextDao().insert(context("other"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val aspects = CanonicalAspectRepository(database, database.orientationDao())
            val links =
                CanonicalAspectLinksRepository(
                    database,
                    database.orientationDao(),
                    database.contextDao(),
                )
            val aspectId = aspects.create("Owned Aspect", now = 15L)
            val originalBindingId =
                links.bindCompatibilityWorkspace(
                    aspectId = aspectId,
                    contextId = "other",
                    now = 16L,
                )
            val originalBinding =
                database.orientationDao().getAllWorkspaceBindings()
                    .first { it.id == originalBindingId }

            val leafBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val leafWorkspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))

            val failure =
                runCatching {
                    repository(database, bootstrapper).migrateContext(
                        contextId = "leaf",
                        target =
                            ContextMigrationTarget.ExistingAspectWithExistingWorkspace(
                                aspectId = aspectId,
                            ),
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(leafBefore, database.contextDao().getContextById("leaf"))
            assertEquals(leafWorkspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(
                originalBinding,
                database.orientationDao().getAllWorkspaceBindings()
                    .first { it.id == originalBindingId },
            )
            assertFalse(
                database.orientationDao().getAllWorkspaceBindings()
                    .first { it.id == originalBindingId }
                    .isDeleted,
            )
            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `non-leaf Context migration fails without partial cutover`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("parent"))
            database.contextDao().insert(context("child", parentId = "parent"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val beforeParentWorkspace = requireNotNull(database.workspaceDao().getById("parent"))

            val failure =
                runCatching {
                    repository(database, bootstrapper).migrateContext(
                        contextId = "parent",
                        target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertFalse(requireNotNull(database.contextDao().getContextById("parent")).isDeleted)
            assertEquals(beforeParentWorkspace, database.workspaceDao().getById("parent"))
            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "parent",
                ),
            )
            assertNull(database.orientationDao().getManagedSubject(stableContextSubjectId("parent")))
        } finally {
            database.close()
        }
    }

    @Test
    fun `bottom up child then parent cutover preserves Workspace hierarchy`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("parent"))
            database.contextDao().insert(context("child", parentId = "parent"))

            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)

            val initialParentWorkspace =
                requireNotNull(database.workspaceDao().getById("parent"))
            val initialChildWorkspace =
                requireNotNull(database.workspaceDao().getById("child"))

            assertEquals(
                WorkspaceProvenance.CONTEXT_BACKED.name,
                initialParentWorkspace.provenance,
            )
            assertEquals(
                WorkspaceProvenance.CONTEXT_BACKED.name,
                initialChildWorkspace.provenance,
            )
            assertEquals("parent", initialChildWorkspace.parentWorkspaceId)

            val childResult =
                repository.migrateContext(
                    contextId = "child",
                    target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                    now = 20L,
                )

            assertTrue(childResult.changed)
            assertTrue(requireNotNull(database.contextDao().getContextById("child")).isDeleted)
            assertFalse(requireNotNull(database.contextDao().getContextById("parent")).isDeleted)

            val canonicalChildWorkspace =
                requireNotNull(database.workspaceDao().getById("child"))
            val stillLegacyParentWorkspace =
                requireNotNull(database.workspaceDao().getById("parent"))

            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                canonicalChildWorkspace.provenance,
            )
            assertNull(canonicalChildWorkspace.sourceContextId)
            assertEquals("parent", canonicalChildWorkspace.parentWorkspaceId)

            assertEquals(
                WorkspaceProvenance.CONTEXT_BACKED.name,
                stillLegacyParentWorkspace.provenance,
            )
            assertEquals("parent", stillLegacyParentWorkspace.sourceContextId)

            // Semantic Aspect hierarchy is intentionally independent from the
            // operational Workspace hierarchy. Context.parentId is not inferred
            // as parentAspectId by migration.
            assertNull(
                requireNotNull(
                    database.orientationDao().getAspect(requireNotNull(childResult.subjectId)),
                ).parentAspectId,
            )

            val mixedReport = bootstrapper.ensureBootstrapped(now = 30L)

            assertEquals(
                "parent",
                requireNotNull(database.workspaceDao().getById("child")).parentWorkspaceId,
            )
            assertTrue(
                mixedReport.issues.none {
                    it.contextId == "child" &&
                        it.code == "WORKSPACE_PARENT_COLLISION"
                },
            )

            // The child Context tombstone means the legacy parent is now a leaf.
            assertTrue(database.contextDao().getActiveContextsByParentId("parent").isEmpty())

            val parentResult =
                repository.migrateContext(
                    contextId = "parent",
                    target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                    now = 40L,
                )

            assertTrue(parentResult.changed)
            assertTrue(requireNotNull(database.contextDao().getContextById("parent")).isDeleted)

            val canonicalParentWorkspace =
                requireNotNull(database.workspaceDao().getById("parent"))
            val childAfterParentCutover =
                requireNotNull(database.workspaceDao().getById("child"))

            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                canonicalParentWorkspace.provenance,
            )
            assertNull(canonicalParentWorkspace.sourceContextId)
            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                childAfterParentCutover.provenance,
            )
            assertEquals("parent", childAfterParentCutover.parentWorkspaceId)

            assertNull(
                requireNotNull(
                    database.orientationDao().getAspect(requireNotNull(parentResult.subjectId)),
                ).parentAspectId,
            )

            val finalReport = bootstrapper.ensureBootstrapped(now = 50L)

            assertEquals(
                "parent",
                requireNotNull(database.workspaceDao().getById("child")).parentWorkspaceId,
            )
            assertTrue(
                finalReport.issues.none {
                    (it.contextId == "child" || it.contextId == "parent") &&
                        it.code == "WORKSPACE_PARENT_COLLISION"
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `migration leaves unrelated Context and Workspace untouched`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("first"))
            database.contextDao().insert(context("second"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val secondContextBefore = requireNotNull(database.contextDao().getContextById("second"))
            val secondWorkspaceBefore = requireNotNull(database.workspaceDao().getById("second"))
            val secondCapabilitiesBefore =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "second" }
                    .sortedBy { it.id }

            repository(database, bootstrapper).migrateContext(
                contextId = "first",
                target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                now = 20L,
            )

            assertEquals(secondContextBefore, database.contextDao().getContextById("second"))
            assertEquals(secondWorkspaceBefore, database.workspaceDao().getById("second"))
            assertEquals(
                secondCapabilitiesBefore,
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "second" }
                    .sortedBy { it.id },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `CUT_OVER marker protects pending Context-backed Workspace from bootstrap deletion`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("pending"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val aspectId =
                CanonicalAspectRepository(database, database.orientationDao())
                    .createWithId(
                        id = stableContextSubjectId("pending"),
                        title = "pending",
                        now = 15L,
                    )

            val beforeWorkspace = requireNotNull(database.workspaceDao().getById("pending"))
            val beforeCapabilities =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "pending" }
                    .sortedBy { it.id }

            database.orientationDao().upsertLegacyMappings(
                listOf(
                    LegacySubjectMappingEntity(
                        id = stableMappingId("pending"),
                        sourceType = LegacyOrientationSourceType.CONTEXT.name,
                        sourceId = "pending",
                        subjectId = aspectId,
                        migrationVersion = 1,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = 20L,
                        updatedAt = 20L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )
            val source = requireNotNull(database.contextDao().getContextById("pending"))
            database.contextDao().insert(source.softDelete(20L))

            val report = bootstrapper.ensureBootstrapped(now = 30L)

            assertEquals(beforeWorkspace, database.workspaceDao().getById("pending"))
            assertEquals(
                beforeCapabilities,
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "pending" }
                    .sortedBy { it.id },
            )
            assertTrue(
                report.issues.none {
                    it.contextId == "pending" && it.code == "WORKSPACE_ID_COLLISION"
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `new Orientation cutover preserves Workspace and creates kind aware aggregate`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("orientation-context"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val capabilitiesBefore =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "orientation-context" }
                    .sortedBy { it.id }

            val repository = repository(database, bootstrapper)
            val target =
                ContextMigrationTarget.NewOrientationWithExistingWorkspace(
                    kind = OrientationKind.ONGOING_STANDARD,
                    titleOverride = "  Reliability standard  ",
                )

            val result =
                repository.migrateContext(
                    contextId = "orientation-context",
                    target = target,
                    now = 20L,
                )

            assertTrue(result.changed)
            assertEquals("orientation-context", result.workspaceId)
            assertEquals(stableContextSubjectId("orientation-context"), result.subjectId)

            val subject =
                requireNotNull(
                database.orientationDao().getManagedSubject(requireNotNull(result.subjectId)),
                )
            assertEquals(ManagedSubjectType.ORIENTATION.name, subject.subjectType)
            assertEquals("Reliability standard", subject.title)
            assertFalse(subject.isDeleted)

            val orientation =
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == result.subjectId }
            assertEquals(OrientationKind.ONGOING_STANDARD.name, orientation.kind)

            val assessment =
                database.orientationDao().getAllAssessments()
                    .single { it.orientationId == result.subjectId }

            assertEquals(
                ExpectedSpanValue.ONGOING.name,
                assessment.expectedSpanValue,
            )
            assertEquals(
                ValueOrigin.DERIVED.name,
                assessment.expectedSpanOrigin,
            )

            assertNull(assessment.targetWindowValue)
            assertEquals(
                ValueOrigin.NOT_APPLICABLE.name,
                assessment.targetWindowOrigin,
            )

            assertNull(assessment.importanceValue)
            assertEquals(
                ValueOrigin.UNSET.name,
                assessment.importanceOrigin,
            )

            val revisions =
                database.orientationDao().getAllAssessmentRevisions()
                    .filter { it.orientationId == result.subjectId }
            assertEquals(1, revisions.size)

            val workspace =
                requireNotNull(
                    database.workspaceDao().getById("orientation-context"),
                )
            assertFalse(workspace.isDeleted)
            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                workspace.provenance,
            )
            assertNull(workspace.sourceContextId)

            val capabilitiesAfter =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "orientation-context" }
                    .sortedBy { it.id }
            assertEquals(capabilitiesBefore, capabilitiesAfter)

            val binding =
                database.orientationDao().getAllWorkspaceBindings()
                    .single {
                        !it.isDeleted &&
                            it.subjectId == result.subjectId &&
                            it.workspaceId == "orientation-context" &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
            assertTrue(binding.isPrimary)

            val mapping =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "orientation-context",
                    ),
                )
            assertEquals(result.subjectId, mapping.subjectId)
            assertEquals(
                LegacySubjectMappingState.CUT_OVER.name,
                mapping.state,
            )

            assertTrue(
                requireNotNull(
                    database.contextDao().getContextById("orientation-context"),
                ).isDeleted,
            )

            val retry =
                repository.migrateContext(
                    contextId = "orientation-context",
                    target = target,
                    now = 30L,
                )

            assertFalse(retry.changed)
            assertEquals(result.subjectId, retry.subjectId)
            assertEquals(result.mappingId, retry.mappingId)

            // Idempotent retry must not manufacture another aggregate or binding.
            assertEquals(
                1,
                database.orientationDao().getAllOrientations()
                    .count { it.subjectId == result.subjectId },
            )
            assertEquals(
                1,
                database.orientationDao().getAllAssessmentRevisions()
                    .count { it.orientationId == result.subjectId },
            )
            assertEquals(
                1,
                database.orientationDao().getAllWorkspaceBindings()
                    .count {
                        !it.isDeleted &&
                            it.subjectId == result.subjectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Orientation cutover retry with different kind fails closed`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("orientation-conflict"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val repository = repository(database, bootstrapper)

            val first =
                repository.migrateContext(
                    contextId = "orientation-conflict",
                    target =
                        ContextMigrationTarget.NewOrientationWithExistingWorkspace(
                            kind = OrientationKind.DIRECTION,
                        ),
                    now = 20L,
                )

            val failure =
                runCatching {
                    repository.migrateContext(
                        contextId = "orientation-conflict",
                        target =
                            ContextMigrationTarget.NewOrientationWithExistingWorkspace(
                                kind = OrientationKind.GOAL,
                            ),
                        now = 30L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)

            val orientation =
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == first.subjectId }
            assertEquals(
                OrientationKind.DIRECTION.name,
                orientation.kind,
            )

            val mapping =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "orientation-conflict",
                    ),
                )
            assertEquals(first.subjectId, mapping.subjectId)
            assertEquals(
                LegacySubjectMappingState.CUT_OVER.name,
                mapping.state,
            )

            val workspace =
                requireNotNull(
                    database.workspaceDao().getById("orientation-conflict"),
                )
            assertEquals(
                WorkspaceProvenance.CANONICAL_ONLY.name,
                workspace.provenance,
            )
            assertNull(workspace.sourceContextId)

            // Failed conflicting retry must not create a second Orientation.
            assertEquals(
                1,
                database.orientationDao().getAllOrientations()
                    .count { it.subjectId == first.subjectId },
            )
            assertEquals(
                1,
                database.orientationDao().getAllWorkspaceBindings()
                    .count {
                        !it.isDeleted &&
                            it.subjectId == first.subjectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing complete Orientation adoption preserves semantic aggregate and retries idempotently`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val orientationId = createExistingOrientation(database, "existing", OrientationKind.GOAL, now = 15L)
            val subjectBefore = requireNotNull(database.orientationDao().getManagedSubject(orientationId))
            val nodeBefore = database.orientationDao().getAllOrientations().single { it.subjectId == orientationId }
            val currentBefore = database.orientationDao().getAllAssessments().single { it.orientationId == orientationId }
            val revisionsBefore = database.orientationDao().getAllAssessmentRevisions().filter { it.orientationId == orientationId }
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val capabilitiesBefore = database.orientationDao().getAllWorkspaceCapabilities().filter { it.workspaceId == "leaf" }
            val repository = repository(database, bootstrapper)
            val target = ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(orientationId)

            val first = repository.migrateContext("leaf", target, now = 20L)

            assertTrue(first.changed)
            assertEquals(orientationId, first.subjectId)
            assertEquals(subjectBefore, database.orientationDao().getManagedSubject(orientationId))
            assertEquals(nodeBefore, database.orientationDao().getAllOrientations().single { it.subjectId == orientationId })
            assertEquals(currentBefore, database.orientationDao().getAllAssessments().single { it.orientationId == orientationId })
            assertEquals(revisionsBefore, database.orientationDao().getAllAssessmentRevisions().filter { it.orientationId == orientationId })
            assertEquals(capabilitiesBefore, database.orientationDao().getAllWorkspaceCapabilities().filter { it.workspaceId == "leaf" })
            val workspace = requireNotNull(database.workspaceDao().getById("leaf"))
            assertEquals(workspaceBefore.id, workspace.id)
            assertEquals(workspaceBefore.parentWorkspaceId, workspace.parentWorkspaceId)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
            assertNull(workspace.sourceContextId)
            assertTrue(requireNotNull(database.contextDao().getContextById("leaf")).isDeleted)
            assertEquals(
                LegacySubjectMappingState.CUT_OVER.name,
                requireNotNull(database.orientationDao().getLegacyMapping(LegacyOrientationSourceType.CONTEXT.name, "leaf")).state,
            )
            assertEquals(
                1,
                database.orientationDao().getAllWorkspaceBindings().count {
                    !it.isDeleted && it.workspaceId == "leaf" && it.subjectId == orientationId &&
                        it.bindingType == WorkspaceBindingType.EMBODIES.name && it.isPrimary
                },
            )

            val second = repository.migrateContext("leaf", target, now = 99L)
            assertEquals(first.copy(changed = false), second)
            assertEquals(subjectBefore, database.orientationDao().getManagedSubject(orientationId))
            assertEquals(nodeBefore, database.orientationDao().getAllOrientations().single { it.subjectId == orientationId })
            assertEquals(currentBefore, database.orientationDao().getAllAssessments().single { it.orientationId == orientationId })
            assertEquals(revisionsBefore, database.orientationDao().getAllAssessmentRevisions().filter { it.orientationId == orientationId })
            assertEquals(
                1,
                database.orientationDao().getAllWorkspaceBindings().count {
                    !it.isDeleted && it.workspaceId == "leaf" && it.subjectId == orientationId &&
                        it.bindingType == WorkspaceBindingType.EMBODIES.name
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing Orientation reserved by any legacy mapping including tombstone fails closed`() = runBlocking {
        listOf(false, true).forEach { tombstoned ->
            val database = database()
            try {
                database.contextDao().insert(context("leaf"))
                val bootstrapper = bootstrapper(database)
                bootstrapper.ensureBootstrapped(now = 10L)
                val orientationId = createExistingOrientation(database, "existing", OrientationKind.GOAL, now = 15L)
                database.orientationDao().upsertLegacyMappings(
                    listOf(
                        LegacySubjectMappingEntity(
                            id = "reserved-$tombstoned",
                            sourceType = LegacyOrientationSourceType.GOAL.name,
                            sourceId = "goal-$tombstoned",
                            subjectId = orientationId,
                            migrationVersion = 1,
                            state = LegacySubjectMappingState.CUT_OVER.name,
                            createdAt = 16L,
                            updatedAt = 16L,
                            syncedAt = null,
                            isDeleted = tombstoned,
                            version = 1L,
                        ),
                    ),
                )
                val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
                val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
                val bindingsBefore = database.orientationDao().getAllWorkspaceBindings()

                val failure = runCatching {
                    repository(database, bootstrapper).migrateContext(
                        "leaf",
                        ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(orientationId),
                        now = 20L,
                    )
                }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
                assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
                assertEquals(bindingsBefore, database.orientationDao().getAllWorkspaceBindings())
                assertNull(database.orientationDao().getLegacyMapping(LegacyOrientationSourceType.CONTEXT.name, "leaf"))
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun `existing Orientation embodied by another Workspace fails without displacement`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            database.contextDao().insert(context("other"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val orientationId = createExistingOrientation(database, "existing", OrientationKind.GOAL, now = 15L)
            val graph = CanonicalOrientationGraphRepository(database, database.orientationDao(), database.workspaceDao())
            val originalBindingId = graph.bindExistingPrimaryEmbodiment(orientationId, "other", now = 16L)
            val originalBinding = database.orientationDao().getAllWorkspaceBindings().single { it.id == originalBindingId }
            val leafBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))

            val failure = runCatching {
                repository(database, bootstrapper).migrateContext(
                    "leaf",
                    ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(orientationId),
                    now = 20L,
                )
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(leafBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(originalBinding, database.orientationDao().getAllWorkspaceBindings().single { it.id == originalBindingId })
            assertFalse(database.orientationDao().getAllWorkspaceBindings().single { it.id == originalBindingId }.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `incomplete existing Orientation aggregate fails before cutover mutation`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val orientationId = createExistingOrientation(database, "existing", OrientationKind.GOAL, now = 15L)
            val current = database.orientationDao().getAllAssessments().single { it.orientationId == orientationId }
            database.orientationDao().upsertAssessments(listOf(current.copy(isDeleted = true, updatedAt = 16L, version = 2L)))
            val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val bindingsBefore = database.orientationDao().getAllWorkspaceBindings()

            val failure = runCatching {
                repository(database, bootstrapper).migrateContext(
                    "leaf",
                    ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(orientationId),
                    now = 20L,
                )
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(bindingsBefore, database.orientationDao().getAllWorkspaceBindings())
            assertNull(database.orientationDao().getLegacyMapping(LegacyOrientationSourceType.CONTEXT.name, "leaf"))
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing Orientation adoption retry for another subject fails closed`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val firstOrientation = createExistingOrientation(database, "orientation-a", OrientationKind.GOAL, now = 15L)
            val secondOrientation = createExistingOrientation(database, "orientation-b", OrientationKind.DIRECTION, now = 16L)
            val repository = repository(database, bootstrapper)
            val first = repository.migrateContext(
                "leaf",
                ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(firstOrientation),
                now = 20L,
            )
            val mappingBefore = requireNotNull(database.orientationDao().getLegacyMapping(LegacyOrientationSourceType.CONTEXT.name, "leaf"))
            val firstBindingBefore = database.orientationDao().getAllWorkspaceBindings().single {
                !it.isDeleted && it.workspaceId == "leaf" && it.subjectId == firstOrientation
            }

            val failure = runCatching {
                repository.migrateContext(
                    "leaf",
                    ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(secondOrientation),
                    now = 30L,
                )
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(firstOrientation, first.subjectId)
            assertEquals(mappingBefore, database.orientationDao().getLegacyMapping(LegacyOrientationSourceType.CONTEXT.name, "leaf"))
            assertEquals(firstBindingBefore, database.orientationDao().getAllWorkspaceBindings().single { it.id == firstBindingBefore.id })
            assertTrue(requireNotNull(database.contextDao().getContextById("leaf")).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace-only cutover preserves the existing operational Workspace`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val capabilitiesBefore =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "leaf" }
                    .sortedBy { it.id }

            val result =
                repository(database, bootstrapper).migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.WorkspaceOnly,
                    now = 20L,
                )

            assertTrue(result.changed)
            assertNull(result.subjectId)
            assertNull(result.mappingId)
            assertTrue(requireNotNull(database.contextDao().getContextById("leaf")).isDeleted)
            val workspace = requireNotNull(database.workspaceDao().getById("leaf"))
            assertEquals(workspaceBefore.id, workspace.id)
            assertEquals(workspaceBefore.parentWorkspaceId, workspace.parentWorkspaceId)
            assertFalse(workspace.isDeleted)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
            assertNull(workspace.sourceContextId)
            assertEquals(
                capabilitiesBefore,
                database.orientationDao().getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == "leaf" }
                    .sortedBy { it.id },
            )
            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
            assertTrue(
                database.orientationDao().getAllWorkspaceBindings().none {
                    !it.isDeleted &&
                        it.workspaceId == "leaf" &&
                        it.bindingType == WorkspaceBindingType.EMBODIES.name
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace-only cutover materializes Context presentation without replacing Workspace overrides`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(
                context("leaf").copy(
                    name = "Context name",
                    description = "Context description",
                ),
            )
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)

            val workspaceBefore =
                requireNotNull(database.workspaceDao().getById("leaf")).copy(
                    nameOverride = "Workspace name",
                    descriptionOverride = null,
                )
            database.workspaceDao().upsert(listOf(workspaceBefore))

            repository(database, bootstrapper).migrateContext(
                contextId = "leaf",
                target = ContextMigrationTarget.WorkspaceOnly,
                now = 20L,
            )

            val workspace = requireNotNull(database.workspaceDao().getById("leaf"))
            assertEquals("Workspace name", workspace.nameOverride)
            assertEquals("Context description", workspace.descriptionOverride)
            assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
            assertNull(workspace.sourceContextId)
            assertTrue(requireNotNull(database.contextDao().getContextById("leaf")).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace-only cutover retry accepts only its completed state`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)
            val first =
                repository.migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.WorkspaceOnly,
                    now = 20L,
                )
            val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val capabilitiesBefore = database.orientationDao().getAllWorkspaceCapabilities()
            val bindingsBefore = database.orientationDao().getAllWorkspaceBindings()

            val second =
                repository.migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.WorkspaceOnly,
                    now = 99L,
                )

            assertEquals(first.copy(changed = false), second)
            assertNull(second.subjectId)
            assertNull(second.mappingId)
            assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(capabilitiesBefore, database.orientationDao().getAllWorkspaceCapabilities())
            assertEquals(bindingsBefore, database.orientationDao().getAllWorkspaceBindings())
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace-only retry tolerates later canonical embodiment`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)

            val first =
                repository.migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.WorkspaceOnly,
                    now = 20L,
                )

            val orientationId =
                createExistingOrientation(
                    database = database,
                    id = "later-orientation",
                    kind = OrientationKind.GOAL,
                    now = 25L,
                )
            val graph =
                CanonicalOrientationGraphRepository(
                    database = database,
                    dao = database.orientationDao(),
                    workspaceDao = database.workspaceDao(),
                )
            val bindingId =
                graph.bindExistingPrimaryEmbodiment(
                    subjectId = orientationId,
                    workspaceId = "leaf",
                    now = 26L,
                )

            val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val bindingBefore =
                database.orientationDao().getAllWorkspaceBindings()
                    .single { it.id == bindingId }

            val second =
                repository.migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.WorkspaceOnly,
                    now = 30L,
                )

            assertEquals(first.copy(changed = false), second)
            assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(
                bindingBefore,
                database.orientationDao().getAllWorkspaceBindings()
                    .single { it.id == bindingId },
            )
            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace bootstrap does not resurrect Workspace-only Context ownership`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            repository(database, bootstrapper).migrateContext(
                contextId = "leaf",
                target = ContextMigrationTarget.WorkspaceOnly,
                now = 20L,
            )
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))
            val capabilitiesBefore = database.orientationDao().getAllWorkspaceCapabilities()

            bootstrapper.ensureBootstrapped(now = 30L)

            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(
                capabilitiesBefore,
                database.orientationDao().getAllWorkspaceCapabilities(),
            )
            assertTrue(
                database.workspaceDao().getAll().none {
                    it.id == "leaf" &&
                        it.provenance == WorkspaceProvenance.CONTEXT_BACKED.name
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `semantic cutover cannot be reinterpreted as Workspace-only`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val repository = repository(database, bootstrapper)
            val semantic =
                repository.migrateContext(
                    contextId = "leaf",
                    target = ContextMigrationTarget.NewAspectWithExistingWorkspace(),
                    now = 20L,
                )
            val mappingBefore =
                requireNotNull(
                    database.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        "leaf",
                    ),
                )
            val bindingBefore =
                database.orientationDao().getAllWorkspaceBindings().single {
                    !it.isDeleted &&
                        it.workspaceId == "leaf" &&
                        it.subjectId == semantic.subjectId &&
                        it.bindingType == WorkspaceBindingType.EMBODIES.name
                }

            val failure =
                runCatching {
                    repository.migrateContext(
                        contextId = "leaf",
                        target = ContextMigrationTarget.WorkspaceOnly,
                        now = 30L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(
                mappingBefore,
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
            assertEquals(
                bindingBefore,
                database.orientationDao().getAllWorkspaceBindings().single { it.id == bindingBefore.id },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `existing embodiment blocks Workspace-only cutover without displacement`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val orientationId =
                createExistingOrientation(database, "existing", OrientationKind.GOAL, now = 15L)
            val graph =
                CanonicalOrientationGraphRepository(
                    database,
                    database.orientationDao(),
                    database.workspaceDao(),
                )
            val bindingId = graph.bindExistingPrimaryEmbodiment(orientationId, "leaf", now = 16L)
            val bindingBefore =
                database.orientationDao().getAllWorkspaceBindings().single { it.id == bindingId }
            val contextBefore = requireNotNull(database.contextDao().getContextById("leaf"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("leaf"))

            val failure =
                runCatching {
                    repository(database, bootstrapper).migrateContext(
                        contextId = "leaf",
                        target = ContextMigrationTarget.WorkspaceOnly,
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(contextBefore, database.contextDao().getContextById("leaf"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("leaf"))
            assertEquals(
                bindingBefore,
                database.orientationDao().getAllWorkspaceBindings().single { it.id == bindingId },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `non-leaf Context blocks Workspace-only cutover`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("parent"))
            database.contextDao().insert(context("child", parentId = "parent"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val contextBefore = requireNotNull(database.contextDao().getContextById("parent"))
            val workspaceBefore = requireNotNull(database.workspaceDao().getById("parent"))

            val failure =
                runCatching {
                    repository(database, bootstrapper).migrateContext(
                        contextId = "parent",
                        target = ContextMigrationTarget.WorkspaceOnly,
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(contextBefore, database.contextDao().getContextById("parent"))
            assertEquals(workspaceBefore, database.workspaceDao().getById("parent"))
        } finally {
            database.close()
        }
    }

    @Test
    fun `live Context with canonical Workspace collision cannot cut over Workspace-only`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(context("leaf"))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped(now = 10L)
            val canonicalWorkspace =
                requireNotNull(database.workspaceDao().getById("leaf")).copy(
                    updatedAt = 15L,
                    version = 2L,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                )
            database.workspaceDao().upsert(listOf(canonicalWorkspace))

            val failure =
                runCatching {
                    repository(database, bootstrapper).migrateContext(
                        contextId = "leaf",
                        target = ContextMigrationTarget.WorkspaceOnly,
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertFalse(requireNotNull(database.contextDao().getContextById("leaf")).isDeleted)
            assertEquals(canonicalWorkspace, database.workspaceDao().getById("leaf"))
            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    "leaf",
                ),
            )
        } finally {
            database.close()
        }
    }

    private fun repository(
        database: AppDatabase,
        bootstrapper: CanonicalWorkspaceBootstrapper,
    ) =
        CanonicalContextMigrationRepository(
            contextDao = database.contextDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            aspectRepository = CanonicalAspectRepository(database, database.orientationDao()),
            aspectLinksRepository =
                CanonicalAspectLinksRepository(
                    database,
                    database.orientationDao(),
                    database.contextDao(),
                ),
            orientationRepository =
                CanonicalOrientationRepository(
                    database = database,
                    dao = database.orientationDao(),
                ),
            graphRepository =
                CanonicalOrientationGraphRepository(
                    database = database,
                    dao = database.orientationDao(),
                    workspaceDao = database.workspaceDao(),
                ),
            workspaceWriteThrough = ContextWorkspaceWriteThrough(bootstrapper),
            canonicalWorkspaceTagRepository = CanonicalWorkspaceTagRepository(database),
        )

    private suspend fun createExistingOrientation(
        database: AppDatabase,
        id: String,
        kind: OrientationKind,
        now: Long,
    ): String {
        val assessment = initialOrientationAssessment(kind)
        CanonicalOrientationRepository(database, database.orientationDao()).saveOrientation(
            subject =
                ManagedSubject(
                    id = id,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 3L,
                    subjectType = ManagedSubjectType.ORIENTATION,
                    title = "Existing $id",
                    description = "Preserve me",
                ),
            orientation =
                OrientationNode(
                    subjectId = id,
                    kind = kind,
                    lifecycle = null,
                    lifecycleOrigin = ValueOrigin.UNSET,
                    assessment = assessment,
                ),
            revision =
                OrientationAssessmentRevision(
                    id = "$id-revision",
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 4L,
                    orientationId = id,
                    effectiveFrom = now,
                    recordedAt = now,
                    source = AssessmentRevisionSource.USER,
                    reason = "Independent canonical authoring",
                    assessment = assessment,
                ),
        )
        return id
    }

    @Test
    fun `legacy System child does not block regular parent cutover or rewrite canonical System topology`() =
        runBlocking {
            val database = database()
            try {
                val systemChildId = SystemContexts.LEVELS.raw
                val canonicalSystemParentId = SystemContexts.PERSONAL_MANAGEMENT.raw

                database.contextDao().insert(context("parent"))
                database.contextDao().insert(
                    context(
                        id = systemChildId,
                        parentId = "parent",
                    ),
                )
                seedCanonicalSystemWorkspace(
                    database = database,
                    id = canonicalSystemParentId,
                    parentId = null,
                    name = "personal-management",
                )
                seedCanonicalSystemWorkspace(
                    database = database,
                    id = systemChildId,
                    parentId = canonicalSystemParentId,
                    name = "levels",
                )

                val bootstrapper = bootstrapper(database)
                bootstrapper.ensureBootstrapped(now = 10L)
                val repository = repository(database, bootstrapper)

                val childBefore =
                    requireNotNull(database.workspaceDao().getById(systemChildId))
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    childBefore.provenance,
                )
                assertNull(childBefore.sourceContextId)
                assertEquals(
                    canonicalSystemParentId,
                    childBefore.parentWorkspaceId,
                )

                val result =
                    repository.migrateContext(
                        contextId = "parent",
                        target =
                            ContextMigrationTarget
                                .NewAspectWithExistingWorkspace(),
                        now = 20L,
                    )

                assertTrue(result.changed)
                assertTrue(
                    requireNotNull(
                        database.contextDao().getContextById("parent"),
                    ).isDeleted,
                )

                // Legacy reserved Context metadata remains compatibility
                // evidence only and is not promoted into canonical topology.
                assertFalse(
                    requireNotNull(
                        database.contextDao().getContextById(systemChildId),
                    ).isDeleted,
                )

                val parentWorkspace =
                    requireNotNull(database.workspaceDao().getById("parent"))
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    parentWorkspace.provenance,
                )
                assertNull(parentWorkspace.sourceContextId)

                val childAfterCutover =
                    requireNotNull(database.workspaceDao().getById(systemChildId))
                assertEquals(childBefore, childAfterCutover)

                val report = bootstrapper.ensureBootstrapped(now = 30L)
                val childAfterBootstrap =
                    requireNotNull(database.workspaceDao().getById(systemChildId))

                assertEquals(childBefore, childAfterBootstrap)
                assertEquals(
                    canonicalSystemParentId,
                    childAfterBootstrap.parentWorkspaceId,
                )
                assertTrue(
                    report.issues.none {
                        it.contextId == systemChildId &&
                            it.code == "WORKSPACE_PARENT_COLLISION"
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `regular compatibility child under canonical parent still gets parent collision quarantine`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insert(context("parent"))
                database.contextDao().insert(
                    context(
                        id = "child",
                        parentId = "parent",
                    ),
                )

                val bootstrapper = bootstrapper(database)
                bootstrapper.ensureBootstrapped(now = 10L)

                val parentWorkspace =
                    requireNotNull(database.workspaceDao().getById("parent"))
                        .copy(
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                            updatedAt = 20L,
                            version = 2L,
                        )
                database.workspaceDao().upsert(listOf(parentWorkspace))

                val report = bootstrapper.ensureBootstrapped(now = 30L)
                val childWorkspace =
                    requireNotNull(database.workspaceDao().getById("child"))

                assertNull(childWorkspace.parentWorkspaceId)
                assertTrue(
                    report.issues.any {
                        it.contextId == "child" &&
                            it.code == "WORKSPACE_PARENT_COLLISION"
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `Workspace-only parent cutover preserves canonical System topology over legacy child metadata`() =
        runBlocking {
            val database = database()
            try {
                val systemChildId = SystemContexts.LEVELS.raw
                val canonicalSystemParentId = SystemContexts.PERSONAL_MANAGEMENT.raw

                database.contextDao().insert(context("parent"))
                database.contextDao().insert(
                    context(
                        id = systemChildId,
                        parentId = "parent",
                    ),
                )
                seedCanonicalSystemWorkspace(
                    database = database,
                    id = canonicalSystemParentId,
                    parentId = null,
                    name = "personal-management",
                )
                seedCanonicalSystemWorkspace(
                    database = database,
                    id = systemChildId,
                    parentId = canonicalSystemParentId,
                    name = "levels",
                )

                val bootstrapper = bootstrapper(database)
                bootstrapper.ensureBootstrapped(now = 10L)
                val repository = repository(database, bootstrapper)

                val childBefore =
                    requireNotNull(database.workspaceDao().getById(systemChildId))

                val result =
                    repository.migrateContext(
                        contextId = "parent",
                        target = ContextMigrationTarget.WorkspaceOnly,
                        now = 20L,
                    )

                assertTrue(result.changed)
                assertTrue(
                    requireNotNull(
                        database.contextDao().getContextById("parent"),
                    ).isDeleted,
                )
                assertFalse(
                    requireNotNull(
                        database.contextDao().getContextById(systemChildId),
                    ).isDeleted,
                )

                val parentWorkspace =
                    requireNotNull(database.workspaceDao().getById("parent"))
                assertEquals(
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                    parentWorkspace.provenance,
                )
                assertNull(parentWorkspace.sourceContextId)

                val childAfterCutover =
                    requireNotNull(database.workspaceDao().getById(systemChildId))
                assertEquals(childBefore, childAfterCutover)
                assertEquals(
                    canonicalSystemParentId,
                    childAfterCutover.parentWorkspaceId,
                )

                val report = bootstrapper.ensureBootstrapped(now = 30L)
                val childAfterBootstrap =
                    requireNotNull(database.workspaceDao().getById(systemChildId))

                assertEquals(childBefore, childAfterBootstrap)
                assertTrue(
                    report.issues.none {
                        it.contextId == systemChildId &&
                            it.code == "WORKSPACE_PARENT_COLLISION"
                    },
                )
            } finally {
                database.close()
            }
        }

    private suspend fun seedCanonicalSystemWorkspace(
        database: AppDatabase,
        id: String,
        parentId: String?,
        name: String,
    ) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = name,
                    descriptionOverride = null,
                    parentWorkspaceId = parentId,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private fun bootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(androidContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun context(
        id: String,
        parentId: String? = null,
    ) =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun stableMappingId(contextId: String): String =
        UUID.nameUUIDFromBytes(
            "CONTEXT-MIGRATION:$contextId".toByteArray(StandardCharsets.UTF_8),
        ).toString()
}
