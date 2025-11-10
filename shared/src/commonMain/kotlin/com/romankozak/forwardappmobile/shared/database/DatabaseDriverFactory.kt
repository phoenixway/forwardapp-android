package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Абстракція, яку реалізують платформи.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}