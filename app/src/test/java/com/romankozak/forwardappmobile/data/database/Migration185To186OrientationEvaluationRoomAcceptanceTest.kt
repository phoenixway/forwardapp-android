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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration185To186OrientationEvaluationRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 185 opens as 186 with neutral evaluation defaults`() {
        val dbName = "migration_185_186_orientation_evaluation"
        createFromExported185(dbName)
        seedExistingCanonicalAssessment(dbName)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_185_186)
                .allowMainThreadQueries()
                .build()

        try {
            val db = database.openHelper.writableDatabase

            assertEquals(186L, scalarLong(db, "PRAGMA user_version"))

            db.query(
                """
                SELECT
                    importanceValue,
                    importanceOrigin,
                    impactValue,
                    impactOrigin,
                    effortValue,
                    effortOrigin,
                    costValue,
                    costOrigin,
                    riskValue,
                    riskOrigin,
                    evaluationState
                FROM orientation_assessments
                WHERE orientationId = 'orientation-1'
                """.trimIndent(),
            ).use { cursor ->
                check(cursor.moveToFirst())

                assertEquals("HIGH", cursor.getString(0))
                assertEquals("EXPLICIT", cursor.getString(1))
                assertEquals("MEDIUM", cursor.getString(2))
                assertEquals("EXPLICIT", cursor.getString(3))

                assertNull(cursor.getString(4))
                assertEquals("UNSET", cursor.getString(5))
                assertNull(cursor.getString(6))
                assertEquals("UNSET", cursor.getString(7))
                assertNull(cursor.getString(8))
                assertEquals("UNSET", cursor.getString(9))
                assertEquals("UNASSESSED", cursor.getString(10))
            }

            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM orientation_assessments WHERE orientationId = 'orientation-1'",
                ),
            )
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
        }
    }

    private fun seedExistingCanonicalAssessment(dbName: String) {
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(185) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        ).use { helper ->
            val db = helper.writableDatabase

            db.execSQL(
                """
                INSERT INTO managed_subjects
                (
                    id,
                    createdAt,
                    updatedAt,
                    syncedAt,
                    isDeleted,
                    version,
                    subjectType,
                    title,
                    description
                )
                VALUES
                (
                    'orientation-1',
                    1,
                    2,
                    NULL,
                    0,
                    1,
                    'ORIENTATION',
                    'Existing orientation',
                    NULL
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO orientations
                (
                    subjectId,
                    kind,
                    lifecycle,
                    lifecycleOrigin
                )
                VALUES
                (
                    'orientation-1',
                    'GOAL',
                    'READY',
                    'DERIVED'
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO orientation_assessment_revisions
                (
                    id,
                    orientationId,
                    effectiveFrom,
                    recordedAt,
                    source,
                    reason,
                    assessmentJson,
                    createdAt,
                    updatedAt,
                    syncedAt,
                    isDeleted,
                    version
                )
                VALUES
                (
                    'revision-1',
                    'orientation-1',
                    1,
                    1,
                    'MIGRATION',
                    'Existing v185 revision',
                    '{}',
                    1,
                    2,
                    NULL,
                    0,
                    1
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO orientation_assessments
                (
                    orientationId,
                    revisionId,
                    importanceValue,
                    importanceOrigin,
                    impactValue,
                    impactOrigin,
                    breadthValue,
                    breadthOrigin,
                    expectedSpanValue,
                    expectedSpanOrigin,
                    targetWindowValue,
                    targetWindowOrigin,
                    attentionTierValue,
                    attentionTierOrigin,
                    commitmentValue,
                    commitmentOrigin,
                    confidenceValue,
                    confidenceOrigin,
                    provenanceJson,
                    createdAt,
                    updatedAt,
                    syncedAt,
                    isDeleted,
                    version
                )
                VALUES
                (
                    'orientation-1',
                    'revision-1',
                    'HIGH',
                    'EXPLICIT',
                    'MEDIUM',
                    'EXPLICIT',
                    NULL,
                    'UNSET',
                    NULL,
                    'UNSET',
                    NULL,
                    'UNSET',
                    NULL,
                    'UNSET',
                    NULL,
                    'UNSET',
                    NULL,
                    'UNSET',
                    NULL,
                    1,
                    2,
                    NULL,
                    0,
                    1
                )
                """.trimIndent(),
            )
        }
    }

    private fun createFromExported185(dbName: String) {
        context.deleteDatabase(dbName)

        val schema =
            JsonParser.parseString(findSchema185().readText())
                .asJsonObject
                .getAsJsonObject("database")

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(185) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            schema.getAsJsonArray("entities").forEach { element ->
                                val entity = element.asJsonObject
                                val table = entity.get("tableName").asString

                                db.execSQL(
                                    entity
                                        .get("createSql")
                                        .asString
                                        .replace("\${TABLE_NAME}", table),
                                )

                                entity.getAsJsonArray("indices")?.forEach { index ->
                                    db.execSQL(
                                        index
                                            .asJsonObject
                                            .get("createSql")
                                            .asString
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

    private fun findSchema185(): File {
        var current: File? =
            File(requireNotNull(System.getProperty("user.dir"))).absoluteFile

        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/185.json"

        while (current != null) {
            File(current, relative)
                .takeIf(File::isFile)
                ?.let { return it }

            current = current.parentFile
        }

        error("Unable to locate exported Room schema 185")
    }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            requireNotNull(cursor.getString(0))
        }
}
