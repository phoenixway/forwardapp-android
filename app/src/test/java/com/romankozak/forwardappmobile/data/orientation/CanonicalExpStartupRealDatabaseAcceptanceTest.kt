package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.BuildConfig
import com.romankozak.forwardappmobile.data.database.ALL_MIGRATIONS
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalFreshHierarchyEstablishmentSource
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyAuthorityActivator
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV1HierarchyMaterializer
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV1HierarchySnapshotBuilder
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningAdapter
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.LegacyManualTacticalMissionIntentAdopter
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.database.HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.day.DayPlanningActivationReport
import com.romankozak.forwardappmobile.data.workspace.SystemContextShellRetirer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.data.product.CanonicalGoalSemanticReadRepository
import com.romankozak.forwardappmobile.data.product.CanonicalProductPersistenceFrame
import com.romankozak.forwardappmobile.data.product.CanonicalProductReadRepository
import com.romankozak.forwardappmobile.data.product.CanonicalProductReadRoomSource
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Explicit HOST-only acceptance for the first five synchronous EXP startup stages.
 *
 * The real database inputs remain outside the repository and are never opened
 * by Room. Each test run operates on a disposable copy including WAL and SHM.
 *
 * This is not a full Android UI startup acceptance.
 */
@RunWith(RobolectricTestRunner::class)
class CanonicalExpStartupRealDatabaseAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `historical EXP database survives full synchronous startup twice`() =
        runBlocking {
            assertTrue(
                "This acceptance must use the experimental build flavor",
                BuildConfig.IS_EXPERIMENTAL_BUILD,
            )

            val sourceDirectory =
                requireNotNull(
                    System.getenv("FORWARDAPP_REAL_DB_DIR")
                        ?.takeIf(String::isNotBlank),
                ) {
                    "FORWARDAPP_REAL_DB_DIR is required for explicit HOST acceptance"
                }

            val sourceFiles =
                listOf(
                    "forward_app_database",
                    "forward_app_database-wal",
                    "forward_app_database-shm",
                ).map { name ->
                    File(sourceDirectory, name).also { file ->
                        require(
                            file.isFile &&
                                !java.nio.file.Files.isSymbolicLink(file.toPath())
                        ) {
                            "Missing or symlinked database input: $name"
                        }
                    }
                }

            val originalHashes = sourceFiles.associateWith(::sha256)
            val dbName = "r11_exp_startup_${UUID.randomUUID()}"
            val target = context.getDatabasePath(dbName)

            require(
                target.parentFile?.mkdirs() == true ||
                    target.parentFile?.isDirectory == true
            )
            require(sourceFiles.none { it.canonicalFile == target.canonicalFile })

            var openedDatabase: AppDatabase? = null

            try {
                sourceFiles.forEach { source ->
                    val suffix = source.name.removePrefix("forward_app_database")
                    val stagedFile = File(target.path + suffix)
                    source.copyTo(stagedFile, overwrite = false)
                    assertEquals(
                        "Historical database staging changed source bytes",
                        originalHashes.getValue(source),
                        sha256(stagedFile),
                    )
                }

                val db =
                    Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                        .addMigrations(*ALL_MIGRATIONS)
                        .addCallback(HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK)
                        .allowMainThreadQueries()
                        .build()

                openedDatabase = db

                val orientationDao = db.orientationDao()
                val s0ManagedSubjects =
                    orientationDao.observeManagedSubjects().first()

                val historicalRevisions =
                    orientationDao.getAllAssessmentRevisions()
                        .associateBy { it.id }
                val initialAssessmentCount =
                    orientationDao.getAllAssessments().size

                assertTrue(
                    "Expected real historical revisions",
                    historicalRevisions.isNotEmpty(),
                )
                assertTrue(
                    "Expected real historical assessments",
                    initialAssessmentCount > 0,
                )

                // R11.31: original offline SQLite is schema 198, matching
                // AppDatabase. Verify the copied database at the first Room
                // access boundary, before application startup materialization.
                fun s0ScalarLong(sql: String): Long =
                    db.openHelper.readableDatabase.query(sql).use { cursor ->
                        check(cursor.moveToFirst()) {
                            "Missing scalar result at historical S0 boundary"
                        }
                        cursor.getLong(0)
                    }

                assertEquals(
                    "Historical EXP Room schema version diverged",
                    198L,
                    s0ScalarLong("PRAGMA user_version"),
                )

                val s0Integrity =
                    db.openHelper.readableDatabase
                        .query("PRAGMA integrity_check")
                        .use { cursor ->
                            check(cursor.moveToFirst()) {
                                "Missing SQLite integrity result"
                            }
                            cursor.getString(0)
                        }
                assertEquals(
                    "Historical EXP SQLite integrity failed",
                    "ok",
                    s0Integrity,
                )

                val s0ForeignKeyViolations =
                    db.openHelper.readableDatabase
                        .query("PRAGMA foreign_key_check")
                        .use { cursor -> cursor.count }
                assertEquals(
                    "Historical EXP SQLite foreign key violations",
                    0,
                    s0ForeignKeyViolations,
                )

                val originalTableCounts =
                    linkedMapOf(
                        "tactical_missions" to 78L,
                        "arc_quests" to 12L,
                        "tactical_mission_attachment_cross_ref" to 1L,
                        "attachments" to 288L,
                        "legacy_subject_mappings" to 4348L,
                        "managed_subjects" to 4348L,
                        "orientations" to 4348L,
                        "planning_scopes" to 0L,
                        "planning_commitments" to 0L,
                    )

                originalTableCounts.forEach { (table, expected) ->
                    assertEquals(
                        "Historical EXP S0 table population diverged: $table",
                        expected,
                        s0ScalarLong("SELECT COUNT(*) FROM `$table`"),
                    )
                }

                println("R11_EXP_S0_ORIGINAL_BASELINE=PASS")

                // R11.31: capture historical carriers after Room migrations,
                // before any synchronous application startup materialization.
                val historicalMissions =
                    db.tacticalMissionDao()
                        .getAllMissionsSync()
                        .associateBy { it.id }
                val historicalArcQuests =
                    db.arcQuestDao()
                        .getAllSync()
                        .associateBy { it.id }
                val historicalAttachmentRows =
                    db.tacticalMissionDao()
                        .getAllMissionAttachmentCrossRefs()
                val historicalAttachmentLinks =
                    historicalAttachmentRows.toSet()

                assertEquals(
                    "Unexpected historical TacticalMission population",
                    78,
                    historicalMissions.size,
                )
                assertEquals(
                    "Unexpected historical ArcQuest population",
                    12,
                    historicalArcQuests.size,
                )
                assertEquals(
                    "Historical attachment links contain duplicates",
                    historicalAttachmentRows.size,
                    historicalAttachmentLinks.size,
                )
                assertTrue(
                    "Expected historical TacticalMission attachment links",
                    historicalAttachmentLinks.isNotEmpty(),
                )
                assertEquals(
                    "Unexpected historical ArcQuest tombstone count",
                    1,
                    historicalArcQuests.values.count { it.isDeleted },
                )

                suspend fun assertHistoricalPreservation(checkpoint: String) {
                    val missions =
                        db.tacticalMissionDao()
                            .getAllMissionsSync()
                            .associateBy { it.id }
                    val quests =
                        db.arcQuestDao()
                            .getAllSync()
                            .associateBy { it.id }
                    val attachmentRows =
                        db.tacticalMissionDao()
                            .getAllMissionAttachmentCrossRefs()
                    val links = attachmentRows.toSet()

                    // Deliberately avoid dumping historical payloads or IDs
                    // into JUnit failure messages.
                    assertTrue(
                        "$checkpoint TacticalMission payload changed",
                        historicalMissions == missions,
                    )
                    assertTrue(
                        "$checkpoint TacticalMission attachment links changed",
                        historicalAttachmentLinks == links &&
                            historicalAttachmentRows.size == links.size,
                    )
                    assertTrue(
                        "$checkpoint ArcQuest identity set changed",
                        historicalArcQuests.keys == quests.keys,
                    )

                    historicalArcQuests.forEach { (id, before) ->
                        val after = quests[id]
                        assertTrue(
                            "$checkpoint ArcQuest carrier disappeared",
                            after != null,
                        )
                        if (after != null) {
                            // Only fields explicitly owned by canonical
                            // compatibility projection may differ on S0 -> S1.
                            assertTrue(
                                "$checkpoint ArcQuest specialized fields changed",
                                before.copy(
                                    title = after.title,
                                    description = after.description,
                                    status = after.status,
                                    order = after.order,
                                    isDeleted = after.isDeleted,
                                ) == after,
                            )
                            assertTrue(
                                "$checkpoint ArcQuest tombstone resurrected",
                                !before.isDeleted || after.isDeleted,
                            )
                        }
                    }

                    assertTrue(
                        "$checkpoint attachment references missing missions",
                        links.all { it.missionId in missions },
                    )
                    for (link in links) {
                        assertTrue(
                            "$checkpoint attachment reference lost its attachment",
                            db.tacticalMissionDao()
                                .attachmentExists(link.attachmentId),
                        )
                    }

                    println("R11_EXP_PRESERVATION_${checkpoint}=PASS")
                }

                assertHistoricalPreservation("S0")

                val initializer =
                    DatabaseInitializer(
                        systemWorkspaceMaterializer =
                            SystemWorkspaceMaterializer(
                                db,
                                db.contextDao(),
                                db.workspaceDao(),
                            ),
                        systemWorkspaceTagSeed =
                            SystemWorkspaceTagSeed(
                                db,
                                db.contextDao(),
                            ),
                        systemContextShellRetirer =
                            SystemContextShellRetirer(
                                db,
                                db.contextDao(),
                            ),
                    )

                val dayTheme =
                    CanonicalDayThemeBootstrapper(
                        db,
                        db.dayThemeDocumentDao(),
                        db.canonicalDayThemeDao(),
                    )

                val orientation =
                    CanonicalOrientationBootstrapper(
                        db,
                        orientationDao,
                        db.goalDao(),
                        db.mainBeaconDao(),
                        db.arcQuestDao(),
                        db.canonicalDayThemeDao(),
                        dayTheme,
                    )

                val scoring =
                    CanonicalScoringAuthorityActivator(
                        db,
                        orientationDao,
                        db.goalDao(),
                        db.contextDao(),
                        db.canonicalScoringAuthorityStateDao(),
                    )

                val themeBridge =
                    ThemeOrientationBridge(
                        orientationDao,
                        db.canonicalDayThemeDao(),
                        orientation,
                    )

                val dayPlanning =
                    CanonicalDayPlanningAuthorityActivator(
                        db,
                        db.dayPlanDao(),
                        db.canonicalDayThemeDao(),
                        orientationDao,
                        db.planningDao(),
                        db.dayPlanningScopeExtensionDao(),
                        db.dayPlanningAuthorityStateDao(),
                        themeBridge,
                    )

                val workspace =
                    CanonicalWorkspaceBootstrapper(
                        database = db,
                        workspaceDao = db.workspaceDao(),
                        orientationDao = orientationDao,
                        contextDao = db.contextDao(),
                        contextStructureDao = db.contextStructureDao(),
                    )

                val orientationRepository =
                    CanonicalOrientationRepository(db, orientationDao)

                val questBridge =
                    QuestOrientationBridge(orientationDao, orientation)

                val tactical =
                    CanonicalTacticalPlanningAuthorityActivator(
                        db,
                        LegacyManualTacticalMissionIntentAdopter(
                            db,
                            orientationRepository,
                        ),
                        questBridge,
                    )

                val strategic =
                    CanonicalStrategicPlanningAuthorityActivator(
                        db,
                        questBridge,
                        tactical,
                    )

                val planningRepository =
                    CanonicalPlanningRepository(
                        db,
                        db.planningDao(),
                        db.dayPlanningScopeExtensionDao(),
                        db.tacticalPlanningScopeExtensionDao(),
                    )

                val strategicAdapter =
                    CanonicalStrategicPlanningAdapter(
                        db,
                        strategic,
                        planningRepository,
                        orientationRepository,
                        questBridge,
                    )

                val hierarchy =
                    CanonicalHierarchyAuthorityActivator(
                        db,
                        CanonicalFreshHierarchyEstablishmentSource(db),
                        CanonicalV1HierarchySnapshotBuilder(),
                        CanonicalV1HierarchyMaterializer(db),
                    )

                val first =
                    runFiveStages(
                        pass = 1,
                        initializer = initializer,
                        dayTheme = dayTheme,
                        orientation = orientation,
                        scoring = scoring,
                        dayPlanning = dayPlanning,
                    )

                runRemainingStages(
                    pass = 1,
                    workspace = workspace,
                    tactical = tactical,
                    strategic = strategic,
                    strategicAdapter = strategicAdapter,
                    hierarchy = hierarchy,
                )

                assertHistoricalPreservation("S1")

                val arcQuestsAfterFirst =
                    db.arcQuestDao()
                        .getAllSync()
                        .associateBy { it.id }

                val strategicFocusAfterFirst =
                    db.planningDao()
                        .getAllCommitments()
                        .filter {
                            it.role == "FOCUS" &&
                                it.provenanceSourceType == "ARC_QUEST"
                        }

                assertEquals(
                    "Unexpected canonical Strategic FOCUS population",
                    11,
                    strategicFocusAfterFirst.size,
                )
                assertEquals(
                    "Historical Strategic FOCUS tombstone missing",
                    1,
                    strategicFocusAfterFirst.count {
                        it.isDeleted && it.status == "DROPPED"
                    },
                )

                assertNotNull(
                    "Tactical authority marker missing",
                    db.tacticalPlanningAuthorityStateDao().get(),
                )
                assertNotNull(
                    "Strategic authority marker missing",
                    db.strategicPlanningAuthorityStateDao().get(),
                )

                assertTrue(
                    "Expected first scoring authority activation",
                    first.first.performed,
                )
                assertNotNull(
                    "Scoring marker missing",
                    db.canonicalScoringAuthorityStateDao().get(),
                )
                assertNotNull(
                    "Day Planning marker missing",
                    db.dayPlanningAuthorityStateDao().get(),
                )

                val revisionsAfterFirst =
                    orientationDao.getAllAssessmentRevisions()
                        .associateBy { it.id }
                val assessmentsAfterFirst =
                    orientationDao.getAllAssessments().associateBy { it.orientationId }
                val overridesAfterFirst =
                    orientationDao.getAllScoringOverrides()
                        .associateBy { it.orientationId }

                val scopesAfterFirst =
                    db.planningDao().getAllScopes().associateBy { it.id }
                val commitmentsAfterFirst =
                    db.planningDao().getAllCommitments().associateBy { it.id }

                assertTrue(
                    "Original immutable revisions changed during first startup",
                    historicalRevisions.all { (id, original) ->
                        revisionsAfterFirst[id] == original
                    },
                )
                assertTrue(
                    "Current assessments disappeared during first startup",
                    assessmentsAfterFirst.size >= initialAssessmentCount,
                )

                val second =
                    runFiveStages(
                        pass = 2,
                        initializer = initializer,
                        dayTheme = dayTheme,
                        orientation = orientation,
                        scoring = scoring,
                        dayPlanning = dayPlanning,
                    )

                val secondRemaining =
                    runRemainingStages(
                        pass = 2,
                        workspace = workspace,
                        tactical = tactical,
                        strategic = strategic,
                        strategicAdapter = strategicAdapter,
                        hierarchy = hierarchy,
                    )

                assertHistoricalPreservation("S2")

                assertTrue(
                    "Repeated startup changed ArcQuest compatibility rows",
                    arcQuestsAfterFirst ==
                        db.arcQuestDao()
                            .getAllSync()
                            .associateBy { it.id },
                )

                assertFalse(
                    "Second Tactical activation must be inert",
                    secondRemaining.first.performed,
                )
                assertFalse(
                    "Second Strategic activation must be inert",
                    secondRemaining.second.performed,
                )

                assertEquals(
                    "Repeated startup changed canonical Planning scopes",
                    scopesAfterFirst,
                    db.planningDao().getAllScopes().associateBy { it.id },
                )
                assertEquals(
                    "Repeated startup changed canonical Planning commitments",
                    commitmentsAfterFirst,
                    db.planningDao().getAllCommitments().associateBy { it.id },
                )

                assertFalse(
                    "Second scoring activation must be inert",
                    second.first.performed,
                )
                assertFalse(
                    "Second Day Planning activation must be inert",
                    second.second.performed,
                )

                assertEquals(
                    "Repeated startup changed immutable revisions",
                    revisionsAfterFirst,
                    orientationDao.getAllAssessmentRevisions()
                        .associateBy { it.id },
                )
                assertEquals(
                    "Repeated startup changed current assessments",
                    assessmentsAfterFirst,
                    orientationDao.getAllAssessments()
                        .associateBy { it.orientationId },
                )
                assertEquals(
                    "Repeated startup changed scoring overrides",
                    overridesAfterFirst,
                    orientationDao.getAllScoringOverrides()
                        .associateBy { it.orientationId },
                )

                assertTrue(
                    "Original historical revisions lost",
                    historicalRevisions.all { (id, original) ->
                        revisionsAfterFirst[id] == original
                    },
                )

                // R1P.15: exercise the actual product and Goal read paths
                // against the same historical copy after completed S2 startup,
                // before deliberately introducing the negative Goal drift.
                stage("2.canonicalProductAndGoalRead") {
                    assertHistoricalCanonicalProductRead(
                        db = db,
                        originalSubjectIds = s0ManagedSubjects.map { it.id }.toSet(),
                        originalTombstoneIds =
                            s0ManagedSubjects.filter { it.isDeleted }.map { it.id }.toSet(),
                    )
                }

                // Negative regression: relaxing only the canonical-owned
                // timestamp must not admit genuine Goal shadow divergence.
                val liveGoalMapping =
                    orientationDao.getAllLegacyMappings()
                        .firstOrNull {
                            !it.isDeleted &&
                                it.sourceType == "GOAL" &&
                                it.state == "CUT_OVER"
                        }

                val goalMapping =
                    requireNotNull(liveGoalMapping) {
                        "Expected at least one live CUT_OVER Goal"
                    }

                val originalSubject =
                    requireNotNull(
                        orientationDao.getManagedSubject(goalMapping.subjectId)
                    )

                orientationDao.upsertManagedSubjects(
                    listOf(
                        originalSubject.copy(
                            title = originalSubject.title + " [R11_TEST_DRIFT]"
                        )
                    )
                )

                val driftReport = orientation.ensureBootstrapped()

                assertTrue(
                    "Real Goal title drift must still be rejected",
                    driftReport.issues.any {
                        it.sourceType == "GOAL" &&
                            it.code == "SHADOW_MISMATCH" &&
                            it.detail.contains("subject.title")
                    },
                )

                println("R11_EXP_GOAL_TITLE_DRIFT_REJECTED=PASS")
                println("R11_EXP_FULL_STARTUP_REAL_DB=PASS")
            } finally {
                openedDatabase?.close()
                context.deleteDatabase(dbName)

                originalHashes.forEach { (source, original) ->
                    assertEquals(
                        "Real EXP database input changed",
                        original,
                        sha256(source),
                    )
                }
            }
        }

    private suspend fun assertHistoricalCanonicalProductRead(
        db: AppDatabase,
        originalSubjectIds: Set<String>,
        originalTombstoneIds: Set<String>,
    ) {
        val dao = db.orientationDao()

        val subjectsBefore = dao.observeManagedSubjects().first()
        val assessmentsBefore = dao.getAllAssessments().toSet()

        assertEquals(
            "Unexpected original S0 managed subject count",
            4348,
            originalSubjectIds.size,
        )
        assertEquals(
            "Unexpected original S0 tombstone count",
            88,
            originalTombstoneIds.size,
        )

        val s2SubjectsById = subjectsBefore.associateBy { it.id }

        assertEquals(
            "S2 contains duplicate managed subject identities",
            subjectsBefore.size,
            s2SubjectsById.size,
        )
        assertTrue(
            "S2 lost original historical managed subjects",
            s2SubjectsById.keys.containsAll(originalSubjectIds),
        )
        assertTrue(
            "S2 resurrected original historical tombstones",
            originalTombstoneIds.all { id ->
                s2SubjectsById[id]?.isDeleted == true
            },
        )
        assertEquals(
            "S2 unexpectedly changed historical tombstone population",
            88,
            subjectsBefore.count { it.isDeleted },
        )

        val liveSubjectsAfterStartup =
            subjectsBefore.count { !it.isDeleted }

        println("R1P15_S0_MANAGED_SUBJECTS=${originalSubjectIds.size}")
        println("R1P15_S2_MANAGED_SUBJECTS=${subjectsBefore.size}")
        println("R1P15_S2_ADDED_SUBJECTS=${subjectsBefore.size - originalSubjectIds.size}")
        println("R1P15_S2_LIVE_SUBJECTS=$liveSubjectsAfterStartup")

        val source = CanonicalProductReadRoomSource(db)

        when (val frame = withTimeout(180_000L) { source.observe().first() }) {
            is CanonicalProductPersistenceFrame.Ready -> {
                println("R1P15_PRODUCT_PERSISTENCE_FRAME=READY")
            }

            is CanonicalProductPersistenceFrame.IntegrityFailure -> {
                frame.violations
                    .groupingBy { it.code }
                    .eachCount()
                    .toSortedMap()
                    .forEach { (code, count) ->
                        println("R1P15_FRAME_VIOLATION_CODE=$code COUNT=$count")
                    }

                throw AssertionError("R1P15_PRODUCT_PERSISTENCE_FRAME=INTEGRITY_FAILURE")
            }
        }

        val productScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        try {
            val repository =
                CanonicalProductReadRepository(source, productScope)

            val product =
                withTimeout(180_000L) {
                    repository.observe().first()
                }

            when (product) {
                is CanonicalProductReadResult.Ready -> {
                    assertEquals(
                        "Canonical Product omitted live S2 subjects",
                        liveSubjectsAfterStartup,
                        product.orientations.size,
                    )
                    println("R1P15_PRODUCT_DOMAIN_READ=READY")
                    println("R1P15_PRODUCT_LIVE_ORIENTATIONS=${product.orientations.size}")
                }

                is CanonicalProductReadResult.IntegrityFailure -> {
                    product.violations
                        .groupingBy { it.code }
                        .eachCount()
                        .toSortedMap()
                        .forEach { (code, count) ->
                            println("R1P15_PRODUCT_VIOLATION_CODE=$code COUNT=$count")
                        }

                    throw AssertionError("R1P15_PRODUCT_DOMAIN_READ=INTEGRITY_FAILURE")
                }
            }

            val canonicalGoals =
                withTimeout(180_000L) {
                    CanonicalGoalSemanticReadRepository(repository)
                        .observeAll()
                        .first()
                }

            println("R1P15_GOAL_SEMANTIC_READ=PASS")
            println("R1P15_GOAL_SEMANTIC_COUNT=${canonicalGoals.size}")
        } finally {
            productScope.cancel()
        }

        assertEquals(
            "Product read modified historical subjects",
            subjectsBefore.toSet(),
            dao.observeManagedSubjects().first().toSet(),
        )
        assertEquals(
            "Product read modified historical assessments",
            assessmentsBefore,
            dao.getAllAssessments().toSet(),
        )

        println("R1P15_HISTORICAL_TOMBSTONES=88")
        println("R1P15_HISTORICAL_PRODUCT_READ_ACCEPTANCE=PASS")
    }

    private suspend fun runFiveStages(
        pass: Int,
        initializer: DatabaseInitializer,
        dayTheme: CanonicalDayThemeBootstrapper,
        orientation: CanonicalOrientationBootstrapper,
        scoring: CanonicalScoringAuthorityActivator,
        dayPlanning: CanonicalDayPlanningAuthorityActivator,
    ): Pair<CanonicalScoringActivationReport, DayPlanningActivationReport> {
        stage("$pass.systemWorkspaceOwnership") {
            initializer.ensureCanonicalSystemWorkspaceOwnership()
        }

        stage("$pass.dayThemeBootstrap") {
            dayTheme.ensureBootstrapped()
        }

        stage("$pass.orientationBootstrap") {
            val report = orientation.ensureBootstrapped()
            assertTrue(
                "Orientation bootstrap issues: ${report.issues.size}",
                report.issues.isEmpty(),
            )
        }

        val scoringReport =
            stage("$pass.scoringAuthorityActivation") {
                scoring.ensureActivated(now = 1_800_000_000_000L + pass)
            }

        val dayReport =
            stage("$pass.dayPlanningAuthorityActivation") {
                try {
                    dayPlanning.ensureActivated(now = 1_800_000_000_100L + pass)
                } catch (originalFailure: IllegalStateException) {
                    try {
                        val diagnostic = orientation.ensureBootstrapped()
                        val groups =
                            diagnostic.issues
                                .groupingBy { "${it.sourceType}:${it.code}" }
                                .eachCount()
                                .toSortedMap()

                        println(
                            "R11_EXP_ORIENTATION_RECHECK_ISSUES=${diagnostic.issues.size}"
                        )

                        groups.forEach { (key, count) ->
                            println(
                                "R11_EXP_ORIENTATION_ISSUE_GROUP=$key COUNT=$count"
                            )
                        }

                        // Only report schema field names, never Goal IDs,
                        // titles, values, or user-authored diagnostic text.
                        val fieldPattern = Regex("[A-Za-z][A-Za-z0-9_.]*")
                        val diagnosticPrefix =
                            "Canonical row differs from the legacy projection: "

                        val goalMismatchFields =
                            diagnostic.issues
                                .filter {
                                    it.sourceType == "GOAL" &&
                                        it.code == "SHADOW_MISMATCH"
                                }
                                .flatMap { issue ->
                                    if (!issue.detail.startsWith(diagnosticPrefix)) {
                                        emptyList()
                                    } else {
                                        issue.detail
                                            .removePrefix(diagnosticPrefix)
                                            .split(",")
                                            .map(String::trim)
                                            .filter(fieldPattern::matches)
                                    }
                                }

                        val fieldCounts =
                            goalMismatchFields
                                .groupingBy { it }
                                .eachCount()
                                .toSortedMap()

                        println(
                            "R11_EXP_GOAL_SHADOW_PARSED_FIELDS=${goalMismatchFields.size}"
                        )

                        fieldCounts.forEach { (field, count) ->
                            println(
                                "R11_EXP_GOAL_SHADOW_FIELD=$field COUNT=$count"
                            )
                        }
                    } catch (diagnosticFailure: Exception) {
                        println(
                            "R11_EXP_ORIENTATION_DIAGNOSTIC_EXCEPTION=" +
                                diagnosticFailure.javaClass.simpleName
                        )
                    }

                    throw originalFailure
                }
            }

        return scoringReport to dayReport
    }

    private suspend fun runRemainingStages(
        pass: Int,
        workspace: CanonicalWorkspaceBootstrapper,
        tactical: CanonicalTacticalPlanningAuthorityActivator,
        strategic: CanonicalStrategicPlanningAuthorityActivator,
        strategicAdapter: CanonicalStrategicPlanningAdapter,
        hierarchy: CanonicalHierarchyAuthorityActivator,
    ): Pair<
        com.romankozak.forwardappmobile.data.planning.tactical.TacticalPlanningActivationReport,
        com.romankozak.forwardappmobile.data.planning.strategic.StrategicPlanningActivationReport,
    > {
        stage("$pass.workspaceBootstrap") {
            val report = workspace.ensureBootstrapped()
            assertTrue(
                "Workspace bootstrap issues: ${report.issues.size}",
                report.issues.isEmpty(),
            )
        }

        val tacticalReport =
            stage("$pass.tacticalPlanningAuthorityActivation") {
                tactical.ensureActivated(
                    now = 1_800_000_000_200L + pass
                )
            }

        val strategicReport =
            stage("$pass.strategicPlanningAuthorityActivation") {
                strategic.ensureActivated(
                    now = 1_800_000_000_300L + pass
                )
            }

        stage("$pass.strategicCompatibilityProjection") {
            strategicAdapter.projectCompatibilityRows()
        }

        stage("$pass.hierarchyAuthorityEstablishment") {
            hierarchy.ensureEstablished(
                now = 1_800_000_000_400L + pass
            )
        }

        return tacticalReport to strategicReport
    }

    private suspend fun <T> stage(
        name: String,
        action: suspend () -> T,
    ): T {
        println("R11_EXP_STAGE_BEGIN=$name")

        val result =
            try {
                action()
            } catch (failure: Throwable) {
                // Report only stage and exception type. Historical EXP
                // record content must not be copied into diagnostics.
                throw IllegalStateException(
                    "R11_EXP_STAGE_FAILURE=$name " +
                        "TYPE=${failure.javaClass.simpleName}",
                    failure,
                )
            }

        println("R11_EXP_STAGE_PASS=$name")
        return result
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
