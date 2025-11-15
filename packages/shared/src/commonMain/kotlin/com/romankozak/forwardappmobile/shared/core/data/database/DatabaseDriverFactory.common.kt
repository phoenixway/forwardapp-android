package com.romankozak.forwardappmobile.shared.core.data.database

import app.cash.sqldelight.db.SqlDriver

// 🔹 оголошення "порожнього" типу, який кожна платформа реалізує по-своєму
expect abstract class PlatformContext

// 🔹 дефолтний аргумент вказується тільки тут
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}
