package com.romankozak.forwardappmobile.shared.core.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

// На JVM контекст не потрібен
actual typealias PlatformContext = Unit

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        // Тестовий файлик для JVM
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
