package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

expect interface PlatformContext

expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

