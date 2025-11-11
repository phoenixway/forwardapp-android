package com.romankozak.forwardappmobile.shared.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.*

actual fun createTestDriver(): Any {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val driver = AndroidSqliteDriver(ForwardAppDatabase.Schema, context, "test.db")
    return driver
}

actual fun createTestDatabase(driver: Any): ForwardAppDatabase {
    require(driver is AndroidSqliteDriver)

    return ForwardAppDatabase(
        driver = driver,
        GoalsAdapter = Goals.Adapter(
            createdAtAdapter = TestAdapters.longAdapter,
            tagsAdapter = TestAdapters.stringListAdapter,
            relatedLinksAdapter = TestAdapters.relatedLinksListAdapter,
            valueImportanceAdapter = TestAdapters.doubleAdapter,
            valueImpactAdapter = TestAdapters.doubleAdapter,
            effortAdapter = TestAdapters.doubleAdapter,
            costAdapter = TestAdapters.doubleAdapter,
            riskAdapter = TestAdapters.doubleAdapter,
            weightEffortAdapter = TestAdapters.doubleAdapter,
            weightCostAdapter = TestAdapters.doubleAdapter,
            weightRiskAdapter = TestAdapters.doubleAdapter,
            rawScoreAdapter = TestAdapters.doubleAdapter,
            displayScoreAdapter = TestAdapters.longAdapter
        ),
        projectsAdapter = Projects.Adapter(
            createdAtAdapter = TestAdapters.longAdapter,
            goalOrderAdapter = TestAdapters.longAdapter,
            tagsAdapter = TestAdapters.stringListAdapter,
            relatedLinksAdapter = TestAdapters.relatedLinksListAdapter,
            projectTypeAdapter = TestAdapters.projectTypeAdapter,
            reservedGroupAdapter = TestAdapters.reservedGroupAdapter,
            valueImportanceAdapter = TestAdapters.doubleAdapter,
            valueImpactAdapter = TestAdapters.doubleAdapter,
            effortAdapter = TestAdapters.doubleAdapter,
            costAdapter = TestAdapters.doubleAdapter,
            riskAdapter = TestAdapters.doubleAdapter,
            weightEffortAdapter = TestAdapters.doubleAdapter,
            weightCostAdapter = TestAdapters.doubleAdapter,
            weightRiskAdapter = TestAdapters.doubleAdapter,
            rawScoreAdapter = TestAdapters.doubleAdapter,
            displayScoreAdapter = TestAdapters.longAdapter
        )
    )
}

actual fun closeTestDriver(driver: Any) {
    require(driver is AndroidSqliteDriver)
    driver.close()
}