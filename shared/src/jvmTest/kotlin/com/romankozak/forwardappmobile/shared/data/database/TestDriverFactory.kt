package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase

actual fun createTestDriver(): SqlDriver {
    val driver = TestDriver.driver
    try {
        driver.execute(null, "DELETE FROM projects;", 0)
        println("DELETE FROM projects executed successfully.")
        driver.execute(null, "DELETE FROM Goals;", 0)
        println("DELETE FROM Goals executed successfully.")
        driver.execute(null, "DELETE FROM ListItems;", 0)
        println("DELETE FROM ListItems executed successfully.")
    } catch (e: Exception) {
        println("Error executing DELETE: ${e.message}")
        e.printStackTrace()
    }
    return driver
}

actual fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase {
    return createForwardAppDatabase(driver)
}

actual fun closeTestDriver(driver: SqlDriver) {
    // Do nothing, the driver is closed by the shutdown hook
}

object TestDriver {
    val driver: SqlDriver by lazy {
        val driver = JdbcSqliteDriver("jdbc:sqlite:test.db")
        println("Creating test database schema...")
        try {
            ForwardAppDatabase.Schema.create(driver)
            println("Schema created successfully.")
        } catch (e: Exception) {
            println("Error creating schema: ${e.message}")
            e.printStackTrace()
        }
        driver
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            driver.close()
            java.io.File("test.db").delete()
        })
    }
}
