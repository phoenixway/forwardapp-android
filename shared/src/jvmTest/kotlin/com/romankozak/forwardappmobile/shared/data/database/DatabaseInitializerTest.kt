package com.romankozak.forwardappmobile.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase // Added import
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.database.doubleAdapter
import com.romankozak.forwardappmobile.shared.database.longAdapter
import com.romankozak.forwardappmobile.shared.database.projectTypeAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.database.reservedGroupAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter

actual fun createTestDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ForwardAppDatabase.Schema.create(driver)
    return driver
}

actual fun createTestDatabase(driver: SqlDriver): ForwardAppDatabase {
    return ForwardAppDatabase(
        driver = driver,
        projectsAdapter = Projects.Adapter(
            createdAtAdapter = longAdapter,
            tagsAdapter = stringListAdapter,
            relatedLinksAdapter = relatedLinksListAdapter,
            goalOrderAdapter = longAdapter,
            valueImportanceAdapter = doubleAdapter,
            valueImpactAdapter = doubleAdapter,
            effortAdapter = doubleAdapter,
            costAdapter = doubleAdapter,
            riskAdapter = doubleAdapter,
            weightEffortAdapter = doubleAdapter,
            weightCostAdapter = doubleAdapter,
            weightRiskAdapter = doubleAdapter,
            rawScoreAdapter = doubleAdapter,
            displayScoreAdapter = longAdapter,
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
            displayScoreAdapter = longAdapter
        )
    )
}

actual fun closeTestDriver(driver: SqlDriver) {
    driver.close()
}
