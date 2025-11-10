package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:ForwardAppDatabase.db").also {
            try { ForwardAppDatabase.Schema.create(it) } catch (_: Exception) {}
        }
}