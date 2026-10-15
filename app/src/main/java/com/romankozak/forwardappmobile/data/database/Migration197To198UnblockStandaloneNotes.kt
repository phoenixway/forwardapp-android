package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Context-FK.10A: physical storage unblocking for standalone Legacy Notes.
 *
 * Schema 197 -> 198.
 *
 * Nullable contextId is historical compatibility evidence only.
 * It is not canonical ownership, a Workspace, or an Orientation subject.
 *
 * Room owns the migration transaction. FK.10B owns runtime convergence.
 */
val MIGRATION_197_198 =
    object : Migration(197, 198) {
        override fun migrate(db: SupportSQLiteDatabase) {
            requireSource197198(db)
            requireForeignKeysClean197198(db)

            val originalCount =
                scalar197198(db, "SELECT COUNT(*) FROM `notes`")

            // External-content FTS4 must be rebuilt after swapping notes.
            db.execSQL("DROP TABLE `notes_fts`")

            db.execSQL(
                """
                CREATE TABLE `fk10a_notes_new` (
                    `id` TEXT NOT NULL,
                    `contextId` TEXT,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            val columns =
                listOf(
                    "id", "contextId", "title", "content",
                    "createdAt", "updatedAt", "syncedAt",
                    "isDeleted", "version",
                ).joinToString(", ") { "`$it`" }

            db.execSQL(
                """
                INSERT INTO `fk10a_notes_new` ($columns)
                SELECT $columns FROM `notes`
                """.trimIndent(),
            )

            check(
                scalar197198(
                    db,
                    "SELECT COUNT(*) FROM `fk10a_notes_new`",
                ) == originalCount
            ) {
                "FK.10A changed historical Note row count"
            }

            // Exact payload preservation in both directions, including
            // NULL metadata, tombstones, version and historical Context ID.
            for ((source, target) in listOf(
                "notes" to "fk10a_notes_new",
                "fk10a_notes_new" to "notes",
            )) {
                check(
                    scalar197198(
                        db,
                        """
                        SELECT COUNT(*) FROM (
                            SELECT $columns FROM `$source`
                            EXCEPT
                            SELECT $columns FROM `$target`
                        )
                        """.trimIndent(),
                    ) == 0L
                ) {
                    "FK.10A changed historical Note payload"
                }
            }

            db.execSQL("DROP TABLE `notes`")
            db.execSQL(
                "ALTER TABLE `fk10a_notes_new` RENAME TO `notes`"
            )

            db.execSQL(
                """
                CREATE INDEX `index_notes_contextId`
                ON `notes` (`contextId`)
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE VIRTUAL TABLE `notes_fts`
                USING FTS4(
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    content=`notes`
                )
                """.trimIndent(),
            )

            db.execSQL(
                "INSERT INTO `notes_fts`(`notes_fts`) VALUES('rebuild')"
            )

            check(
                scalar197198(
                    db,
                    "SELECT COUNT(*) FROM `notes`",
                ) == originalCount
            ) {
                "FK.10A lost Note rows during table swap"
            }

            requireTarget197198(db)
            requireForeignKeysClean197198(db)
            requireIntegrityClean197198(db)
        }
    }

private fun requireSource197198(db: SupportSQLiteDatabase) {
    val expected =
        setOf(
            "id", "contextId", "title", "content",
            "createdAt", "updatedAt", "syncedAt",
            "isDeleted", "version",
        )

    val columns = noteColumns197198(db)

    check(columns.keys == expected && columns["contextId"] == 1) {
        "FK.10A unexpected source Note schema"
    }

    db.query("PRAGMA foreign_key_list(`notes`)").use { cursor ->
        val table = cursor.getColumnIndexOrThrow("table")
        val from = cursor.getColumnIndexOrThrow("from")
        val to = cursor.getColumnIndexOrThrow("to")
        val onDelete = cursor.getColumnIndexOrThrow("on_delete")

        check(cursor.moveToFirst()) {
            "FK.10A source Note FK missing"
        }

        check(
            cursor.getString(table) == "contexts" &&
                cursor.getString(from) == "contextId" &&
                cursor.getString(to) == "id" &&
                cursor.getString(onDelete) == "CASCADE"
        ) {
            "FK.10A unexpected source Context FK"
        }

        check(!cursor.moveToNext()) {
            "FK.10A unexpected additional source Note FK"
        }
    }

    check(
        scalar197198(
            db,
            """
            SELECT COUNT(*) FROM sqlite_master
            WHERE type = 'table' AND name = 'notes_fts'
            """.trimIndent(),
        ) == 1L
    ) {
        "FK.10A source FTS missing"
    }
}

private fun requireTarget197198(db: SupportSQLiteDatabase) {
    val columns = noteColumns197198(db)

    check(columns["contextId"] == 0) {
        "FK.10A historical locator is not nullable"
    }

    db.query("PRAGMA foreign_key_list(`notes`)").use { cursor ->
        check(!cursor.moveToFirst()) {
            "FK.10A Note still has a physical FK"
        }
    }

    check(
        scalar197198(
            db,
            """
            SELECT COUNT(*) FROM sqlite_master
            WHERE type = 'index'
              AND name = 'index_notes_contextId'
              AND tbl_name = 'notes'
            """.trimIndent(),
        ) == 1L
    ) {
        "FK.10A locator index missing"
    }

    check(
        scalar197198(
            db,
            """
            SELECT COUNT(*) FROM sqlite_master
            WHERE type = 'table' AND name = 'notes_fts'
            """.trimIndent(),
        ) == 1L
    ) {
        "FK.10A FTS missing after rebuild"
    }
}

private fun noteColumns197198(
    db: SupportSQLiteDatabase,
): Map<String, Int> {
    val result = mutableMapOf<String, Int>()

    db.query("PRAGMA table_info(`notes`)").use { cursor ->
        val name = cursor.getColumnIndexOrThrow("name")
        val notNull = cursor.getColumnIndexOrThrow("notnull")

        while (cursor.moveToNext()) {
            result[cursor.getString(name)] = cursor.getInt(notNull)
        }
    }

    return result
}

private fun requireForeignKeysClean197198(db: SupportSQLiteDatabase) {
    db.query("PRAGMA foreign_key_check").use { cursor ->
        check(!cursor.moveToFirst()) {
            "FK.10A foreign_key_check failed"
        }
    }
}

private fun requireIntegrityClean197198(db: SupportSQLiteDatabase) {
    db.query("PRAGMA integrity_check").use { cursor ->
        check(cursor.moveToFirst() && cursor.getString(0) == "ok") {
            "FK.10A integrity_check failed"
        }
    }
}

private fun scalar197198(
    db: SupportSQLiteDatabase,
    sql: String,
): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst()) {
            "FK.10A scalar query returned no rows"
        }
        cursor.getLong(0)
    }
