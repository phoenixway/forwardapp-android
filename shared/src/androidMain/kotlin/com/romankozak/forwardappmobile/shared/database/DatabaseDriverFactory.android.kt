package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?,
) {
    actual fun createDriver(): SqlDriver {
        val context = requireNotNull(platformContext) {
            "Android context is required to create the SQLDelight driver."
        }
        return AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = context,
            name = DATABASE_NAME,
        )
    }

    private companion object {
        const val DATABASE_NAME = "forward_app_database"
    }
}
