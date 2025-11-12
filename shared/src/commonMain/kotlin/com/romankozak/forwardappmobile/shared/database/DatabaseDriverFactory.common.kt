package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory(platformContext: Any? = null) {
    fun createDriver(): SqlDriver
}

