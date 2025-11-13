package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter

inline fun <reified T : Enum<T>> EnumColumnAdapter() = object : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String): T {
        return enumValueOf<T>(databaseValue)
    }

    override fun encode(value: T): String {
        return value.name
    }
}
