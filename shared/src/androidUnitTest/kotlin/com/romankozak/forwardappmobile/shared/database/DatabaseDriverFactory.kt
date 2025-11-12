package com.romankozak.forwardappmobile.shared.database

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return AndroidSqliteDriver(ForwardAppDatabase.Schema, app, "test.db")
    }
}
