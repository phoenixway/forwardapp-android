package com.romankozak.forwardappmobile.shared.core.data.database

import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

// 🔹 JVM реалізація: контекст не потрібен
actual abstract class PlatformContext

actual class DatabaseDriverFactory actual constructor(
    platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}