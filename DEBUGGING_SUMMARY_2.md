# Debugging Summary 2

This document consolidates the content of all relevant files and the latest compilation errors for debugging the `expect`/`actual` mechanism and test failures.

---

## File Contents

--- FILE: shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.common.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

// 🔹 оголошення "порожнього" типу, який кожна платформа реалізує по-своєму
expect abstract class PlatformContext

// 🔹 дефолтний аргумент вказується тільки тут
expect class DatabaseDriverFactory(platformContext: PlatformContext? = null) {
    fun createDriver(): SqlDriver
}
```

--- FILE: shared/src/androidMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.android.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

// 🔹 Android реалізація: просто alias на Context
actual typealias PlatformContext = Context

actual class DatabaseDriverFactory actual constructor(
    private val platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val ctx = platformContext ?: error("Android Context required")
        return AndroidSqliteDriver(ForwardAppDatabase.Schema, ctx, "ForwardAppDatabase.db")
    }
}
```

--- FILE: shared/src/jvmMain/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.jvm.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

// 🔹 JVM реалізація: контекст не потрібен
actual abstract class PlatformContext

actual class DatabaseDriverFactory actual constructor(
    platformContext: PlatformContext?
) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ForwardAppDatabase.Schema.create(driver)
        return driver
    }
}
```

--- FILE: shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// ------------------------------------------------------
// 🔹 Конфігурація JSON
// ------------------------------------------------------

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ------------------------------------------------------
// 🔹 Базові адаптери
// ------------------------------------------------------

val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double) = databaseValue
    override fun encode(value: Double) = value
}

val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long) = databaseValue.toInt()
    override fun encode(value: Int) = value.toLong()
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long) = databaseValue != 0L
    override fun encode(value: Boolean) = if (value) 1L else 0L
}

val stringAdapter = object : ColumnAdapter<String, String> {
    override fun decode(databaseValue: String) = databaseValue
    override fun encode(value: String) = value
}

// ------------------------------------------------------
// 🔹 JSON-адаптери
// ------------------------------------------------------

val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(String.serializer()), databaseValue)
    }

    override fun encode(value: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), value)
}

val relatedLinksListAdapter = object : ColumnAdapter<List<RelatedLink>, String> {
    override fun decode(databaseValue: String): List<RelatedLink> {
        if (databaseValue.isEmpty()) return emptyList()
        return json.decodeFromString(ListSerializer(RelatedLink.serializer()), databaseValue)
    }

    override fun encode(value: List<RelatedLink>): String =
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
            ?: throw IllegalStateException("Unknown reserved group: $databaseValue")
    override fun encode(value: ReservedGroup): String = value.groupName
}

// ------------------------------------------------------
// 🔹 Фабрика створення бази
// ------------------------------------------------------

fun createForwardAppDatabase(driver: SqlDriver): ForwardAppDatabase {
    println("Creating database schema...")
    try {
        ForwardAppDatabase.Schema.create(driver)
        println("Database schema created successfully.")
    } catch (e: Exception) {
        println("Error creating database schema: ${e.message}")
        e.printStackTrace()
        throw e
    }

    val goalsAdapter = Goals.Adapter(
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
        displayScoreAdapter = longAdapter
    )

    val projectsAdapter = Projects.Adapter(
        createdAtAdapter = longAdapter,
        goalOrderAdapter = longAdapter,
        tagsAdapter = stringListAdapter,
        relatedLinksAdapter = relatedLinksListAdapter,
        projectTypeAdapter = projectTypeAdapter,
        reservedGroupAdapter = reservedGroupAdapter,
        valueImportanceAdapter = doubleAdapter,
        valueImpactAdapter = doubleAdapter,
        effortAdapter = doubleAdapter,
        costAdapter = doubleAdapter,
        riskAdapter = doubleAdapter,
        weightEffortAdapter = doubleAdapter,
        weightCostAdapter = doubleAdapter,
        weightRiskAdapter = doubleAdapter,
        rawScoreAdapter = doubleAdapter,
        displayScoreAdapter = longAdapter
    )

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = goalsAdapter,
        projectsAdapter = projectsAdapter
    )
}
```

--- FILE: shared/src/commonTest/kotlin/com/romankozak/forwardappmobile/shared/data/database/DatabaseInitializerTest.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.*

expect fun createTestDriver(): SqlDriver
expect fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase
expect fun closeTestDriver(driver: SqlDriver)

class DatabaseInitializerTest {

    private lateinit var db: ForwardAppDatabase
    private lateinit var driver: SqlDriver
    private lateinit var initializer: DatabaseInitializer

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        db = createTestDatabase(driver)
        initializer = DatabaseInitializer(db)
    }

    @AfterTest
    fun tearDown() {
        closeTestDriver(driver)
    }

    @Test
    fun `initialize creates special projects when database is empty`() = runTest {
        try {
            initializer.initialize()
        } catch (e: Exception) {
            val logFile = java.io.File("/home/romankozak/.gemini/tmp/f0d5f14bc037e204b853dd06d685bc18bd58df1bfcb9976e722e3e26d7d98360/test_error.log")
            logFile.writeText("SQLiteException: ${e.message}\nStackTrace: ${e.stackTraceToString()}")
            throw e
        }

        val specialProject = db.projectsQueries.getProjectsByType(ProjectType.SYSTEM).executeAsOneOrNull()
        assertNotNull(specialProject)
        assertEquals("special", specialProject.name)

        val inbox = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.Inbox).executeAsOneOrNull()
        assertNotNull(inbox)
        assertEquals(specialProject.id, inbox.parentId)

        val strategicGroup = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.StrategicGroup).executeAsOneOrNull()
        assertNotNull(strategicGroup)
        assertEquals(specialProject.id, strategicGroup.parentId)

        val mainBeaconsGroup = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.MainBeaconsGroup).executeAsOneOrNull()
        assertNotNull(mainBeaconsGroup)
        assertEquals("main-beacon-realization", mainBeaconsGroup.name)
        assertEquals(specialProject.id, mainBeaconsGroup.parentId)

        val listProject = db.projectsQueries.getProjectById("main-beacon-list-id").executeAsOneOrNull()
        assertNotNull(listProject)
        assertEquals("list", listProject.name)
        assertEquals(mainBeaconsGroup.id, listProject.parentId)

        val missionProject = db.projectsQueries.getProjectsByReservedGroup(ReservedGroup.MainBeacons).executeAsOneOrNull()
        assertNotNull(missionProject)
        assertEquals("mission", missionProject.name)
        assertEquals(listProject.id, missionProject.parentId)
    }

    @Test
    fun `initialize does not create duplicates`() = runTest {
        initializer.initialize()
        val countAfterFirstInit = db.projectsQueries.getAllProjectsUnordered().executeAsList().size

        initializer.initialize()
        val countAfterSecondInit = db.projectsQueries.getAllProjectsUnordered().executeAsList().size

        assertEquals(countAfterFirstInit, countAfterSecondInit)
    }
}
```

--- FILE: shared/src/androidUnitTest/kotlin/com/romankozak/forwardappmobile/shared/data/database/TestDriverFactory.kt ---
```kotlin
package com.romankozak.forwardappmobile.shared.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.*

import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase

actual fun createTestDriver(): SqlDriver {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val driver = AndroidSqliteDriver(ForwardAppDatabase.Schema, context, "test.db")
    return driver
}

actual fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase {
    return createForwardAppDatabase(driver)
}

actual fun closeTestDriver(driver: SqlDriver) {
    driver.close()
}
```

--- FILE: shared/src/androidUnitTest/kotlin/com/romankozak/forwardappmobile/shared/data/database/TestAdapters.kt ---
```kotlin

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
```

---

## Latest Error Log

```
> Task :shared:testDebugUnitTest FAILED                                                                                             
                                                                                                                                    
DatabaseInitializerTest > initialize creates special projects when database is empty FAILED                                         
    java.lang.IllegalStateException at DatabaseInitializerTest.kt:22                                                                
    kotlin.UninitializedPropertyAccessException at DatabaseInitializerTest.kt:29                                                    
                                                                                                                                    
DatabaseInitializerTest > initialize does not create duplicates FAILED                                                              
    java.lang.IllegalStateException at DatabaseInitializerTest.kt:22                                                                
    kotlin.UninitializedPropertyAccessException at DatabaseInitializerTest.kt:29                                                    
                                                                                                                                    
2 tests completed, 2 failed                                                                                                         
                                                                                                                                    
> Task :shared:testReleaseUnitTest FAILED                                                                                           
                                                                                                                                    
DatabaseInitializerTest > initialize creates special projects when database is empty FAILED                                         
    java.lang.IllegalStateException at DatabaseInitializerTest.kt:22                                                                
    kotlin.UninitializedPropertyAccessException at DatabaseInitializerTest.kt:29                                                    
                                                                                                                                    
DatabaseInitializerTest > initialize does not create duplicates FAILED                                                              
    java.lang.IllegalStateException at DatabaseInitializerTest.kt:22                                                                
    kotlin.UninitializedPropertyAccessException at DatabaseInitializerTest.kt:29                                                    
                                                                                                                                    
2 tests completed, 2 failed                                                                                                         
                                                                                                                                    
FAILURE: Build completed with 2 failures.
```

```