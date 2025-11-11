
package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TestAdapters {
    val stringAdapter = object : ColumnAdapter<String, String> {
        override fun decode(databaseValue: String) = databaseValue
        override fun encode(value: String) = value
    }

    val longAdapter = object : ColumnAdapter<Long, Long> {
        override fun decode(databaseValue: Long) = databaseValue
        override fun encode(value: Long) = value
    }

    val doubleAdapter = object : ColumnAdapter<Double, Double> {
        override fun decode(databaseValue: Double) = databaseValue
        override fun encode(value: Double) = value
    }

    val stringListAdapter = object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String): List<String> {
            return if (databaseValue.isEmpty()) emptyList()
            else Json.decodeFromString(databaseValue)
        }
        override fun encode(value: List<String>): String {
            return Json.encodeToString(value)
        }
    }

    val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
        override fun decode(databaseValue: String): List<RelatedLink> {
            return if (databaseValue.isEmpty()) emptyList()
            else Json.decodeFromString(databaseValue)
        }
        override fun encode(value: List<RelatedLink>): String {
            return Json.encodeToString(value)
        }
    }

    val projectTypeAdapter = object : ColumnAdapter<ProjectType, String> {
        override fun decode(databaseValue: String) = ProjectType.valueOf(databaseValue)
        override fun encode(value: ProjectType) = value.name
    }

    val reservedGroupAdapter = object : ColumnAdapter<ReservedGroup, String> {
        override fun decode(databaseValue: String): ReservedGroup =
            ReservedGroup.fromString(databaseValue)
                ?: throw IllegalStateException("Unknown reserved group: $databaseValue")
        override fun encode(value: ReservedGroup): String = value.groupName
    }
}
