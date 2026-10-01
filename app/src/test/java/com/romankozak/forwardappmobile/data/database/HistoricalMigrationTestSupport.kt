package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

internal fun openHistoricalMigrationDatabase(
    context: Context,
    dbName: String,
    vararg migrations: Migration,
): SupportSQLiteOpenHelper {
    require(migrations.isNotEmpty()) {
        "At least one migration is required"
    }

    val ordered = migrations.toList()
    ordered.zipWithNext().forEach { (current, next) ->
        require(current.endVersion == next.startVersion) {
            "Non-contiguous historical migration chain: " +
                "${current.startVersion}->${current.endVersion}, " +
                "${next.startVersion}->${next.endVersion}"
        }
    }

    val expectedStartVersion = ordered.first().startVersion
    val targetVersion = ordered.last().endVersion

    return FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(targetVersion) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }

                    override fun onCreate(db: SupportSQLiteDatabase) =
                        error(
                            "Historical fixture must already exist at schema " +
                                expectedStartVersion,
                        )

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {
                        check(
                            oldVersion == expectedStartVersion &&
                                newVersion == targetVersion,
                        ) {
                            "Unexpected historical migration " +
                                "$oldVersion->$newVersion; expected " +
                                "$expectedStartVersion->$targetVersion"
                        }

                        ordered.forEach { migration ->
                            migration.migrate(db)
                        }
                    }
                },
            ).build(),
    )
}
