package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: Any? = null
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ForwardAppDatabase.Schema, platformContext as Context, "ForwardAppDatabase.db")
}


