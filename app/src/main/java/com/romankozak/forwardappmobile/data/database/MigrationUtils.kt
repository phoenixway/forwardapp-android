package com.romankozak.forwardappmobile.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

fun migrateSpecialProjects(db: SupportSQLiteDatabase) {
    Log.d("MigrationUtils", "Starting migrateSpecialProjects")

    // Update project names to be lowercase and hyphenated first
    db.execSQL("UPDATE projects SET name = 'special' WHERE name = 'Спеціальні'")
    db.execSQL("UPDATE projects SET name = 'inbox' WHERE name = 'Вхідні'")
    db.execSQL("UPDATE projects SET name = 'strategic' WHERE name = 'Стратегічні'")
    db.execSQL("UPDATE projects SET name = 'mission' WHERE name = 'Місія'")
    db.execSQL("UPDATE projects SET name = 'long-term-strategy' WHERE name = 'Довгострокова стратегія'")
    db.execSQL("UPDATE projects SET name = 'medium-term-program' WHERE name = 'Середньострокова програма'")
    db.execSQL("UPDATE projects SET name = 'active-quests' WHERE name = 'Активні квести'")
    db.execSQL("UPDATE projects SET name = 'strategic-inbox' WHERE name = 'Стратегічні цілі'")
    db.execSQL("UPDATE projects SET name = 'strategic-inbox' WHERE name = 'strategic-goals'")
    db.execSQL("UPDATE projects SET name = 'strategic-review' WHERE name = 'Стратегічний огляд'")
    db.execSQL("UPDATE projects SET name = 'main-beacons' WHERE name = 'Головні маяки'")
    db.execSQL("UPDATE projects SET reserved_group = 'MainBeacons' WHERE (name = 'mission' OR name = 'Місія') AND reserved_group != 'MainBeacons'")

    val specialProjectIdCursor = db.query("SELECT id FROM projects WHERE project_type = 'SYSTEM' LIMIT 1")
    var specialProjectId: String? = null
    if (specialProjectIdCursor.moveToFirst()) {
        specialProjectId = specialProjectIdCursor.getString(specialProjectIdCursor.getColumnIndexOrThrow("id"))
    }
    specialProjectIdCursor.close()
    Log.d("MigrationUtils", "specialProjectId: $specialProjectId")

    if (specialProjectId != null) {
        // Find strategic group (after name update)
        val strategicGroupIdCursor = db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'strategic' LIMIT 1", arrayOf(specialProjectId))
        var strategicGroupId: String? = null
        if (strategicGroupIdCursor.moveToFirst()) {
            strategicGroupId = strategicGroupIdCursor.getString(strategicGroupIdCursor.getColumnIndexOrThrow("id"))
        }
        strategicGroupIdCursor.close()
        Log.d("MigrationUtils", "strategicGroupId: $strategicGroupId")

        // Find or create main_beacons_group (after name update)
        val mainBeaconsGroupIdCursor = db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'main-beacons' LIMIT 1", arrayOf(specialProjectId))
        var mainBeaconsGroupId: String? = null
        if (mainBeaconsGroupIdCursor.moveToFirst()) {
            mainBeaconsGroupId = mainBeaconsGroupIdCursor.getString(mainBeaconsGroupIdCursor.getColumnIndexOrThrow("id"))
        }
        mainBeaconsGroupIdCursor.close()
        Log.d("MigrationUtils", "mainBeaconsGroupId (before create): $mainBeaconsGroupId")

        if (mainBeaconsGroupId == null) {
            mainBeaconsGroupId = java.util.UUID.randomUUID().toString()
            db.execSQL(
                "INSERT INTO projects (id, name, parentId, is_expanded, project_type, reserved_group, createdAt, scoring_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(mainBeaconsGroupId, "main-beacons", specialProjectId, 0, "RESERVED", "MainBeaconsGroup", System.currentTimeMillis(), "NOT_ASSESSED")
            )
            Log.d("MigrationUtils", "main-beacons group created with ID: $mainBeaconsGroupId")
        }

        // Find mission project (now that its name should be 'mission')
        val missionProjectIdCursor = db.query("SELECT id, parentId FROM projects WHERE reserved_group = 'MainBeacons' LIMIT 1")
        var missionProjectId: String? = null
        var missionParentId: String? = null
        if (missionProjectIdCursor.moveToFirst()) {
            missionProjectId = missionProjectIdCursor.getString(missionProjectIdCursor.getColumnIndexOrThrow("id"))
            missionParentId = missionProjectIdCursor.getString(missionProjectIdCursor.getColumnIndexOrThrow("parentId"))
        }
        missionProjectIdCursor.close()
        Log.d("MigrationUtils", "missionProjectId: $missionProjectId, missionParentId: $missionParentId")

        if (missionProjectId != null && missionParentId != mainBeaconsGroupId) {
            // Update mission project's parent
            db.execSQL(
                "UPDATE projects SET parentId = ? WHERE id = ?",
                arrayOf(mainBeaconsGroupId, missionProjectId)
            )
            Log.d("MigrationUtils", "Mission project parent updated to: $mainBeaconsGroupId")
        } else if (missionProjectId != null) {
            Log.d("MigrationUtils", "Mission project already under main-beacons group or not found.")
        }
    }
    Log.d("MigrationUtils", "Finished migrateSpecialProjects")
}
