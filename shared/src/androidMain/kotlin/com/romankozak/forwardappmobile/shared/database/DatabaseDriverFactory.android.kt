package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver

// ✅ цей alias тепер сумісний з expect interface
actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            ForwardAppDatabase.Schema,
            platformContext,
            "ForwardAppDatabase.db"
        )
}

