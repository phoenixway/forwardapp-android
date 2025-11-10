package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

// ✅ Простий очікуваний клас — обгортка для будь-якого контексту
expect class PlatformContext

expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}

