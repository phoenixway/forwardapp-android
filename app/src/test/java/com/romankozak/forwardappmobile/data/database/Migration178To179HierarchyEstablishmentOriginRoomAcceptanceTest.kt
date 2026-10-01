package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration178To179HierarchyEstablishmentOriginRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `marker-less upgraded database is classified legacy even when H1 is empty`() =
        runBlocking {
            val database = migratedDatabase("migration_178_179_markerless_empty")
            try {
                assertEquals(
                    HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE.name,
                    origin(database),
                )
                assertNull(database.hierarchyAuthorityActivationStateDao().get(GENERAL))
            } finally {
                database.close()
            }
        }

    @Test
    fun `marker-less upgraded database remains legacy when deterministic H1 is present`() =
        runBlocking {
            val database =
                migratedDatabase(
                dbName = "migration_178_179_markerless_h1",
                seed = { db -> seedPlacement(db) },
                )
            try {
                assertEquals(
                    HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE.name,
                    origin(database),
                )
                assertEquals(1, database.hierarchyPlacementDao().getAll().size)
            } finally {
                database.close()
            }
        }

    @Test
    fun `version one marker classifies established and remains intact`() =
        runBlocking {
            assertMarkerClassifiesEstablished(version = 1)
        }

    @Test
    fun `version two marker classifies established and remains intact`() =
        runBlocking {
            assertMarkerClassifiesEstablished(version = 2)
        }

    @Test
    fun `fresh current schema is explicitly classified fresh native`() =
        runBlocking {
            val database =
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .addCallback(HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertEquals(HierarchyEstablishmentOrigin.FRESH_NATIVE.name, origin(database))
                assertNull(database.hierarchyAuthorityActivationStateDao().get(GENERAL))
            } finally {
                database.close()
            }
        }

    private suspend fun assertMarkerClassifiesEstablished(version: Int) {
        val activatedAt = 100L + version
        val database =
            migratedDatabase(
                dbName = "migration_178_179_marker_$version",
                seed = { db -> seedMarker(db, version, activatedAt) },
            )
        try {
            assertEquals(HierarchyEstablishmentOrigin.ESTABLISHED.name, origin(database))
            val marker =
                requireNotNull(database.hierarchyAuthorityActivationStateDao().get(GENERAL))
            assertEquals(version, marker.version)
            assertEquals(activatedAt, marker.activatedAt)
        } finally {
            database.close()
        }
    }

    private suspend fun origin(database: AppDatabase): String? =
        database.hierarchyEstablishmentOriginDao().get(GENERAL)?.origin

    private fun migratedDatabase(
        dbName: String,
        seed: (SupportSQLiteDatabase) -> Unit = {},
    ): AppDatabase {
        createFromExported178(dbName)
        openVersion178(dbName).use { helper -> seed(helper.writableDatabase) }
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_178_179)
            .allowMainThreadQueries()
            .build()
    }

    private fun seedMarker(
        db: SupportSQLiteDatabase,
        version: Int,
        activatedAt: Long,
    ) {
        db.execSQL(
            """
            INSERT INTO hierarchy_authority_activation_state(hierarchyId, version, activatedAt)
            VALUES('GENERAL', $version, $activatedAt)
            """.trimIndent(),
        )
    }

    private fun seedPlacement(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO hierarchy_placements(
                id, hierarchyId, targetType, targetId, parentPlacementId,
                placementKind, siblingOrder, createdAt, updatedAt, syncedAt,
                isDeleted, version
            ) VALUES(
                'existing-placement', 'GENERAL', 'WORKSPACE', 'existing-target', NULL,
                'PRIMARY', 0, 1, 1, NULL, 0, 1
            )
            """.trimIndent(),
        )
    }

    private fun createFromExported178(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema178().readText())
                .asJsonObject
                .getAsJsonObject("database")
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(178) {
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
            )
        helper.use { it.writableDatabase }
    }

    private fun openVersion178(dbName: String) =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(178) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        )

    private fun findSchema178(): File {
        val paths =
            listOf(
                "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/178.json",
                "schemas/com.romankozak.forwardappmobile.database.AppDatabase/178.json",
            )
        var current: File? = File(System.getProperty("user.dir")).absoluteFile
        while (current != null) {
            paths.forEach { relative ->
                File(current, relative).takeIf(File::isFile)?.let { return it }
            }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 178")
    }

    private companion object {
        const val GENERAL = "GENERAL"
    }
}
