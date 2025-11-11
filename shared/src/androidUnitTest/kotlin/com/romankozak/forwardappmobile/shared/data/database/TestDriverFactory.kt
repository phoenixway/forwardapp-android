package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.Projects
//import com.romankozak.forwardappmobile.shared.data.database

actual fun createTestDriver(): Any {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ForwardAppDatabase.Schema.create(driver)
    return driver
}

actual fun createTestDatabase(driver: Any): ForwardAppDatabase {
    require(driver is JdbcSqliteDriver)
    
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
    require(driver is JdbcSqliteDriver)
    driver.close()
}
