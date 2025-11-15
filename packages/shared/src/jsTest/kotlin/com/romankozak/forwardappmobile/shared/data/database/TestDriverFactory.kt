package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import org.w3c.dom.Worker

actual fun createTestDriver(): SqlDriver {
    return WebWorkerDriver(
        Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""))
    )
}

actual fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase {
    ForwardAppDatabase.Schema.create(driver)
    return createForwardAppDatabase(driver)
}

actual fun closeTestDriver(driver: SqlDriver) {
    driver.close()
}
