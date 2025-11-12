package com.romankozak.forwardappmobile.shared.core.data.database

import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

// 🔹 Android реалізація: просто alias на Context
actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val ctx = platformContext ?: error("Android Context required")
        return AndroidSqliteDriver(ForwardAppDatabase.Schema, ctx, "ForwardAppDatabase.db")
    }
}