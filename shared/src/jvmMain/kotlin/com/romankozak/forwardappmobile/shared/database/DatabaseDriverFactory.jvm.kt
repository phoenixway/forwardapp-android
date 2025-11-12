package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory actual constructor(
    platformContext: Any?
) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
