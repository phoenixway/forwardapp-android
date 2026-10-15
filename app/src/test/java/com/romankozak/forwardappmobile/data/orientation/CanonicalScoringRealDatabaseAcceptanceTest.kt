package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.data.database.ALL_MIGRATIONS
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Explicit HOST-only acceptance against a copied production SQLite database.
 *
 * No production data is checked into the repository.
 * Input files are read, never opened by Room or modified.
 */
@RunWith(RobolectricTestRunner::class)
class CanonicalScoringRealDatabaseAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `real historical database activates scoring without rewriting immutable history`() =
        runBlocking {
            val sourceDirectory =
                requireNotNull(
                    System.getenv("FORWARDAPP_REAL_DB_DIR")
                        ?.takeIf { it.isNotBlank() },
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
                        require(file.isFile && !java.nio.file.Files.isSymbolicLink(file.toPath())) {
                            "Real database input is missing or is a symlink"
                        }
                    }
                }

            val sourceHashes = sourceFiles.associateWith(::sha256)
            val dbName = "r11_real_database_${UUID.randomUUID()}"
            val target = context.getDatabasePath(dbName)
            require(target.parentFile?.mkdirs() == true || target.parentFile?.isDirectory == true)
            require(sourceFiles.none { it.canonicalFile == target.canonicalFile })

            var database: AppDatabase? = null

            try {
                sourceFiles.forEach { file ->
                    val suffix = file.name.removePrefix("forward_app_database")
                    file.copyTo(File(target.path + suffix), overwrite = false)
                }

                val db =
                    Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                        .addMigrations(*ALL_MIGRATIONS)
                        .allowMainThreadQueries()
                        .build()
                database = db

                val dao = db.orientationDao()
                val markerDao = db.canonicalScoringAuthorityStateDao()

                assertNull(
                    "Real PROD copy already has a scoring activation marker",
                    markerDao.get(),
                )
                assertTrue(
                    "Real PROD copy already contains scoring overrides",
                    dao.getAllScoringOverrides().isEmpty(),
                )

                val historicalRevisions =
                    dao.getAllAssessmentRevisions().associateBy { it.id }
                val originalAssessmentCount = dao.getAllAssessments().size

                assertTrue("Expected historical revisions", historicalRevisions.isNotEmpty())
                assertTrue("Expected historical assessments", originalAssessmentCount > 0)

                val activator =
                    CanonicalScoringAuthorityActivator(
                        db,
                        dao,
                        db.goalDao(),
                        db.contextDao(),
                        markerDao,
                    )

                val first = activator.ensureActivated(now = 1_800_000_000_000L)

                assertTrue("First activation must execute", first.performed)
                assertTrue("Expected canonical scoring targets", first.orientations > 0)
                assertNotNull("Activation marker missing", markerDao.get())

                val afterRevisions =
                    dao.getAllAssessmentRevisions().associateBy { it.id }

                assertTrue(
                    "One or more immutable historical revisions changed",
                    historicalRevisions.all { (id, revision) ->
                        afterRevisions[id] == revision
                    },
                )

                assertTrue(
                    "Activation lost current assessments",
                    dao.getAllAssessments().size >= originalAssessmentCount,
                )

                val overridesAfterFirst = dao.getAllScoringOverrides().toSet()
                assertTrue("No scoring overrides were created", overridesAfterFirst.isNotEmpty())

                val second = activator.ensureActivated(now = 1_800_000_000_001L)

                assertFalse("Second activation must be inert", second.performed)
                assertEquals(first.orientations, second.orientations)

                assertTrue(
                    "Repeat activation changed scoring overrides",
                    dao.getAllScoringOverrides().toSet() == overridesAfterFirst,
                )

                assertTrue(
                    "Repeat activation changed immutable historical revisions",
                    historicalRevisions.all { (id, revision) ->
                        dao.getAllAssessmentRevisions()
                            .any { it.id == id && it == revision }
                    },
                )
            } finally {
                database?.close()
                context.deleteDatabase(dbName)

                sourceHashes.forEach { (file, expectedHash) ->
                    assertEquals(
                        "A production-copy input file was modified",
                        expectedHash,
                        sha256(file),
                    )
                }
            }
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
