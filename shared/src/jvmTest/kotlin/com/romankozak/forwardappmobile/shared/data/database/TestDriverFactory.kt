package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase

actual fun createTestDriver(): SqlDriver {
    val driver = JdbcSqliteDriver("jdbc:sqlite:test.db")
    println("Creating test database schema...")
    try {
        ForwardAppDatabase.Schema.create(driver)
        println("Schema created successfully.")
        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type='table';",
            parameters = 0,
            binders = null
        )
        println("Result: $result")
    } catch (e: Exception) {
        println("Error creating schema or executing query: ${e.message}")
        e.printStackTrace()
    }
    return driver
}

actual fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase {
    return createForwardAppDatabase(driver)
}

actual fun closeTestDriver(driver: SqlDriver) {
    driver.close()
    java.io.File("test.db").delete()
}
