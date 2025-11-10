package com.romankozak.forwardappmobile.shared.data.database.adapter

import app.cash.sqldelight.ColumnAdapter

class ListToStringAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        return if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            databaseValue.split(",")
        }
    }

    override fun encode(value: List<String>): String {
        return value.joinToString(",")
    }
}
