package com.romankozak.forwardappmobile.features.mainscreen.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyPresentationProvenance
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyReadSnapshotAssembler
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2PresentedHierarchyEntry
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2SyntheticScopeKind
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementGroupScopeMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.MainBeaconOrientationBridge
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainBeaconV2CreateRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create in two Groups writes PRIMARY LINK scopes and PART_OF atomically`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            val groups = listOf("group-a", "group-b")
            groups.forEachIndexed { index, id ->
                val group = MainBeaconGroup(
                    id = id,
                    title = "Group $index",
                    createdAt = 10L,
                    updatedAt = 10L,
                )
                db.mainBeaconDao().insertGroup(group)
                bridge.writeCommon(group)
            }

            val beacon = beacon("beacon-a")
            repository.createBeacon(
                beacon = beacon,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = groups.toSet(),
                levelStatuses = emptyList(),
            )

            val subjectId = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    beacon.id,
                ),
            ).subjectId
            val placements = db.hierarchyPlacementDao().getAll()
                .filter { !it.isDeleted && it.targetId == subjectId }
            assertEquals(2, placements.size)
            assertEquals(1, placements.count { it.placementKind == "PRIMARY" })
            assertEquals(1, placements.count { it.placementKind == "LINK" })
            assertTrue(placements.all { it.parentPlacementId == null })

            val scopes = db.hierarchyPlacementGroupScopeDao().getAll()
                .filter { !it.isDeleted && it.placementId in placements.map { p -> p.id } }
            assertEquals(2, scopes.size)
            val groupSubjects = groups.map { id ->
                requireNotNull(
                    db.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                        id,
                    ),
                ).subjectId
            }.toSet()
            assertEquals(groupSubjects, scopes.mapNotNull { it.groupSubjectId }.toSet())

            val memberships = db.orientationDao().getAllOrientationRelations()
                .filter {
                    !it.isDeleted &&
                        it.relationType == OrientationRelationType.PART_OF.name &&
                        it.fromOrientationId == subjectId
                }
            assertEquals(groupSubjects, memberships.map { it.toOrientationId }.toSet())
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 Group reorder changes presentation order without structural writes`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            val groupA = MainBeaconGroup(id = "group-a", title = "Group A", order = 0L)
            val groupB = MainBeaconGroup(id = "group-b", title = "Group B", order = 1L)
            for (group in listOf(groupA, groupB)) {
                db.mainBeaconDao().insertGroup(group)
                bridge.writeCommon(group)
            }

            val beaconA = beacon("beacon-a")
            val beaconB = beacon("beacon-b")
            repository.createBeacon(
                beacon = beaconA,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = setOf(groupA.id),
                levelStatuses = emptyList(),
            )
            repository.createBeacon(
                beacon = beaconB,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = setOf(groupB.id),
                levelStatuses = emptyList(),
            )

            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val beforeRelations = db.orientationDao().getAllOrientationRelations()
            val beforeMembers = db.mainBeaconDao().getAllGroupMembersSync()
            val beforeBeacons = db.mainBeaconDao().getAllBeaconsSync()

            repository.reorderGroups(listOf(groupB.id, groupA.id))

            assertEquals(
                listOf(groupB.id, groupA.id),
                db.mainBeaconDao().getAllGroupsSync().map { it.id },
            )
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertEquals(beforeRelations, db.orientationDao().getAllOrientationRelations())
            assertEquals(beforeMembers, db.mainBeaconDao().getAllGroupMembersSync())
            assertEquals(beforeBeacons, db.mainBeaconDao().getAllBeaconsSync())

            val read =
                CanonicalV2HierarchyReadSnapshotAssembler().assemble(
                    placements =
                        db.hierarchyPlacementDao().getAll()
                            .filterNot { it.isDeleted }
                            .map { it.toHierarchyPlacementStrict() },
                    admittedWorkspacePresentations = emptyList(),
                    managedSubjects = db.orientationDao().getAllManagedSubjects(),
                    legacySubjectMappings = db.orientationDao().getAllLegacyMappings(),
                    relations = db.orientationDao().getAllOrientationRelations(),
                    groupScopes = db.hierarchyPlacementGroupScopeDao().getAll(),
                    legacyGroups = db.mainBeaconDao().getAllGroupsSync(),
                    presentationProvenance = CanonicalV2HierarchyPresentationProvenance(),
                )

            assertEquals(
                listOf(groupB.id, groupA.id),
                read.presentation.entries
                    .filterIsInstance<CanonicalV2PresentedHierarchyEntry.SyntheticScope>()
                    .filter { it.kind == CanonicalV2SyntheticScopeKind.GROUP }
                    .map { it.id },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 Group reorder rejects incomplete or duplicate Group identities without writes`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            val groups =
                listOf(
                    MainBeaconGroup(id = "group-a", title = "Group A", order = 0L),
                    MainBeaconGroup(id = "group-b", title = "Group B", order = 1L),
                )
            for (group in groups) {
                db.mainBeaconDao().insertGroup(group)
                bridge.writeCommon(group)
            }

            val beforeGroups = db.mainBeaconDao().getAllGroupsSync()
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val beforeRelations = db.orientationDao().getAllOrientationRelations()
            val beforeMembers = db.mainBeaconDao().getAllGroupMembersSync()

            val empty =
                runCatching { repository.reorderGroups(emptyList()) }
                    .exceptionOrNull()
            assertTrue(empty is IllegalArgumentException)

            val incomplete =
                runCatching { repository.reorderGroups(listOf("group-a")) }
                    .exceptionOrNull()
            assertTrue(incomplete is IllegalArgumentException)

            val duplicate =
                runCatching { repository.reorderGroups(listOf("group-a", "group-a")) }
                    .exceptionOrNull()
            assertTrue(duplicate is IllegalArgumentException)

            val unknown =
                runCatching { repository.reorderGroups(listOf("group-a", "missing-group")) }
                    .exceptionOrNull()
            assertTrue(unknown is IllegalArgumentException)

            assertEquals(beforeGroups, db.mainBeaconDao().getAllGroupsSync())
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertEquals(beforeRelations, db.orientationDao().getAllOrientationRelations())
            assertEquals(beforeMembers, db.mainBeaconDao().getAllGroupMembersSync())
        } finally {
            db.close()
        }
    }

    @Test
    fun `nested create uses selected LINK parent instead of PRIMARY`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val parent = beacon("parent-beacon")
            repository.createBeacon(
                beacon = parent,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
            )
            val parentSubjectId = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    parent.id,
                ),
            ).subjectId
            val primary = db.hierarchyPlacementDao().getAll()
                .single { it.targetId == parentSubjectId && it.placementKind == "PRIMARY" }

            val placementRepository = CanonicalHierarchyPlacementRepository(db)
            val selectedLink = placementRepository.createLinkAppearance(
                target = HierarchyTargetRef(
                    type = HierarchyTargetType.MANAGED_SUBJECT,
                    id = parentSubjectId,
                ),
                now = 25L,
            )
            HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(
                placementId = selectedLink,
                groupSubjectId = null,
                now = 25L,
            )

            val child = beacon("child-beacon")
            repository.createBeacon(
                beacon = child,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
                parentBeaconId = parent.id,
                parentPlacementId = selectedLink,
            )

            val childSubjectId = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    child.id,
                ),
            ).subjectId
            val childPlacement = db.hierarchyPlacementDao().getAll()
                .single { it.targetId == childSubjectId && !it.isDeleted }
            assertEquals("PRIMARY", childPlacement.placementKind)
            assertEquals(selectedLink.value, childPlacement.parentPlacementId)
            assertTrue(childPlacement.parentPlacementId != primary.id)
            assertTrue(
                db.hierarchyPlacementGroupScopeDao().getAll()
                    .none { it.placementId == childPlacement.id && !it.isDeleted },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `mismatched Beacon parent and occurrence reject create without writes`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val first = beacon("parent-first")
            val second = beacon("parent-second")
            for (parent in listOf(first, second)) {
                repository.createBeacon(
                    beacon = parent,
                    relatedOwnerIds = emptySet(),
                    relatedAttachmentIds = emptySet(),
                    groupIds = emptySet(),
                    levelStatuses = emptyList(),
                )
            }
            val secondSubjectId = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    second.id,
                ),
            ).subjectId
            val secondPlacementId = PlacementId(
                db.hierarchyPlacementDao().getAll()
                    .single { it.targetId == secondSubjectId && !it.isDeleted }
                    .id,
            )
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val invalidChild = beacon("invalid-child")

            try {
                repository.createBeacon(
                    beacon = invalidChild,
                    relatedOwnerIds = emptySet(),
                    relatedAttachmentIds = emptySet(),
                    groupIds = emptySet(),
                    levelStatuses = emptyList(),
                    parentBeaconId = first.id,
                    parentPlacementId = secondPlacementId,
                )
                fail("Expected the mismatched parent target and occurrence to be rejected")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("disagree"))
            }

            assertNull(db.mainBeaconDao().getBeaconById(invalidChild.id))
            assertNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    invalidChild.id,
                ),
            )
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
        } finally {
            db.close()
        }
    }

    @Test
    fun `unknown Group rolls back legacy Beacon canonical subject and H1`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val beacon = beacon("beacon-invalid")
            try {
                repository.createBeacon(
                    beacon = beacon,
                    relatedOwnerIds = emptySet(),
                    relatedAttachmentIds = emptySet(),
                    groupIds = setOf("missing-group"),
                    levelStatuses = emptyList(),
                )
                fail("Expected a missing canonical Group to abort the entire transaction")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("Group"))
            }
            assertNull(db.mainBeaconDao().getBeaconById(beacon.id))
            assertNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    beacon.id,
                ),
            )
            assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `metadata edit preserves Group membership and hierarchy rows`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            val group = MainBeaconGroup(
                id = "group-a",
                title = "Group",
                createdAt = 10L,
                updatedAt = 10L,
            )
            db.mainBeaconDao().insertGroup(group)
            bridge.writeCommon(group)
            val beacon = beacon("beacon-edit")
            repository.createBeacon(
                beacon = beacon,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = setOf(group.id),
                levelStatuses = emptyList(),
            )
            val beforeMembers = db.mainBeaconDao().getAllGroupMembersSync()
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val beforeRelations = db.orientationDao().getAllOrientationRelations()

            repository.updateBeacon(
                beacon = beacon.copy(title = "Edited", updatedAt = 40L),
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = setOf(group.id),
                levelStatuses = emptyList(),
            )

            assertEquals("Edited", db.mainBeaconDao().getBeaconById(beacon.id)?.title)
            assertEquals(beforeMembers, db.mainBeaconDao().getAllGroupMembersSync())
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertEquals(beforeRelations, db.orientationDao().getAllOrientationRelations())
            assertFalse(
                db.orientationDao().getAllOrientationRelations().any {
                    it.relationType == OrientationRelationType.PART_OF.name && it.isDeleted
                },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 metadata edit cannot rewrite legacy Beacon order`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val beacon = beacon("beacon-order")
            repository.createBeacon(
                beacon = beacon,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
            )
            val stored = requireNotNull(db.mainBeaconDao().getBeaconById(beacon.id))
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val failure = runCatching {
                repository.updateBeacon(
                    beacon = stored.copy(order = stored.order + 1L, title = "Unauthorized order edit"),
                    relatedOwnerIds = emptySet(),
                    relatedAttachmentIds = emptySet(),
                    groupIds = emptySet(),
                    levelStatuses = emptyList(),
                )
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(stored, db.mainBeaconDao().getBeaconById(beacon.id))
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 invalid Group delete rejects before database mutation`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val parent = beacon("legacy-parent")
            val child = beacon("legacy-child")
            for (beacon in listOf(parent, child)) {
                repository.createBeacon(
                    beacon = beacon,
                    relatedOwnerIds = emptySet(),
                    relatedAttachmentIds = emptySet(),
                    groupIds = emptySet(),
                    levelStatuses = emptyList(),
                )
            }
            val beforeBeacons = db.mainBeaconDao().getAllBeaconsSync()
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeMembers = db.mainBeaconDao().getAllGroupMembersSync()

            val blocked = listOf<suspend () -> Any?>(
                { repository.deleteGroup("missing-group") },
            )
            blocked.forEach { operation ->
                val failure = runCatching { operation() }.exceptionOrNull()
                assertTrue("Expected V2 legacy structural writer rejection: $failure", failure is IllegalArgumentException)
            }

            assertEquals(beforeBeacons, db.mainBeaconDao().getAllBeaconsSync())
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeMembers, db.mainBeaconDao().getAllGroupMembersSync())
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 delete tombstones PRIMARY and LINK but preserves independent Beacon targets`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            for (groupId in listOf("group-one", "group-two")) {
                val group = MainBeaconGroup(id = groupId, title = groupId)
                db.mainBeaconDao().insertGroup(group)
                bridge.writeCommon(group)
            }
            val deleted = beacon("deleted-beacon")
            val independent = beacon("independent-beacon")
            repository.createBeacon(
                beacon = deleted,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = setOf("group-one", "group-two"),
                levelStatuses = emptyList(),
            )
            repository.createBeacon(
                beacon = independent,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
            )
            val mapping = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, deleted.id),
            )
            val before = db.hierarchyPlacementDao().getAll()
                .filter { it.targetId == mapping.subjectId && !it.isDeleted }
            assertEquals(2, before.size)
            assertEquals(setOf("PRIMARY", "LINK"), before.map { it.placementKind }.toSet())

            repository.deleteBeacon(deleted.id)

            assertNull(db.mainBeaconDao().getBeaconById(deleted.id))
            assertTrue(requireNotNull(db.orientationDao().getManagedSubject(mapping.subjectId)).isDeleted)
            assertTrue(
                db.hierarchyPlacementDao().getAll()
                    .filter { it.id in before.map { placement -> placement.id } }
                    .all { it.isDeleted },
            )
            assertTrue(
                db.hierarchyPlacementGroupScopeDao().getAll()
                    .filter { it.placementId in before.map { placement -> placement.id } }
                    .all { it.isDeleted },
            )
            assertTrue(db.mainBeaconDao().getAllGroupMembersSync().none { it.beaconId == deleted.id })
            assertTrue(
                db.orientationDao().getAllOrientationRelations()
                    .filter { it.fromOrientationId == mapping.subjectId && it.relationType == OrientationRelationType.PART_OF.name }
                    .all { it.isDeleted },
            )
            assertTrue(db.mainBeaconDao().getBeaconById(independent.id) != null)
            val independentMapping = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, independent.id),
            )
            assertFalse(requireNotNull(db.orientationDao().getManagedSubject(independentMapping.subjectId)).isDeleted)
            assertTrue(
                db.hierarchyPlacementDao().getAll()
                    .any { it.targetId == independentMapping.subjectId && !it.isDeleted },
            )
            // Restoring only the semantic target must not implicitly restore appearances.
            val tombstone = requireNotNull(db.orientationDao().getManagedSubject(mapping.subjectId))
            db.orientationDao().upsertManagedSubjects(
                listOf(tombstone.copy(isDeleted = false, version = tombstone.version + 1L)),
            )
            assertTrue(
                db.hierarchyPlacementDao().getAll()
                    .filter { it.id in before.map { placement -> placement.id } }
                    .all { it.isDeleted },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 delete rejects a live child beneath LINK without changing any target`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val parent = beacon("parent-for-link")
            val child = beacon("child-under-link")
            repository.createBeacon(parent, emptySet(), emptySet(), emptySet(), emptyList())
            val mapping = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, parent.id),
            )
            val link = CanonicalHierarchyPlacementRepository(db).createLinkAppearance(
                target = HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, mapping.subjectId),
                now = 25L,
            )
            HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(link, null, 25L)
            repository.createBeacon(
                beacon = child,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
                parentBeaconId = parent.id,
                parentPlacementId = link,
            )
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val failure = runCatching { repository.deleteBeacon(parent.id) }.exceptionOrNull()

            assertTrue("Expected leaf-only rejection: $failure", failure != null)
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertFalse(requireNotNull(db.orientationDao().getManagedSubject(mapping.subjectId)).isDeleted)
            assertTrue(db.mainBeaconDao().getBeaconById(parent.id) != null)
            assertTrue(db.mainBeaconDao().getBeaconById(child.id) != null)
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 legacy DELETE failure rolls back canonical target and occurrence tombstones`() = runBlocking {
        val db = database()
        try {
            val (repository, _) = repository(db)
            val target = beacon("rollback-beacon")
            repository.createBeacon(target, emptySet(), emptySet(), emptySet(), emptyList())
            val mapping = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, target.id),
            )
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            db.openHelper.writableDatabase.execSQL(
                """CREATE TRIGGER reject_beacon_delete BEFORE DELETE ON main_beacons
                   BEGIN SELECT RAISE(ABORT, 'forced Beacon deletion failure'); END""",
            )
            val failure = runCatching { repository.deleteBeacon(target.id) }.exceptionOrNull()

            assertTrue("Expected legacy DELETE failure: $failure", failure != null)
            assertFalse(requireNotNull(db.orientationDao().getManagedSubject(mapping.subjectId)).isDeleted)
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertTrue(db.mainBeaconDao().getBeaconById(target.id) != null)
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 Group deletion converts sole root to NoGroup preserving its live child`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            val group = MainBeaconGroup(id = "sole-group", title = "Sole Group")
            db.mainBeaconDao().insertGroup(group)
            bridge.writeCommon(group)

            val parent = beacon("sole-group-parent")
            val child = beacon("sole-group-child")
            repository.createBeacon(parent, emptySet(), emptySet(), setOf(group.id), emptyList())
            val parentMapping = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, parent.id),
            )
            val groupMapping = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON_GROUP.name, group.id),
            )
            val root = db.hierarchyPlacementDao().getAll()
                .single { !it.isDeleted && it.targetId == parentMapping.subjectId }
            repository.createBeacon(
                beacon = child,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
                parentBeaconId = parent.id,
                parentPlacementId = PlacementId(root.id),
            )
            val beforeChild = db.hierarchyPlacementDao().getAll()
                .single { !it.isDeleted && it.parentPlacementId == root.id }

            repository.deleteGroup(group.id)

            assertTrue(requireNotNull(db.hierarchyPlacementDao().getById(root.id)).isDeleted.not())
            assertEquals(
                null,
                requireNotNull(db.hierarchyPlacementGroupScopeDao().getByPlacementId(root.id)).groupSubjectId,
            )
            assertFalse(requireNotNull(db.hierarchyPlacementDao().getById(beforeChild.id)).isDeleted)
            assertEquals(root.id, db.hierarchyPlacementDao().getById(beforeChild.id)?.parentPlacementId)
            assertFalse(requireNotNull(db.orientationDao().getManagedSubject(parentMapping.subjectId)).isDeleted)
            assertTrue(requireNotNull(db.orientationDao().getManagedSubject(groupMapping.subjectId)).isDeleted)
            assertNull(db.mainBeaconDao().getAllGroupsSync().firstOrNull { it.id == group.id })
            assertTrue(db.mainBeaconDao().getAllGroupMembersSync().none { it.groupId == group.id })
            assertTrue(
                db.orientationDao().getAllOrientationRelations()
                    .filter {
                        it.fromOrientationId == parentMapping.subjectId &&
                            it.toOrientationId == groupMapping.subjectId &&
                            it.relationType == OrientationRelationType.PART_OF.name
                    }.all { it.isDeleted },
            )
            HierarchyPlacementGroupScopeMutationCoordinator(db).validateAuthoritativeState()
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 Group deletion removes only its leaf root when another Group root remains`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            for (id in listOf("retired-group", "retained-group")) {
                val group = MainBeaconGroup(id = id, title = id)
                db.mainBeaconDao().insertGroup(group)
                bridge.writeCommon(group)
            }
            val target = beacon("two-group-beacon")
            val independent = beacon("independent-group-beacon")
            repository.createBeacon(
                target, emptySet(), emptySet(), setOf("retired-group", "retained-group"), emptyList(),
            )
            repository.createBeacon(
                independent, emptySet(), emptySet(), setOf("retained-group"), emptyList(),
            )
            val targetSubject = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, target.id),
            ).subjectId
            val retiredGroupSubject = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name, "retired-group",
                ),
            ).subjectId
            val retainedGroupSubject = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name, "retained-group",
                ),
            ).subjectId
            val before = db.hierarchyPlacementDao().getAll()
            val scoped = db.hierarchyPlacementGroupScopeDao().getAll().associateBy { it.placementId }
            val retiredRoot = before.single {
                !it.isDeleted && it.targetId == targetSubject &&
                    scoped[it.id]?.groupSubjectId == retiredGroupSubject
            }
            val retainedRoot = before.single {
                !it.isDeleted && it.targetId == targetSubject &&
                    scoped[it.id]?.groupSubjectId == retainedGroupSubject
            }
            val independentBefore = before.filter { it.targetId != targetSubject }

            repository.deleteGroup("retired-group")

            assertTrue(requireNotNull(db.hierarchyPlacementDao().getById(retiredRoot.id)).isDeleted)
            assertTrue(
                requireNotNull(db.hierarchyPlacementGroupScopeDao().getByPlacementId(retiredRoot.id)).isDeleted,
            )
            assertFalse(requireNotNull(db.hierarchyPlacementDao().getById(retainedRoot.id)).isDeleted)
            assertEquals(
                retainedGroupSubject,
                db.hierarchyPlacementGroupScopeDao().getByPlacementId(retainedRoot.id)?.groupSubjectId,
            )
            assertEquals(
                independentBefore,
                db.hierarchyPlacementDao().getAll().filter { it.targetId != targetSubject },
            )
            assertFalse(requireNotNull(db.orientationDao().getManagedSubject(targetSubject)).isDeleted)
            assertTrue(db.mainBeaconDao().getBeaconById(target.id) != null)
            assertTrue(db.mainBeaconDao().getBeaconById(independent.id) != null)
            assertEquals(listOf("retained-group"), db.mainBeaconDao().getGroupIdsForBeacon(target.id))
            HierarchyPlacementGroupScopeMutationCoordinator(db).validateAuthoritativeState()
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 Group deletion rejects nonempty removable branch before changing any root`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            for (id in listOf("group-to-reject", "group-to-keep")) {
                val group = MainBeaconGroup(id = id, title = id)
                db.mainBeaconDao().insertGroup(group)
                bridge.writeCommon(group)
            }
            val parent = beacon("branch-parent")
            val child = beacon("branch-child")
            val other = beacon("other-affected-root")
            repository.createBeacon(
                parent, emptySet(), emptySet(), setOf("group-to-reject", "group-to-keep"), emptyList(),
            )
            repository.createBeacon(
                other, emptySet(), emptySet(), setOf("group-to-reject", "group-to-keep"), emptyList(),
            )
            val parentSubject = requireNotNull(
                db.orientationDao().getLegacyMapping(LegacyOrientationSourceType.MAIN_BEACON.name, parent.id),
            ).subjectId
            val rejectedGroupSubject = requireNotNull(
                db.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name, "group-to-reject",
                ),
            ).subjectId
            val scopes = db.hierarchyPlacementGroupScopeDao().getAll().associateBy { it.placementId }
            val affectedRoot = db.hierarchyPlacementDao().getAll().single {
                it.targetId == parentSubject && !it.isDeleted &&
                    scopes[it.id]?.groupSubjectId == rejectedGroupSubject
            }
            repository.createBeacon(
                beacon = child,
                relatedOwnerIds = emptySet(),
                relatedAttachmentIds = emptySet(),
                groupIds = emptySet(),
                levelStatuses = emptyList(),
                parentBeaconId = parent.id,
                parentPlacementId = PlacementId(affectedRoot.id),
            )

            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val beforeSubjects = db.orientationDao().getAllManagedSubjects()
            val beforeRelations = db.orientationDao().getAllOrientationRelations()
            val beforeMembers = db.mainBeaconDao().getAllGroupMembersSync()

            val failure = runCatching { repository.deleteGroup("group-to-reject") }.exceptionOrNull()

            assertTrue("Expected leaf-only rejection, got $failure", failure is HierarchyChildPolicyRejectedException)
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertEquals(beforeSubjects, db.orientationDao().getAllManagedSubjects())
            assertEquals(beforeRelations, db.orientationDao().getAllOrientationRelations())
            assertEquals(beforeMembers, db.mainBeaconDao().getAllGroupMembersSync())
            assertTrue(db.mainBeaconDao().getAllGroupsSync().any { it.id == "group-to-reject" })
        } finally {
            db.close()
        }
    }

    @Test
    fun `V2 Group legacy DELETE failure rolls back NoGroup and canonical tombstones`() = runBlocking {
        val db = database()
        try {
            val (repository, bridge) = repository(db)
            val group = MainBeaconGroup(id = "rollback-group", title = "Rollback Group")
            db.mainBeaconDao().insertGroup(group)
            bridge.writeCommon(group)
            repository.createBeacon(
                beacon("rollback-group-beacon"),
                emptySet(), emptySet(), setOf(group.id), emptyList(),
            )
            val beforePlacements = db.hierarchyPlacementDao().getAll()
            val beforeScopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val beforeSubjects = db.orientationDao().getAllManagedSubjects()
            val beforeRelations = db.orientationDao().getAllOrientationRelations()
            val beforeMembers = db.mainBeaconDao().getAllGroupMembersSync()
            db.openHelper.writableDatabase.execSQL(
                """CREATE TRIGGER reject_group_delete BEFORE DELETE ON main_beacon_groups
                   BEGIN SELECT RAISE(ABORT, 'forced Group deletion failure'); END""",
            )

            val failure = runCatching { repository.deleteGroup(group.id) }.exceptionOrNull()

            assertTrue("Expected forced legacy DELETE failure, got $failure", failure != null)
            assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
            assertEquals(beforeScopes, db.hierarchyPlacementGroupScopeDao().getAll())
            assertEquals(beforeSubjects, db.orientationDao().getAllManagedSubjects())
            assertEquals(beforeRelations, db.orientationDao().getAllOrientationRelations())
            assertEquals(beforeMembers, db.mainBeaconDao().getAllGroupMembersSync())
            assertTrue(db.mainBeaconDao().getAllGroupsSync().any { it.id == group.id })
        } finally {
            db.close()
        }
    }

    private fun beacon(id: String) = MainBeacon(
        id = id,
        title = "Beacon",
        createdAt = 20L,
        updatedAt = 20L,
    )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun repository(db: AppDatabase): Pair<MainBeaconRepository, MainBeaconOrientationBridge> {
        val bridge = MainBeaconOrientationBridge(
            orientationDao = db.orientationDao(),
            mainBeaconDao = db.mainBeaconDao(),
            bootstrapper = mockk<CanonicalOrientationBootstrapper>(relaxed = true),
            hierarchyPlacementLifecycleCoordinator = HierarchyPlacementLifecycleCoordinator(db),
        )
        val repository = MainBeaconRepository(
            appDatabase = db,
            mainBeaconDao = db.mainBeaconDao(),
            orientationBridge = bridge,
            hierarchyPlacementRepository = CanonicalHierarchyPlacementRepository(db),
            groupScopeCoordinator = HierarchyPlacementGroupScopeMutationCoordinator(db),
        )
        return repository to bridge
    }
}
