package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

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

val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long): Long = databaseValue
    override fun encode(value: Long): Long = value
}

val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double): Double = databaseValue
    override fun encode(value: Double): Double = value
}

val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()
    override fun encode(value: Int): Long = value.toLong()
}

@OptIn(InternalSerializationApi::class)
val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        if (databaseValue.isEmpty()) return emptyList()
        return Json.decodeFromString(ListSerializer(RelatedLink::class.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String {
        return Json.encodeToString(ListSerializer(RelatedLink::class.serializer()), value)
    }
}

@OptIn(InternalSerializationApi::class)
val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        if (databaseValue.isEmpty()) return emptyList()
        return Json.decodeFromString(ListSerializer(String::class.serializer()), databaseValue)
    }

    override fun encode(value: List<String>): String {
        return Json.encodeToString(ListSerializer(String::class.serializer()), value)
    }
}

val projectTypeAdapter = object : ColumnAdapter<ProjectType, String> {
    override fun decode(databaseValue: String): ProjectType = ProjectType.fromString(databaseValue)
    override fun encode(value: ProjectType): String = value.name
}

val reservedGroupAdapter = object : ColumnAdapter<ReservedGroup, String> {
    override fun decode(databaseValue: String): ReservedGroup {
        return ReservedGroup.fromString(databaseValue) ?: throw IllegalStateException("Unknown reserved group: $databaseValue")
    }
    override fun encode(value: ReservedGroup): String = value.groupName
}

fun createForwardAppDatabase(
    driverFactory: DatabaseDriverFactory,
): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driverFactory.createDriver(),
        ProjectsAdapter = Projects.Adapter(
            createdAtAdapter = longAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            orderAdapter = longAdapter,
            valueImportanceAdapter = doubleAdapter,
            valueImpactAdapter = doubleAdapter,
            effortAdapter = doubleAdapter,
            costAdapter = doubleAdapter,
            riskAdapter = doubleAdapter,
            weightEffortAdapter = doubleAdapter,
            weightCostAdapter = doubleAdapter,
            weightRiskAdapter = doubleAdapter,
            rawScoreAdapter = doubleAdapter,
            displayScoreAdapter = intAdapter,
            projectTypeAdapter = projectTypeAdapter,
            reservedGroupAdapter = reservedGroupAdapter
        ),
        GoalsAdapter = Goals.Adapter(
            createdAtAdapter = longAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            valueImportanceAdapter = doubleAdapter,
            valueImpactAdapter = doubleAdapter,
            effortAdapter = doubleAdapter,
            costAdapter = doubleAdapter,
            riskAdapter = doubleAdapter,
            weightEffortAdapter = doubleAdapter,
            weightCostAdapter = doubleAdapter,
            weightRiskAdapter = doubleAdapter,
            rawScoreAdapter = doubleAdapter,
            displayScoreAdapter = intAdapter
        ),
        ListItemsAdapter = ListItems.Adapter(
            orderAdapter = longAdapter
        )
    )
}
