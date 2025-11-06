package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?,
) {
    actual fun createDriver(): SqlDriver {
        val context = requireNotNull(platformContext) {
            "Android context is required to create the SQLDelight driver."
        }
        val schema = ForwardAppDatabase.Schema
        return AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = DATABASE_NAME,
            callback =
                object : AndroidSqliteDriver.Callback(schema) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        super.onConfigure(db)
                        db.query("PRAGMA busy_timeout = 5000").close()
                        db.query("PRAGMA journal_mode = WAL").close()
                    }

                    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Room already manages migrations. We simply keep existing schema.
                        // No-op to avoid crashes when SQLDelight opens Room's database.
                    }
                },
        )
    }

    private companion object {
        const val DATABASE_NAME = "forward_app_database"
    }
}
