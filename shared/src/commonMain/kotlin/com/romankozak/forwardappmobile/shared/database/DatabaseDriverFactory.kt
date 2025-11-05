package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific configuration needed to create a SQLDelight driver.
 */
expect abstract class PlatformContext

/**
 * Factory that creates a platform-specific SQLDelight driver.
 *
 * A `PlatformContext` can provide additional information (for example, the Android `Context`).
 */
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

/**
 * Helper for building the shared ForwardApp database from a provided driver factory.
 */
fun createForwardAppDatabase(
    driverFactory: DatabaseDriverFactory,
): ForwardAppDatabase = ForwardAppDatabase(driverFactory.createDriver())
