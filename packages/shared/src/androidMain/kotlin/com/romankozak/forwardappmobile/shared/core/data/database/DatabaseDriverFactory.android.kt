package com.romankozak.forwardappmobile.shared.core.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val ctx = platformContext ?: error("Android Context required")

        val factory = FrameworkSQLiteOpenHelperFactory()

        return AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = ctx,
            name = "ForwardAppDatabase.db",
            factory = { config ->
                factory.create(config)
            }
        )
    }
}
