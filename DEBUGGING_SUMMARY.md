# Debugging Summary: `SQLiteException: no such table: Goals`

This document summarizes the debugging process for an `org.sqlite.SQLiteException: [SQLITE_ERROR] SQL error or missing database (no such table: Goals)` error that occurs when running tests in the `shared` module.

## 1. The Problem

When running the `jvmTest` tests for the `:shared` module, the build fails with the following error:

```
org.sqlite.SQLiteException: [SQLITE_ERROR] SQL error or missing database (no such table: Goals)
```

This error occurs even when running a minimal test case that only creates the database and inserts a single value into the `Goals` table.

## 2. Relevant Code

Here is the relevant code for the project setup, database creation, and testing.

### `shared/build.gradle.kts`

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
}

// 🧩 Workaround для Compose Native initialization bug
System.setProperty("org.jetbrains.kotlin.native.ignoreDisabledTargets", "true")

kotlin {
	@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

    // ✅ Android target (обов’язково для multiplatform)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    // ✅ JVM target (для unit-тестів або desktop-логіки)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
                implementation(libs.kotlinxDatetime)
                implementation(libs.benasherUuid)
                implementation(libs.sqldelightRuntime)
                implementation(libs.sqldelightCoroutines)
                implementation(libs.kotlinInjectRuntime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelightAndroidDriver)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelightSqliteDriver)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest)
            }
        }

        val androidUnitTest by getting {
            kotlin.srcDir("src/androidUnitTest/kotlin")
            dependencies {
                implementation(libs.sqldelightAndroidDriver)
                implementation("androidx.test:core:1.5.0")
            }
        }
        val jvmTest by getting {
            dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }

        
    }
}

android {
    namespace = "com.romankozak.forwardappmobile.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ✅ SQLDelight configuration
sqldelight {
    databases {
        create("ForwardAppDatabase") {
            packageName.set("com.romankozak.forwardappmobile.shared.database")
            srcDirs("src/commonMain/sqldelight")
            deriveSchemaFromMigrations.set(true)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")
            // ✅ linkSqlite видалено — більше не існує у 2.x
        }
    }
}




// ✅ Kotlin Inject via KSP 2.1.x
dependencies {
    add("kspCommonMainMetadata", libs.kotlinInjectCompilerKsp)
    add("kspAndroid", libs.kotlinInjectCompilerKsp)
    add("kspJvm", libs.kotlinInjectCompilerKsp)
}

ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

// ✅ Include generated KSP sources automatically
kotlin.sourceSets.configureEach {
    kotlin.srcDir("build/generated/ksp/$name/kotlin")
}

// ✅ Ensure KSP tasks run before compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
```

### `shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/database/Database.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
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

fun createForwardAppDatabase(driverFactory: DatabaseDriverFactory): ForwardAppDatabase {
    println("Creating database...")
    val driver = driverFactory.createDriver()
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

### `shared/src/commonMain/sqldelight/com/romankozak/forwardappmobile/shared/database/Goals.sq`

```sql
import kotlin.Boolean;
import kotlin.String;
import kotlin.Double;
import kotlin.Long;
import kotlin.collections.List;
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink;

CREATE TABLE Goals (
    id TEXT NOT NULL PRIMARY KEY,
    text TEXT NOT NULL,
    description TEXT,
    completed INTEGER AS Boolean NOT NULL DEFAULT 0,
    createdAt INTEGER AS Long NOT NULL,
    updatedAt INTEGER AS Long,
    tags TEXT AS List<String>,
    relatedLinks TEXT AS List<RelatedLink>,
    valueImportance REAL AS Double NOT NULL DEFAULT 0.0,
    valueImpact REAL AS Double NOT NULL DEFAULT 0.0,
    effort REAL AS Double NOT NULL DEFAULT 0.0,
    cost REAL AS Double NOT NULL DEFAULT 0.0,
    risk REAL AS Double NOT NULL DEFAULT 0.0,
    weightEffort REAL AS Double NOT NULL DEFAULT 1.0,
    weightCost REAL AS Double NOT NULL DEFAULT 1.0,
    weightRisk REAL AS Double NOT NULL DEFAULT 1.0,
    rawScore REAL AS Double NOT NULL DEFAULT 0.0,
    displayScore INTEGER AS Long NOT NULL DEFAULT 0,
    scoringStatus TEXT NOT NULL DEFAULT 'NOT_ASSESSED',
    parentValueImportance REAL AS Double,
    impactOnParentGoal REAL AS Double,
    timeCost REAL AS Double,
    financialCost REAL AS Double
);

getAllGoals:
SELECT * FROM Goals;

getGoalsByIds:
SELECT * FROM Goals WHERE id IN :ids;

insertGoal:
INSERT OR REPLACE INTO Goals(
  id, text, description, completed, createdAt, updatedAt, tags, relatedLinks, valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, rawScore, displayScore, scoringStatus, parentValueImportance, impactOnParentGoal, timeCost, financialCost
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

### `shared/src/commonTest/kotlin/com/romankozak/forwardappmobile/shared/database/TestDatabaseDriverFactory.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}
```

### `shared/src/jvmTest/kotlin/com/romankozak/forwardappmobile/shared/database/TestDatabaseDriverFactory.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        return driver
    }
}
```

### `shared/src/androidUnitTest/kotlin/com/romankozak/forwardappmobile/shared/database/DatabaseDriverFactory.kt`

```kotlin
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
```

### `shared/src/jvmTest/kotlin/com/romankozak/forwardappmobile/shared/features/goals/data/repository/GoalRepositoryTest.kt`

```kotlin
package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryTest {

    private lateinit var db: ForwardAppDatabase
    private lateinit var repository: GoalRepositoryImpl
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setup() {
        val driverFactory = DatabaseDriverFactory()
        driver = driverFactory.createDriver()
        db = createForwardAppDatabase(driverFactory)
        repository = GoalRepositoryImpl(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `getGoalsByIds returns empty list initially`() = runTest {
        val goals = repository.getGoalsByIds(listOf("any_id")).first()
        assertTrue(goals.isEmpty())
    }

    @Test
    fun `insert and retrieve goal by id`() = runTest {
        val goal = Goal(
            id = "goal_1",
            text = "Test Goal",
            description = "Description",
            completed = false,
            createdAt = 1L,
            updatedAt = null,
            tags = listOf("tag1", "tag2"),
            relatedLinks = emptyList(),
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0L,
            scoringStatus = "NOT_ASSESSED",
            parentValueImportance = null,
            impactOnParentGoal = null,
            timeCost = null,
            financialCost = null
        )
        db.goalsQueries.insertGoal(
            id = goal.id,
            text = goal.text,
            description = goal.description,
            completed = goal.completed,
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt,
            tags = goal.tags,
            relatedLinks = goal.relatedLinks,
            valueImportance = goal.valueImportance,
            valueImpact = goal.valueImpact,
            effort = goal.effort,
            cost = goal.cost,
            risk = goal.risk,
            weightEffort = goal.weightEffort,
            weightCost = goal.weightCost,
            weightRisk = goal.weightRisk,
            rawScore = goal.rawScore,
            displayScore = goal.displayScore,
            scoringStatus = goal.scoringStatus,
            parentValueImportance = goal.parentValueImportance,
            impactOnParentGoal = goal.impactOnParentGoal,
            timeCost = goal.timeCost,
            financialCost = goal.financialCost
        )

        val retrievedGoal = repository.getGoalsByIds(listOf(goal.id)).first().firstOrNull()
        assertNotNull(retrievedGoal)
        assertEquals(goal.id, retrievedGoal.id)
        assertEquals(goal.text, retrievedGoal.text)
    }

    @Test
    fun `getGoalsByIds returns multiple goals`() = runTest {
        val goal1 = Goal(
            id = "goal_1",
            text = "Test Goal 1",
            description = null,
            completed = false,
            createdAt = 1L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0L,
            scoringStatus = "NOT_ASSESSED",
            parentValueImportance = null,
            impactOnParentGoal = null,
            timeCost = null,
            financialCost = null
        )
        val goal2 = Goal(
            id = "goal_2",
            text = "Test Goal 2",
            description = null,
            completed = false,
            createdAt = 2L,
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0L,
            scoringStatus = "NOT_ASSESSED",
            parentValueImportance = null,
            impactOnParentGoal = null,
            timeCost = null,
            financialCost = null
        )
        db.goalsQueries.insertGoal(
            id = goal1.id,
            text = goal1.text,
            description = goal1.description,
            completed = goal1.completed,
            createdAt = goal1.createdAt,
            updatedAt = goal1.updatedAt,
            tags = goal1.tags,
            relatedLinks = goal1.relatedLinks,
            valueImportance = goal1.valueImportance,
            valueImpact = goal1.valueImpact,
            effort = goal1.effort,
            cost = goal1.cost,
            risk = goal1.risk,
            weightEffort = goal1.weightEffort,
            weightCost = goal1.weightCost,
            weightRisk = goal1.weightRisk,
            rawScore = goal1.rawScore,
            displayScore = goal1.displayScore,
            scoringStatus = goal1.scoringStatus,
            parentValueImportance = goal1.parentValueImportance,
            impactOnParentGoal = goal1.impactOnParentGoal,
            timeCost = goal1.timeCost,
            financialCost = goal1.financialCost
        )
        db.goalsQueries.insertGoal(
            id = goal2.id,
            text = goal2.text,
            description = goal2.description,
            completed = goal2.completed,
            createdAt = goal2.createdAt,
            updatedAt = goal2.updatedAt,
            tags = goal2.tags,
            relatedLinks = goal2.relatedLinks,
            valueImportance = goal2.valueImportance,
            valueImpact = goal2.valueImpact,
            effort = goal2.effort,
            cost = goal2.cost,
            risk = goal2.risk,
            weightEffort = goal2.weightEffort,
            weightCost = goal2.weightCost,
            weightRisk = goal2.weightRisk,
            rawScore = goal2.rawScore,
            displayScore = goal2.displayScore,
            scoringStatus = goal2.scoringStatus,
            parentValueImportance = goal2.parentValueImportance,
            impactOnParentGoal = goal2.impactOnParentGoal,
            timeCost = goal2.timeCost,
            financialCost = goal2.financialCost
        )

        val goals = repository.getGoalsByIds(listOf(goal1.id, goal2.id)).first()
        assertEquals(2, goals.size)
        assertEquals(goal1.id, goals[0].id)
        assertEquals(goal2.id, goals[1].id)
    }
}
```

## 3. Steps Taken

1.  **Isolated the tests:** I commented out all other tests except for `GoalRepositoryTest.kt` to focus on the simplest failing case.
2.  **Verified serializable classes:** I checked that the `RelatedLink` data class and `LinkType` enum were correctly annotated with `@Serializable`.
3.  **Tried to get a stack trace:** I added `try-catch` blocks to print the stack trace of the exception to the console and to a file.
4.  **Moved schema creation:** I moved the `ForwardAppDatabase.Schema.create(driver)` call to the `createForwardAppDatabase` function to ensure that the schema is created for all drivers.
5.  **Created a minimal test case:** I created a new test file, `SimpleDatabaseTest.kt`, with a minimal test case that only creates the database and inserts a single value. This test also failed with the same error.
6.  **Manually created the table:** I modified the `SimpleDatabaseTest.kt` to manually create the `Goals` table using a raw SQL query. This also failed with the same error.

## 4. Final Error Message and Stack Trace

```
org.sqlite.SQLiteException: [SQLITE_ERROR] SQL error or missing database (no such table: Goals)
	at org.sqlite.core.DB.newSQLException(DB.java:1179)
	at org.sqlite.core.DB.newSQLException(DB.java:1190)
	at org.sqlite.core.DB.throwex(DB.java:1150)
	at org.sqlite.core.NativeDB.prepare_utf8(Native Method)
	at org.sqlite.core.NativeDB.prepare(NativeDB.java:132)
	at org.sqlite.core.DB.prepare(DB.java:264)
	at org.sqlite.core.CorePreparedStatement.<init>(CorePreparedStatement.java:46)
	at org.sqlite.jdbc3.JDBC3PreparedStatement.<init>(JDBC3PreparedStatement.java:32)
	at org.sqlite.jdbc4.JDBC4PreparedStatement.<init>(JDBC4PreparedStatement.java:25)
	at org.sqlite.jdbc4.JDBC4Connection.prepareStatement(JDBC4Connection.java:34)
	at org.sqlite.jdbc3.JDBC3Connection.prepareStatement(JDBC3Connection.java:225)
	at org.sqlite.jdbc3.JDBC3Connection.prepareStatement(JDBC3Connection.java:205)
	at app.cash.sqldelight.driver.jdbc.JdbcDriver.execute(JdbcDriver.kt:133)
	at com.romankozak.forwardappmobile.shared.database.GoalsQueries.insertGoal(GoalsQueries.kt:214)
	at com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepositoryTest$insert and retrieve goal by id$1.invokeSuspend(GoalRepositoryTest.kt:69)
	at com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepositoryTest$insert and retrieve goal by id$1.invoke(GoalRepositoryTest.kt)
	at com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepositoryTest$insert and retrieve goal by id$1.invoke(GoalRepositoryTest.kt)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt$runTest$2$1$1.invokeSuspend(TestBuilders.kt:318)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:101)
	at kotlinx.coroutines.test.TestDispatcher.processEvent$kotlinx_coroutines_test(TestDispatcher.kt:24)
	at kotlinx.coroutines.test.TestCoroutineScheduler.tryRunNextTaskUnless$kotlinx_coroutines_test(TestCoroutineScheduler.kt:99)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt$runTest$2$1$workRunner$1.invokeSuspend(TestBuilders.kt:327)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:101)
	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:263)
	at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:95)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:69)
	at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:47)
	at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
	at kotlinx.coroutines.test.TestBuildersJvmKt.createTestResult(TestBuildersJvm.kt:10)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt.runTest-8Mi8wO0(TestBuilders.kt:310)
	at kotlinx.coroutines.test.TestBuildersKt.runTest-8Mi8wO0(Unknown Source)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt.runTest-8Mi8wO0(TestBuilders.kt:168)
	at kotlinx.coroutines.test.TestBuildersKt.runTest-8Mi8wO0(Unknown Source)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt.runTest-8Mi8wO0$default(TestBuilders.kt:160)
	at kotlinx.coroutines.test.TestBuildersKt.runTest-8Mi8wO0$default(Unknown Source)
	at com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepositoryTest.insert and retrieve goal by id(GoalRepositoryTest.kt:42)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.runTestClass(JUnitTestClassExecutor.java:112)
	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:58)
	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:40)
	at org.gradle.api.internal.tasks.testing.junit.AbstractJUnitTestClassProcessor.processTestClass(AbstractJUnitTestClassProcessor.java:54)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.processTestClass(SuiteTestClassProcessor.java:53)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:33)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:92)
	at jdk.proxy1/jdk.proxy1.$Proxy4.processTestClass(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker$2.run(TestWorker.java:183)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.executeAndMaintainThreadName(TestWorker.java:132)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:103)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:63)
	at org.gradle.process.internal.worker.child.ActionExecutionWorker.execute(ActionExecutionWorker.java:56)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:121)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:71)
	at worker.org.gradle.process.internal.worker.GradleWorkerMain.run(GradleWorkerMain.java:69)
	at worker.org.gradle.process.internal.worker.GradleWorkerMain.main(GradleWorkerMain.java:74)
```