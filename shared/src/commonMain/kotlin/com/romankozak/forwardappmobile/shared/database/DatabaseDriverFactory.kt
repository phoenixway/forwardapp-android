package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.adapters.RelatedLinkAdapter

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

val relatedLinkAdapter = object : ColumnAdapter<RelatedLink, String> {
    override fun decode(databaseValue: String): RelatedLink = Json.decodeFromString(databaseValue)
    override fun encode(value: RelatedLink): String = Json.encodeToString(value)
}
val dayStatusAdapter = object : ColumnAdapter<DayStatus, String> {
    override fun decode(databaseValue: String): DayStatus = DayStatus.valueOf(databaseValue)
    override fun encode(value: DayStatus): String = value.name
}
val taskPriorityAdapter = object : ColumnAdapter<TaskPriority, String> {
    override fun decode(databaseValue: String): TaskPriority = TaskPriority.valueOf(databaseValue)
    override fun encode(value: TaskPriority): String = value.name
}
val taskStatusAdapter = object : ColumnAdapter<TaskStatus, String> {
    override fun decode(databaseValue: String): TaskStatus = TaskStatus.valueOf(databaseValue)
    override fun encode(value: TaskStatus): String = value.name
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue != 0L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}

/**
 * Helper for building the shared ForwardApp database from a provided driver factory.
 */
fun createForwardAppDatabase(
    driverFactory: DatabaseDriverFactory,
): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driverFactory.createDriver(),
        LinkItemsAdapter = LinkItems.Adapter(linkDataAdapter = relatedLinkAdapter),
        GoalsAdapter = Goals.Adapter(
            relatedLinksAdapter = RelatedLinkAdapter,
            completedAdapter = booleanAdapter
        ),
        DayPlansAdapter = DayPlans.Adapter(statusAdapter = dayStatusAdapter),
        DayTasksAdapter = DayTasks.Adapter(
            priorityAdapter = taskPriorityAdapter,
            statusAdapter = taskStatusAdapter
        )
    )
}