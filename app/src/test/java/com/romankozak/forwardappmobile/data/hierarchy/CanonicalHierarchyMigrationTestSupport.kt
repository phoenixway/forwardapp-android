package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.data.database.ALL_MIGRATIONS
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File

internal class CanonicalHierarchyMigrationFixtureHarness(
    private val context: Context,
) {
    fun migrateTo179(
        dbName: String,
        startVersion: Int,
        seed: (SupportSQLiteDatabase) -> Unit,
        afterMigrations: (SupportSQLiteDatabase) -> Unit = {},
    ) {
        context.deleteDatabase(dbName)
        createFromExportedSchema(dbName, startVersion)
        openAtVersion(dbName, startVersion).use { seed(it.writableDatabase) }

        val chain =
            ALL_MIGRATIONS.filter {
                it.startVersion >= startVersion && it.endVersion <= SCHEMA_179
            }
        require(chain.first().startVersion == startVersion)
        require(chain.last().endVersion == SCHEMA_179)
        chain.zipWithNext().forEach { (left, right) ->
            require(left.endVersion == right.startVersion)
        }

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(SCHEMA_179) {
                        override fun onConfigure(db: SupportSQLiteDatabase) {
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }

                        override fun onCreate(db: SupportSQLiteDatabase) =
                            error("Historical fixture must already exist")

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            require(oldVersion == startVersion && newVersion == SCHEMA_179)
                            chain.forEach { it.migrate(db) }
                            afterMigrations(db)
                            db.execSQL(
                                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                                    "VALUES (42, '$SCHEMA_179_IDENTITY_HASH')",
                            )
                        }
                    },
                ).build(),
        ).use { it.writableDatabase }
    }

    /**
     * Opens the current Room contract. Because AppDatabase is schema 180 this
     * may run 179 -> 180 physical retirement and therefore must never be used
     * to inspect or mutate schema-179 legacy storage.
     */
    fun openCurrentRoom(dbName: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    /**
     * Opens an already-created schema-179 fixture without allowing Room 180
     * to migrate it. Use this for pre-retirement evidence, injected migration
     * failures and marker-v1/v2 convergence setup.
     */
    fun openSchema179Raw(dbName: String): SupportSQLiteOpenHelper =
        openAtVersion(dbName, SCHEMA_179)

    fun openSchema180Raw(dbName: String): SupportSQLiteOpenHelper =
        openAtVersion(dbName, 180)

    fun runFailingUpgradeFrom179(
        dbName: String,
        failurePoint: CanonicalHierarchyMigrationCheckpoint,
        now: Long,
    ): Throwable? =
        runCatching {
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(180) {
                            override fun onConfigure(db: SupportSQLiteDatabase) {
                                db.execSQL("PRAGMA foreign_keys = ON")
                            }

                            override fun onCreate(db: SupportSQLiteDatabase) = error("Expected schema 179")

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) {
                                require(oldVersion == 179 && newVersion == 180)
                                CanonicalHierarchyMigrationEstablisher().establish(
                                    db = db,
                                    now = now,
                                    onCheckpoint = { checkpoint ->
                                        if (checkpoint == failurePoint) {
                                            error("Injected migration hierarchy failure at $checkpoint")
                                        }
                                    },
                                )
                            }
                        },
                    ).build(),
            ).use { it.writableDatabase }
        }.exceptionOrNull()

    private fun createFromExportedSchema(
        dbName: String,
        version: Int,
    ) {
        val schema =
            JsonParser.parseString(findSchema(version).readText())
                .asJsonObject
                .getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            schema.getAsJsonArray("entities").forEach { element ->
                                val entity = element.asJsonObject
                                val table = entity.get("tableName").asString
                                db.execSQL(
                                    entity.get("createSql").asString
                                        .replace("\${TABLE_NAME}", table),
                                )
                                entity.getAsJsonArray("indices")?.forEach { index ->
                                    db.execSQL(
                                        index.asJsonObject.get("createSql").asString
                                            .replace("\${TABLE_NAME}", table),
                                    )
                                }
                            }
                            schema.getAsJsonArray("views")?.forEach {
                                db.execSQL(it.asJsonObject.get("createSql").asString)
                            }
                            schema.getAsJsonArray("setupQueries")?.forEach {
                                db.execSQL(it.asString)
                            }
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        ).use { it.writableDatabase }
    }

    private fun openAtVersion(
        dbName: String,
        version: Int,
    ): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onConfigure(db: SupportSQLiteDatabase) {
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }

                        override fun onCreate(db: SupportSQLiteDatabase) = error("Fixture missing")

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected fixture upgrade")
                    },
                ).build(),
        )

    private fun findSchema(version: Int): File {
        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json"
        val candidates =
            listOf(
                File(relative),
                File(System.getProperty("user.dir"), relative),
                File(System.getProperty("user.dir"), "../$relative"),
            )
        return requireNotNull(candidates.firstOrNull(File::isFile)) {
            "Unable to locate exported schema $version from ${System.getProperty("user.dir")}"
        }
    }

    private companion object {
        const val SCHEMA_179 = 179
        const val SCHEMA_179_IDENTITY_HASH = "91089a92c8cba296252eb67615540617"
    }
}

internal suspend fun convergeRuntimeHierarchyPrerequisites(db: AppDatabase) {
    SystemWorkspaceMaterializer(
        database = db,
        contextDao = db.contextDao(),
        workspaceDao = db.workspaceDao(),
    ).materializeAll(
        now = MIGRATION_TEST_NOW,
        seedMissingFactoryCapabilities = false,
    )
    SystemWorkspaceTagSeed(db, db.contextDao())
        .seedMissingCanonicalCollections(now = MIGRATION_TEST_NOW)

    val dayThemeBootstrapper =
        CanonicalDayThemeBootstrapper(
            database = db,
            legacyDao = db.dayThemeDocumentDao(),
            canonicalDao = db.canonicalDayThemeDao(),
        )
    val orientationReport =
        CanonicalOrientationBootstrapper(
            database = db,
            orientationDao = db.orientationDao(),
            goalDao = db.goalDao(),
            mainBeaconDao = db.mainBeaconDao(),
            arcQuestDao = db.arcQuestDao(),
            canonicalDayThemeDao = db.canonicalDayThemeDao(),
            canonicalDayThemeBootstrapper = dayThemeBootstrapper,
        ).ensureBootstrapped()
    require(orientationReport.issues.isEmpty())
}

internal fun runtimeHierarchyActivator(db: AppDatabase): CanonicalHierarchyAuthorityActivator =
    CanonicalHierarchyAuthorityActivator(
        database = db,
        freshSource = CanonicalFreshHierarchyEstablishmentSource(db),
        snapshotBuilder = CanonicalV1HierarchySnapshotBuilder(),
        materializer = CanonicalV1HierarchyMaterializer(db),
    )

internal data class HierarchyMigrationState(
    val placements: List<PlacementState>,
    val groupScopes: List<GroupScopeState>,
    val linkedAppearances: List<LinkedAppearanceState>,
    val targetMappings: List<TargetMappingState>,
    val targetSubjects: List<TargetSubjectState>,
    val orientations: List<OrientationState>,
    val workspaces: List<WorkspaceState>,
    val markerVersion: Int?,
    val markerActivatedAt: Long?,
    val origin: String?,
    val beacons: List<BeaconState>,
)

internal suspend fun hierarchyMigrationState(db: AppDatabase): HierarchyMigrationState {
    val targetMappings =
        db.orientationDao().getAllLegacyMappings()
            .filter {
                it.sourceType == "MAIN_BEACON" || it.sourceType == "MAIN_BEACON_GROUP"
            }
    val targetSubjectIds = targetMappings.mapTo(mutableSetOf()) { it.subjectId }
    val marker = db.hierarchyAuthorityActivationStateDao().get("GENERAL")

    return HierarchyMigrationState(
        placements =
            db.hierarchyPlacementDao().getAll().map {
                PlacementState(
                    it.id, it.hierarchyId, it.targetType, it.targetId,
                    it.parentPlacementId, it.placementKind, it.siblingOrder,
                    it.isDeleted, it.version,
                )
            }.sortedBy { it.id },
        groupScopes =
            db.hierarchyPlacementGroupScopeDao().getAll().map {
                GroupScopeState(it.placementId, it.hierarchyId, it.groupSubjectId, it.isDeleted, it.version)
            }.sortedBy { it.placementId },
        linkedAppearances =
            db.hierarchyPlacementLinkedAppearanceDao().getAll().map {
                LinkedAppearanceState(it.placementId, it.hierarchyId, it.isDeleted, it.version)
            }.sortedBy { it.placementId },
        targetMappings =
            targetMappings.map {
                    TargetMappingState(
                        it.sourceType, it.sourceId, it.subjectId,
                        it.migrationVersion, it.state, it.isDeleted, it.version,
                    )
                }.sortedWith(compareBy({ it.sourceType }, { it.sourceId })),
        targetSubjects =
            db.orientationDao().getAllManagedSubjects()
                .filter { it.id in targetSubjectIds }
                .map {
                    TargetSubjectState(
                        it.id, it.subjectType, it.title, it.description,
                        it.isDeleted, it.version,
                    )
                }.sortedBy { it.id },
        orientations =
            db.orientationDao().getAllOrientations()
                .filter { it.subjectId in targetSubjectIds }
                .map { OrientationState(it.subjectId, it.kind, it.lifecycle, it.lifecycleOrigin) }
                .sortedBy { it.subjectId },
        workspaces =
            db.workspaceDao().getAll()
                .map {
                    WorkspaceState(
                        it.id, it.nameOverride,
                        it.provenance, it.sourceContextId, it.isDeleted,
                    )
                }.sortedBy { it.id },
        markerVersion = marker?.version,
        markerActivatedAt = marker?.activatedAt,
        origin = db.hierarchyEstablishmentOriginDao().get("GENERAL")?.origin,
        beacons =
            db.mainBeaconDao().getAllBeaconsSync()
                .map { BeaconState(it.id, it.order) }
                .sortedBy { it.id },
    )
}

internal fun hierarchyMigrationStateRaw(db: SupportSQLiteDatabase): HierarchyMigrationState {
    fun rows(sql: String, block: (android.database.Cursor) -> Unit) {
        db.query(sql).use { cursor ->
            while (cursor.moveToNext()) block(cursor)
        }
    }

    val targetMappings = mutableListOf<TargetMappingState>()
    rows(
        """
        SELECT sourceType, sourceId, subjectId, migrationVersion, state, isDeleted, version
        FROM legacy_subject_mappings
        WHERE sourceType IN ('MAIN_BEACON', 'MAIN_BEACON_GROUP')
        ORDER BY sourceType, sourceId
        """.trimIndent(),
    ) { c ->
        targetMappings +=
            TargetMappingState(
                c.getString(0), c.getString(1), c.getString(2),
                c.getInt(3), c.getString(4), c.getInt(5) != 0, c.getLong(6),
            )
    }

    val targetSubjectIds = targetMappings.mapTo(mutableSetOf()) { it.subjectId }

    val placements = mutableListOf<PlacementState>()
    rows(
        """
        SELECT id, hierarchyId, targetType, targetId, parentPlacementId,
               placementKind, siblingOrder, isDeleted, version
        FROM hierarchy_placements
        ORDER BY id
        """.trimIndent(),
    ) { c ->
        placements +=
            PlacementState(
                c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                if (c.isNull(4)) null else c.getString(4),
                c.getString(5), c.getLong(6), c.getInt(7) != 0, c.getLong(8),
            )
    }

    val groupScopes = mutableListOf<GroupScopeState>()
    rows(
        """
        SELECT placementId, hierarchyId, groupSubjectId, isDeleted, version
        FROM hierarchy_placement_group_scopes
        ORDER BY placementId
        """.trimIndent(),
    ) { c ->
        groupScopes +=
            GroupScopeState(
                c.getString(0), c.getString(1),
                if (c.isNull(2)) null else c.getString(2),
                c.getInt(3) != 0, c.getLong(4),
            )
    }

    val linkedAppearances = mutableListOf<LinkedAppearanceState>()
    rows(
        """
        SELECT placementId, hierarchyId, isDeleted, version
        FROM hierarchy_placement_linked_appearances
        ORDER BY placementId
        """.trimIndent(),
    ) { c ->
        linkedAppearances +=
            LinkedAppearanceState(
                c.getString(0), c.getString(1), c.getInt(2) != 0, c.getLong(3),
            )
    }

    val targetSubjects = mutableListOf<TargetSubjectState>()
    if (targetSubjectIds.isNotEmpty()) {
        val placeholders = targetSubjectIds.joinToString(",") { "?" }
        db.query(
            """
            SELECT id, subjectType, title, description, isDeleted, version
            FROM managed_subjects
            WHERE id IN ($placeholders)
            ORDER BY id
            """.trimIndent(),
            targetSubjectIds.toTypedArray(),
        ).use { c ->
            while (c.moveToNext()) {
                targetSubjects +=
                    TargetSubjectState(
                        c.getString(0), c.getString(1), c.getString(2),
                        if (c.isNull(3)) null else c.getString(3),
                        c.getInt(4) != 0, c.getLong(5),
                    )
            }
        }
    }

    val orientations = mutableListOf<OrientationState>()
    if (targetSubjectIds.isNotEmpty()) {
        val placeholders = targetSubjectIds.joinToString(",") { "?" }
        db.query(
            """
            SELECT subjectId, kind, lifecycle, lifecycleOrigin
            FROM orientations
            WHERE subjectId IN ($placeholders)
            ORDER BY subjectId
            """.trimIndent(),
            targetSubjectIds.toTypedArray(),
        ).use { c ->
            while (c.moveToNext()) {
                orientations +=
                    OrientationState(
                        c.getString(0), c.getString(1),
                        if (c.isNull(2)) null else c.getString(2),
                        c.getString(3),
                    )
            }
        }
    }

    val workspaces = mutableListOf<WorkspaceState>()
    rows(
        """
        SELECT id, nameOverride, provenance, sourceContextId, isDeleted
        FROM workspaces
        ORDER BY id
        """.trimIndent(),
    ) { c ->
        workspaces +=
            WorkspaceState(
                c.getString(0),
                if (c.isNull(1)) null else c.getString(1),
                c.getString(2),
                if (c.isNull(3)) null else c.getString(3),
                c.getInt(4) != 0,
            )
    }

    var markerVersion: Int? = null
    var markerActivatedAt: Long? = null
    db.query(
        """
        SELECT version, activatedAt
        FROM hierarchy_authority_activation_state
        WHERE hierarchyId = 'GENERAL'
        LIMIT 1
        """.trimIndent(),
    ).use { c ->
        if (c.moveToFirst()) {
            markerVersion = c.getInt(0)
            markerActivatedAt = c.getLong(1)
        }
    }

    var origin: String? = null
    db.query(
        """
        SELECT origin
        FROM hierarchy_establishment_origin
        WHERE hierarchyId = 'GENERAL'
        LIMIT 1
        """.trimIndent(),
    ).use { c ->
        if (c.moveToFirst()) origin = c.getString(0)
    }

    val beacons = mutableListOf<BeaconState>()
    rows("SELECT id, beacon_order FROM main_beacons ORDER BY id") { c ->
        beacons += BeaconState(c.getString(0), c.getLong(1))
    }

    return HierarchyMigrationState(
        placements = placements,
        groupScopes = groupScopes,
        linkedAppearances = linkedAppearances,
        targetMappings = targetMappings,
        targetSubjects = targetSubjects,
        orientations = orientations,
        workspaces = workspaces,
        markerVersion = markerVersion,
        markerActivatedAt = markerActivatedAt,
        origin = origin,
        beacons = beacons,
    )
}


internal data class PlacementState(
    val id: String,
    val hierarchyId: String,
    val targetType: String,
    val targetId: String,
    val parentPlacementId: String?,
    val placementKind: String,
    val siblingOrder: Long,
    val isDeleted: Boolean,
    val version: Long,
)

internal data class GroupScopeState(
    val placementId: String,
    val hierarchyId: String,
    val groupSubjectId: String?,
    val isDeleted: Boolean,
    val version: Long,
)

internal data class LinkedAppearanceState(
    val placementId: String,
    val hierarchyId: String,
    val isDeleted: Boolean,
    val version: Long,
)

internal data class TargetMappingState(
    val sourceType: String,
    val sourceId: String,
    val subjectId: String,
    val migrationVersion: Int,
    val state: String,
    val isDeleted: Boolean,
    val version: Long,
)

internal data class TargetSubjectState(
    val id: String,
    val subjectType: String,
    val title: String,
    val description: String?,
    val isDeleted: Boolean,
    val version: Long,
)

internal data class OrientationState(
    val subjectId: String,
    val kind: String,
    val lifecycle: String?,
    val lifecycleOrigin: String,
)

internal data class WorkspaceState(
    val id: String,
    val name: String?,
    val provenance: String,
    val sourceContextId: String?,
    val isDeleted: Boolean,
)

internal data class BeaconState(
    val id: String,
    val order: Long,
)

internal const val MIGRATION_TEST_NOW = 50_000L
