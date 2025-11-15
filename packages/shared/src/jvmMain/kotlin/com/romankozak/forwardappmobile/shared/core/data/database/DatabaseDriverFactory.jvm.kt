package com.romankozak.forwardappmobile.shared.core.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
