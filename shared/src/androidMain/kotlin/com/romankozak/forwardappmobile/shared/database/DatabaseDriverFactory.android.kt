package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver

// ✅ Обгортка, щоб тип збігався
actual class PlatformContext(val context: Context)

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = platformContext?.context!!,
            name = "ForwardAppDatabase.db"
        )
}

