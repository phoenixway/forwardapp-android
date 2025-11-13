package com.romankozak.forwardappmobile.shared.features.projects.data.db

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.features.projects.domain.model.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.data.database.models.StringList

object ProjectsAdapters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val stringListAdapter = object : ColumnAdapter<StringList, String> {
        override fun decode(databaseValue: String): StringList {
            if (databaseValue.isEmpty()) return emptyList()
            return json.decodeFromString(ListSerializer(String.serializer()), databaseValue)
        }

        override fun encode(value: StringList): String =
            json.encodeToString(ListSerializer(String.serializer()), value)
    }

    val relatedLinksListAdapter = object : ColumnAdapter<RelatedLinkList, String> {
        override fun decode(databaseValue: String): RelatedLinkList {
            if (databaseValue.isEmpty()) return emptyList()
            return json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
        }

        override fun encode(value: RelatedLinkList): String =
            json.encodeToString(ListSerializer(RelatedLink.serializer()), value)
    }

    val projectTypeAdapter = object : ColumnAdapter<ProjectType, String> {
        override fun decode(databaseValue: String): ProjectType =
            ProjectType.valueOf(databaseValue)

        override fun encode(value: ProjectType): String = value.name
    }

    val reservedGroupAdapter = object : ColumnAdapter<ReservedGroup, String> {
        override fun decode(databaseValue: String): ReservedGroup =
            ReservedGroup.fromString(databaseValue)
                ?: throw IllegalStateException("Unknown ReservedGroup: $databaseValue")

        override fun encode(value: ReservedGroup): String = value.groupName
    }
}
