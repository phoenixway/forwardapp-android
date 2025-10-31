package com.romankozak.forwardappmobile.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

fun migrateSpecialProjects(db: SupportSQLiteDatabase) {
    // Update project names to be lowercase and hyphenated
    db.execSQL("UPDATE projects SET name = 'special' WHERE name = 'Спеціальні'")
    db.execSQL("UPDATE projects SET name = 'inbox' WHERE name = 'Вхідні'")
    db.execSQL("UPDATE projects SET name = 'strategic' WHERE name = 'Стратегічні'")
    db.execSQL("UPDATE projects SET name = 'mission' WHERE name = 'Місія'")
    db.execSQL("UPDATE projects SET name = 'long-term-strategy' WHERE name = 'Довгострокова стратегія'")
    db.execSQL("UPDATE projects SET name = 'medium-term-program' WHERE name = 'Середньострокова програма'")
    db.execSQL("UPDATE projects SET name = 'active-quests' WHERE name = 'Активні квести'")
    db.execSQL("UPDATE projects SET name = 'strategic-goals' WHERE name = 'Стратегічні цілі'")
    db.execSQL("UPDATE projects SET name = 'strategic-review' WHERE name = 'Стратегічний огляд'")
    db.execSQL("UPDATE projects SET name = 'main-beacons' WHERE name = 'Головні маяки'")


    val specialProjectIdCursor = db.query("SELECT id FROM projects WHERE project_type = 'SYSTEM' LIMIT 1")
    var specialProjectId: String? = null
    if (specialProjectIdCursor.moveToFirst()) {
        specialProjectId = specialProjectIdCursor.getString(specialProjectIdCursor.getColumnIndexOrThrow("id"))
    }
    specialProjectIdCursor.close()

    if (specialProjectId != null) {
        // Find or create main_beacons_group
        val mainBeaconsGroupIdCursor = db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'main-beacons' LIMIT 1", arrayOf(specialProjectId))
        var mainBeaconsGroupId: String? = null
        if (mainBeaconsGroupIdCursor.moveToFirst()) {
            mainBeaconsGroupId = mainBeaconsGroupIdCursor.getString(mainBeaconsGroupIdCursor.getColumnIndexOrThrow("id"))
        }
        mainBeaconsGroupIdCursor.close()

        if (mainBeaconsGroupId == null) {
            mainBeaconsGroupId = java.util.UUID.randomUUID().toString()
            db.execSQL(
                "INSERT INTO projects (id, name, parentId, is_expanded, project_type, reserved_group, createdAt, scoring_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(mainBeaconsGroupId, "main-beacons", specialProjectId, 0, "RESERVED", "MainBeaconsGroup", System.currentTimeMillis(), "NOT_ASSESSED")
            )
        }

        // Find mission project
        val missionProjectIdCursor = db.query("SELECT id, parentId FROM projects WHERE reserved_group = 'MainBeacons' LIMIT 1")
        var missionProjectId: String? = null
        var missionParentId: String? = null
        if (missionProjectIdCursor.moveToFirst()) {
            missionProjectId = missionProjectIdCursor.getString(missionProjectIdCursor.getColumnIndexOrThrow("id"))
            missionParentId = missionProjectIdCursor.getString(missionProjectIdCursor.getColumnIndexOrThrow("parentId"))
        }
        missionProjectIdCursor.close()

        if (missionProjectId != null && missionParentId != mainBeaconsGroupId) {
            // Update mission project's parent
            db.execSQL(
                "UPDATE projects SET parentId = ? WHERE id = ?",
                arrayOf(mainBeaconsGroupId, missionProjectId)
            )
        }
    }
}