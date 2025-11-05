package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

actual class PlatformContext

/**
 * TODO: Provide a real SQLDelight driver for JS (e.g. sql.js or web worker driver).
 */
actual class DatabaseDriverFactory actual constructor(
    platformContext: PlatformContext?,
) {
    actual fun createDriver(): SqlDriver {
        error("SQLDelight driver for JS is not implemented yet.")
    }
}
