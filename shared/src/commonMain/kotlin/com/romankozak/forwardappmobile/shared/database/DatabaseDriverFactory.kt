package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

// ✅ має бути interface, а не class
expect interface PlatformContext

expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

