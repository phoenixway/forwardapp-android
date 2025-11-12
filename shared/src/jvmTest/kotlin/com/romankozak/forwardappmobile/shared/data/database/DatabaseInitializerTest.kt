import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase // Added import
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.database.doubleAdapter
import com.romankozak.forwardappmobile.shared.database.longAdapter
import com.romankozak.forwardappmobile.shared.database.projectTypeAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.database.reservedGroupAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter

import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase

actual fun createTestDriver(): SqlDriver {
    val driver = JdbcSqliteDriver("jdbc:sqlite:test.db")
    println("Creating DB schema...")
    try {
        ForwardAppDatabase.Schema.create(driver)
        println("Schema created successfully")
        println("Diagnostic query skipped for now.")
    } catch (e: Exception) {
        println("Error creating schema: ${e.message}")
        e.printStackTrace()
    }
    return driver
}

actual fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase {
    return createForwardAppDatabase(driver)
}

actual fun closeTestDriver(driver: SqlDriver) {
    driver.close()
    // Delete the test database file
    val file = java.io.File("test.db")
    if (file.exists()) {
        file.delete()
    }
}
