package com.romankozak.forwardappmobile.data.database

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.data.database.models.ReservedProjectKeys
import java.text.Normalizer
import java.util.UUID

private const val MIGRATION_LOG_TAG = "ForwardMigration"

fun migrateSpecialProjects(db: SupportSQLiteDatabase) {
    Log.d(MIGRATION_LOG_TAG, "Starting migrateSpecialProjects")

    val projectCountCursor = db.query("SELECT COUNT(*) as total FROM projects")
    var totalProjects = 0L
    projectCountCursor.use {
        if (it.moveToFirst()) {
            totalProjects = it.getLong(it.getColumnIndexOrThrow("total"))
        }
    }
    if (totalProjects == 0L) {
        Log.d(MIGRATION_LOG_TAG, "Skipping migrateSpecialProjects: projects table empty")
        return
    }
    normalizeSpecialProjectNames(db)

    if (!db.hasColumn("projects", "system_key")) {
        migrateSpecialProjectsLegacy(db)
    } else {
        migrateSpecialProjectsWithSystemKeys(db)
    }
    Log.d(MIGRATION_LOG_TAG, "Finished migrateSpecialProjects")
}

private fun normalizeSpecialProjectNames(db: SupportSQLiteDatabase) {
    db.execSQL("UPDATE projects SET name = 'personal-management' WHERE name IN ('Спеціальні', 'special')")
    db.execSQL("UPDATE projects SET name = 'inbox' WHERE name = 'Вхідні'")
    db.execSQL("UPDATE projects SET name = 'strategic' WHERE name = 'Стратегічні'")
    db.execSQL("UPDATE projects SET name = 'mission' WHERE name = 'Місія'")
    db.execSQL("UPDATE projects SET name = 'long-term-strategy' WHERE name = 'Довгострокова стратегія'")
    db.execSQL("UPDATE projects SET name = 'medium-term-strategy' WHERE name IN ('Середньострокова програма', 'medium-term-program', 'medium-term-programs')")
    db.execSQL("UPDATE projects SET name = 'active-quests' WHERE name = 'Активні квести'")
    db.execSQL("UPDATE projects SET name = 'strategic-inbox' WHERE name IN ('Стратегічні цілі', 'strategic-goals')")
    db.execSQL("UPDATE projects SET name = 'strategic-review' WHERE name = 'Стратегічний огляд'")
    db.execSQL("UPDATE projects SET name = 'main-beacons' WHERE name IN ('Головні маяки')")
    db.execSQL(
        """
        UPDATE projects
           SET name = 'strategic-beacons'
         WHERE name = 'main-beacons'
           AND reserved_group IN ('main_beacons_group', 'MainBeaconsGroup')
        """.trimIndent()
    )
    db.execSQL("UPDATE projects SET name = 'strategic-programs' WHERE name IN ('strategic-program', 'strategic-programs')")
    db.execSQL("DELETE FROM projects WHERE name LIKE 'main-beacons-realization%' OR system_key = 'main-beacons-realization'")

    db.execSQL("UPDATE projects SET reserved_group = 'main_beacons' WHERE reserved_group IN ('MainBeacons')")
    db.execSQL("UPDATE projects SET reserved_group = 'main_beacons_group' WHERE reserved_group IN ('MainBeaconsGroup')")
    db.execSQL("UPDATE projects SET reserved_group = 'strategic_group' WHERE reserved_group IN ('StrategicGroup')")
    db.execSQL("UPDATE projects SET reserved_group = 'strategic' WHERE reserved_group IN ('Strategic')")

    Log.d(
        MIGRATION_LOG_TAG,
        "normalizeSpecialProjectNames: personal-management, strategic, medium-term-strategy and reserved_group aliases normalized (main-beacons-realization removed)"
    )
}

private fun migrateSpecialProjectsLegacy(db: SupportSQLiteDatabase) {
    val personalManagementProjectIdCursor = db.query("SELECT id FROM projects WHERE project_type = 'SYSTEM' LIMIT 1")
    var personalManagementProjectId: String? = null
    if (personalManagementProjectIdCursor.moveToFirst()) {
        personalManagementProjectId = personalManagementProjectIdCursor.getString(personalManagementProjectIdCursor.getColumnIndexOrThrow("id"))
    }
    personalManagementProjectIdCursor.close()
    Log.d(MIGRATION_LOG_TAG, "legacy personalManagementProjectId: $personalManagementProjectId")

    if (personalManagementProjectId != null) {
        db.execSQL(
            "UPDATE projects SET name = 'personal-management' WHERE id = ?",
            arrayOf(personalManagementProjectId)
        )

        val strategicGroupIdCursor =
            db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'strategic' LIMIT 1", arrayOf(personalManagementProjectId))
        var strategicGroupId: String? = null
        if (strategicGroupIdCursor.moveToFirst()) {
            strategicGroupId = strategicGroupIdCursor.getString(strategicGroupIdCursor.getColumnIndexOrThrow("id"))
        }
        strategicGroupIdCursor.close()

        var strategicBeaconsGroupId: String? = null
        if (strategicGroupId != null) {
            val beaconsUnderStrategicCursor =
                db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'strategic-beacons' LIMIT 1", arrayOf(strategicGroupId))
            if (beaconsUnderStrategicCursor.moveToFirst()) {
                strategicBeaconsGroupId = beaconsUnderStrategicCursor.getString(beaconsUnderStrategicCursor.getColumnIndexOrThrow("id"))
            }
            beaconsUnderStrategicCursor.close()

            if (strategicBeaconsGroupId == null) {
                val beaconsUnderRootCursor =
                    db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'strategic-beacons' LIMIT 1", arrayOf(personalManagementProjectId))
                if (beaconsUnderRootCursor.moveToFirst()) {
                    strategicBeaconsGroupId = beaconsUnderRootCursor.getString(beaconsUnderRootCursor.getColumnIndexOrThrow("id"))
                }
                beaconsUnderRootCursor.close()
            }

            if (strategicBeaconsGroupId == null) {
                strategicBeaconsGroupId = UUID.randomUUID().toString()
                db.execSQL(
                    "INSERT INTO projects (id, name, parentId, is_expanded, project_type, reserved_group, createdAt, scoring_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(strategicBeaconsGroupId, "strategic-beacons", strategicGroupId, 0, "RESERVED", "main_beacons_group", System.currentTimeMillis(), "NOT_ASSESSED")
                )
            } else {
                db.execSQL(
                    "UPDATE projects SET name = 'strategic-beacons', parentId = ? WHERE id = ?",
                    arrayOf(strategicGroupId, strategicBeaconsGroupId)
                )
            }

            val missionProjectIdCursor =
                db.query("SELECT id, parentId FROM projects WHERE reserved_group = 'main_beacons' LIMIT 1")
            var missionProjectId: String? = null
            var missionParentId: String? = null
            if (missionProjectIdCursor.moveToFirst()) {
                missionProjectId = missionProjectIdCursor.getString(missionProjectIdCursor.getColumnIndexOrThrow("id"))
                missionParentId = missionProjectIdCursor.getString(missionProjectIdCursor.getColumnIndexOrThrow("parentId"))
            }
            missionProjectIdCursor.close()

            if (missionProjectId != null && missionParentId != strategicBeaconsGroupId) {
                db.execSQL(
                    "UPDATE projects SET parentId = ? WHERE id = ?",
                    arrayOf(strategicBeaconsGroupId, missionProjectId)
                )
            }

            db.execSQL(
                "UPDATE projects SET parentId = ? WHERE name = 'long-term-strategy'",
                arrayOf(strategicBeaconsGroupId)
            )

            var strategicProgramsExists = false
            val strategicProgramsCursor = db.query("SELECT id, parentId FROM projects WHERE name = 'strategic-programs' LIMIT 1")
            if (strategicProgramsCursor.moveToFirst()) {
                val existingStrategicProgramsId = strategicProgramsCursor.getString(strategicProgramsCursor.getColumnIndexOrThrow("id"))
                strategicProgramsExists = true
                val currentParentId = strategicProgramsCursor.getString(strategicProgramsCursor.getColumnIndexOrThrow("parentId"))
                if (currentParentId != strategicBeaconsGroupId) {
                    db.execSQL(
                        "UPDATE projects SET parentId = ? WHERE id = ?",
                        arrayOf(strategicBeaconsGroupId, existingStrategicProgramsId)
                    )
                }
            }
            strategicProgramsCursor.close()

            if (!strategicProgramsExists) {
                val strategicProgramsId = UUID.randomUUID().toString()
                db.execSQL(
                    "INSERT INTO projects (id, name, parentId, is_expanded, project_type, reserved_group, createdAt, scoring_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(strategicProgramsId, "strategic-programs", strategicBeaconsGroupId, 0, "RESERVED", "strategic", System.currentTimeMillis(), "NOT_ASSESSED")
                )
            }
        }

        db.execSQL(
            "UPDATE projects SET parentId = ? WHERE name = 'medium-term-strategy'",
            arrayOf(personalManagementProjectId)
        )

        var weekProjectId: String? = null
        val weekCursor =
            db.query("SELECT id FROM projects WHERE parentId = ? AND name = 'week' LIMIT 1", arrayOf(personalManagementProjectId))
        if (weekCursor.moveToFirst()) {
            weekProjectId = weekCursor.getString(weekCursor.getColumnIndexOrThrow("id"))
        }
        weekCursor.close()

        if (weekProjectId == null) {
            weekProjectId = UUID.randomUUID().toString()
            db.execSQL(
                "INSERT INTO projects (id, name, parentId, is_expanded, project_type, reserved_group, createdAt, scoring_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(weekProjectId, "week", personalManagementProjectId, 0, "RESERVED", "strategic", System.currentTimeMillis(), "NOT_ASSESSED")
            )
        }

        db.execSQL(
            "UPDATE projects SET parentId = ? WHERE name = 'active-quests'",
            arrayOf(weekProjectId)
        )
    }
}

private fun migrateSpecialProjectsWithSystemKeys(db: SupportSQLiteDatabase) {
    val personalManagementId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.PERSONAL_MANAGEMENT,
        defaultName = "personal-management",
        projectType = "SYSTEM",
        reservedGroup = null,
        parentId = null,
        legacyNames = listOf("personal-management", "special", "Спеціальні")
    ) ?: return

    val strategicId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.STRATEGIC,
        defaultName = "strategic",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.StrategicGroup.groupName,
        parentId = personalManagementId,
        legacyNames = listOf("strategic", "Стратегічні")
    ) ?: return

    val strategicBeaconsId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.STRATEGIC_BEACONS,
        defaultName = "strategic-beacons",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.MainBeaconsGroup.groupName,
        parentId = strategicId,
        legacyParentIds = listOf(personalManagementId),
        legacyNames = listOf("strategic-beacons"),
        legacyNamePatterns = listOf("strategic-beacons%"),
        legacyReservedGroups = listOf("main_beacons_group", "MainBeaconsGroup")
    ) ?: return

    val weekId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.WEEK,
        defaultName = "week",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = personalManagementId,
        legacyNames = listOf("week"),
        legacyNamePatterns = listOf("week%", "Week%")
    ) ?: return

    val todayId =
        ensureProjectWithKey(
            db = db,
            key = ReservedProjectKeys.TODAY,
            defaultName = "today",
            projectType = "RESERVED",
            reservedGroup = ReservedGroup.Inbox.groupName,
            parentId = personalManagementId,
            legacyNames = listOf("today"),
        )

    val mainBeaconsId =
        ensureProjectWithKey(
            db = db,
            key = ReservedProjectKeys.MAIN_BEACONS,
            defaultName = "main-beacons",
            projectType = "RESERVED",
            reservedGroup = ReservedGroup.MainBeacons.groupName,
            parentId = personalManagementId,
            legacyNames = listOf("main-beacons"),
            legacyNamePatterns = listOf("main-beacons%"),
            legacyReservedGroups = listOf(ReservedGroup.MainBeacons.groupName),
        )

    val mediumTermId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.MEDIUM_TERM_STRATEGY,
        defaultName = "medium-term-strategy",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = personalManagementId,
        legacyParentIds = listOf(strategicId),
        legacyNames = listOf("medium-term-strategy", "medium-term-program", "medium-term-programs", "Середньострокова програма"),
        legacyNamePatterns = listOf("%medium-term%", "%medium%strategy%", "%Середнь%")
    )

    ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.INBOX,
        defaultName = "inbox",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Inbox.groupName,
        parentId = todayId ?: personalManagementId,
        legacyNames = listOf("inbox", "Вхідні"),
        legacyNamePatterns = listOf("inbox%", "Inbox%"),
        fuzzyNameCandidates = listOf("inbox")
    )

    if (todayId != null) {
        db.execSQL(
            """
            UPDATE projects
               SET parentId = ?
             WHERE system_key = ?
               AND (parentId IS NULL OR parentId = ?)
            """.trimIndent(),
            arrayOf(todayId, ReservedProjectKeys.INBOX, personalManagementId),
        )
    }

    ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.STRATEGIC_INBOX,
        defaultName = "strategic-inbox",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = strategicId,
        legacyNames = listOf("strategic-inbox", "strategic-goals", "Стратегічні цілі")
    )

    ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.STRATEGIC_REVIEW,
        defaultName = "strategic-review",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = strategicId,
        legacyNames = listOf("strategic-review", "Стратегічний огляд")
    )

    ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.MISSION,
        defaultName = "mission",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.MainBeacons.groupName,
        parentId = strategicBeaconsId,
        legacyParentIds = listOf(strategicId),
        legacyNames = listOf("mission", "Місія"),
        legacyReservedGroups = listOf("main_beacons", "MainBeacons")
    )

    ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.LONG_TERM_STRATEGY,
        defaultName = "long-term-strategy",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = strategicBeaconsId,
        legacyParentIds = listOf(strategicId),
        legacyNames = listOf("long-term-strategy", "Довгострокова стратегія")
    )

    val strategicProgramsId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.STRATEGIC_PROGRAMS,
        defaultName = "strategic-programs",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = strategicBeaconsId,
        legacyParentIds = listOf(strategicId),
        legacyNames = listOf("strategic-programs", "strategic-program"),
        legacyNamePatterns = listOf("%strategic%program%")
    )

    val activeQuestsId = ensureProjectWithKey(
        db = db,
        key = ReservedProjectKeys.ACTIVE_QUESTS,
        defaultName = "active-quests",
        projectType = "RESERVED",
        reservedGroup = ReservedGroup.Strategic.groupName,
        parentId = weekId,
        legacyNames = listOf("active-quests", "Активні квести"),
        legacyNamePatterns = listOf("active-quests%")
    )

    cleanUpDuplicateReservedProjects(db, "week", weekId)
    cleanUpDuplicateReservedProjects(db, "strategic-beacons", strategicBeaconsId)
    cleanUpDuplicateReservedProjects(db, "main-beacons", mainBeaconsId)
    cleanUpDuplicateReservedProjects(db, "today", todayId)
    cleanUpDuplicateReservedProjects(db, "medium-term-strategy", mediumTermId)
    cleanUpDuplicateReservedProjects(db, "strategic-programs", strategicProgramsId)
    cleanUpDuplicateReservedProjects(db, "active-quests", activeQuestsId)
}

private fun ensureProjectWithKey(
    db: SupportSQLiteDatabase,
    key: String,
    defaultName: String,
    projectType: String,
    reservedGroup: String?,
    parentId: String?,
    legacyNames: List<String> = emptyList(),
    legacyNamePatterns: List<String> = emptyList(),
    legacyReservedGroups: List<String> = emptyList(),
    legacyParentIds: List<String?> = emptyList(),
    fuzzyNameCandidates: List<String> = emptyList(),
    createIfMissing: Boolean = true
): String? {
    var discoveryStrategy: String? = null
    val parentScopeCandidates = (listOf(parentId) + legacyParentIds).distinct()
    val parentCandidates = parentScopeCandidates.filterNotNull()
    val parentScopesFallback = if (parentScopeCandidates.isEmpty()) listOf<String?>(null) else parentScopeCandidates

    var targetId = db.queryUniqueId("SELECT id FROM projects WHERE system_key = ? LIMIT 2", arrayOf(key))
    if (targetId != null) discoveryStrategy = "system_key"

    if (targetId == null && parentCandidates.isNotEmpty() && legacyNames.isNotEmpty()) {
        parentCandidates.forEach { parent ->
            val placeholders = legacyNames.joinToString(",") { "?" }
            val args = arrayOf(parent) + legacyNames.toTypedArray()
            targetId = db.queryUniqueId(
                "SELECT id FROM projects WHERE parentId = ? AND name IN ($placeholders) LIMIT 2",
                args
            )
            if (targetId != null) return@forEach
        }
    }

    if (targetId == null && legacyNames.isNotEmpty()) {
        val placeholders = legacyNames.joinToString(",") { "?" }
        targetId = db.queryUniqueId(
            "SELECT id FROM projects WHERE name IN ($placeholders) LIMIT 2",
            legacyNames.toTypedArray()
        )
        if (targetId != null) discoveryStrategy = "legacy_name"
    }

    if (targetId == null && legacyNamePatterns.isNotEmpty()) {
        parentScopesFallback.forEach { parent ->
            legacyNamePatterns.forEach { pattern ->
                val condition = buildString {
                    if (parent != null) append("parentId = ? AND ")
                    append("name LIKE ? AND system_key IS NULL")
                }
                val args = mutableListOf<String>()
                if (parent != null) args += parent
                args += pattern
                val candidate = db.queryUniqueId(
                    "SELECT id FROM projects WHERE $condition LIMIT 2",
                    args.toTypedArray()
                )
                if (candidate != null) {
                    targetId = candidate
                    discoveryStrategy = "legacy_pattern"
                    return@forEach
                }
            }
            if (targetId != null) return@forEach
        }
    }

    if (targetId == null && reservedGroup != null) {
        val groups = (listOf(reservedGroup) + legacyReservedGroups).filterNotNull().distinct()
        if (groups.isNotEmpty()) {
            val placeholders = groups.joinToString(",") { "?" }
            val baseCondition = StringBuilder("reserved_group IN ($placeholders) AND system_key IS NULL")
            val baseArgs = groups.toMutableList()

            parentCandidates.forEach { parent ->
                val args = baseArgs.toMutableList()
                val condition = StringBuilder(baseCondition)
                condition.append(" AND parentId = ?")
                args += parent
                val candidate = db.queryUniqueId(
                    "SELECT id FROM projects WHERE $condition LIMIT 2",
                    args.toTypedArray()
                )
                if (candidate != null) {
                    targetId = candidate
                    return@forEach
                }
            }

            if (targetId == null) {
                targetId = db.queryUniqueId(
                    "SELECT id FROM projects WHERE $baseCondition LIMIT 2",
                    baseArgs.toTypedArray()
                )
                if (targetId != null) discoveryStrategy = "reserved_group"
            }
        }
    }

    if (targetId == null) {
        val fuzzyTargets = (fuzzyNameCandidates + legacyNames + listOf(defaultName, key))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (fuzzyTargets.isNotEmpty()) {
            targetId = db.findProjectByFuzzyName(
                targetNames = fuzzyTargets,
                parentCandidates = parentScopeCandidates,
                requireParentMatch = parentScopeCandidates.isNotEmpty()
            )?.also { discoveryStrategy = "fuzzy_parent" } ?: db.findProjectByFuzzyName(
                targetNames = fuzzyTargets,
                parentCandidates = parentScopeCandidates,
                requireParentMatch = false
            )?.also { discoveryStrategy = "fuzzy_any" }
        }
    }

    if (targetId == null && createIfMissing) {
        targetId = UUID.randomUUID().toString()
        db.execSQL(
            "INSERT INTO projects (id, name, parentId, is_expanded, project_type, reserved_group, createdAt, scoring_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(targetId, defaultName, parentId, 0, projectType, reservedGroup, System.currentTimeMillis(), "NOT_ASSESSED")
        )
        discoveryStrategy = "created"
    }

    targetId ?: return null

    db.execSQL("UPDATE projects SET system_key = ? WHERE id = ?", arrayOf(key, targetId))

    var currentParentId: String? = null
    var currentReservedGroup: String? = null
    var currentProjectType: String? = null
    db.query("SELECT parentId, reserved_group, project_type FROM projects WHERE id = ?", arrayOf(targetId)).use { cursor ->
        if (cursor.moveToFirst()) {
            currentParentId = cursor.getString(cursor.getColumnIndexOrThrow("parentId"))
            currentReservedGroup = cursor.getString(cursor.getColumnIndexOrThrow("reserved_group"))
            currentProjectType = cursor.getString(cursor.getColumnIndexOrThrow("project_type"))
        }
    }

    if (currentProjectType != projectType) {
        db.execSQL("UPDATE projects SET project_type = ? WHERE id = ?", arrayOf(projectType, targetId))
    }

    if (reservedGroup != null) {
        if (currentReservedGroup != reservedGroup) {
            db.execSQL("UPDATE projects SET reserved_group = ? WHERE id = ?", arrayOf(reservedGroup, targetId))
        }
    } else if (currentReservedGroup != null) {
        db.execSQL("UPDATE projects SET reserved_group = NULL WHERE id = ?", arrayOf(targetId))
    }

    val normalizedCurrentParent = currentParentId?.takeIf { it.isNotBlank() && !it.equals("null", true) }
    if (parentId != null) {
        if (normalizedCurrentParent == null) {
            db.execSQL("UPDATE projects SET parentId = ? WHERE id = ?", arrayOf(parentId, targetId))
        }
    } else if (normalizedCurrentParent != null) {
        db.execSQL("UPDATE projects SET parentId = NULL WHERE id = ?", arrayOf(targetId))
    }

    Log.d(
        MIGRATION_LOG_TAG,
        "ensureProjectWithKey[$key]: resolved $targetId via ${discoveryStrategy ?: "unknown"} (parent=$parentId)"
    )

    return targetId
}

private fun cleanUpDuplicateReservedProjects(
    db: SupportSQLiteDatabase,
    defaultName: String,
    canonicalId: String?
) {
    canonicalId ?: return

    val deleteQuery = """
        DELETE FROM projects
        WHERE name = ?
          AND system_key IS NULL
          AND project_type = 'RESERVED'
          AND id != ?
    """.trimIndent()
    db.execSQL(deleteQuery, arrayOf(defaultName, canonicalId))
}

private val DIACRITICS_REGEX = "\\p{Mn}+".toRegex()
private val NON_ALPHANUMERIC_REGEX = "[^a-z0-9]+".toRegex()

private fun normalizeProjectName(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val withoutDiacritics = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
    val normalized = NON_ALPHANUMERIC_REGEX.replace(withoutDiacritics, "-").trim('-')
    return normalized.ifBlank { null }
}

private fun computeFuzzyThreshold(target: String): Int {
    return when {
        target.length <= 4 -> 1
        target.length <= 8 -> 2
        target.length <= 16 -> maxOf(2, target.length / 4)
        else -> maxOf(3, target.length / 5)
    }
}

private fun levenshteinDistance(left: String, right: String): Int {
    if (left == right) return 0
    if (left.isEmpty()) return right.length
    if (right.isEmpty()) return left.length

    var previousRow = IntArray(right.length + 1) { it }
    var currentRow = IntArray(right.length + 1)

    left.forEachIndexed { i, lChar ->
        currentRow[0] = i + 1
        right.forEachIndexed { j, rChar ->
            val insertCost = currentRow[j] + 1
            val deleteCost = previousRow[j + 1] + 1
            val replaceCost = previousRow[j] + if (lChar == rChar) 0 else 1
            currentRow[j + 1] = minOf(insertCost, deleteCost, replaceCost)
        }
        val tmp = previousRow
        previousRow = currentRow
        currentRow = tmp
    }

    return previousRow[right.length]
}

private fun SupportSQLiteDatabase.findProjectByFuzzyName(
    targetNames: List<String>,
    parentCandidates: List<String?>,
    requireParentMatch: Boolean
): String? {
    val normalizedTargets = targetNames.mapNotNull { normalizeProjectName(it) }.distinct()
    if (normalizedTargets.isEmpty()) return null

    val cursor = query("SELECT id, parentId, name FROM projects WHERE system_key IS NULL")
    var bestId: String? = null
    var bestScore = Int.MAX_VALUE

    cursor.use {
        val idIndex = it.getColumnIndexOrThrow("id")
        val nameIndex = it.getColumnIndexOrThrow("name")
        val parentIndex = it.getColumnIndexOrThrow("parentId")
        while (it.moveToNext()) {
            val parentId = it.getString(parentIndex)
            val parentMatches = parentCandidates.isEmpty() || parentCandidates.contains(parentId)
            if (requireParentMatch && !parentMatches) continue

            val normalizedName = normalizeProjectName(it.getString(nameIndex)) ?: continue
            normalizedTargets.forEach { target ->
                val distance = levenshteinDistance(normalizedName, target)
                val threshold = computeFuzzyThreshold(target)
                if (distance <= threshold) {
                    val penalty = if (parentMatches) 0 else 1
                    val adjustedScore = distance + penalty
                    if (adjustedScore < bestScore) {
                        bestScore = adjustedScore
                        bestId = it.getString(idIndex)
                    }
                }
            }
        }
    }

    if (bestId != null) {
        Log.d(
            MIGRATION_LOG_TAG,
            "findProjectByFuzzyName targets=$normalizedTargets requireParentMatch=$requireParentMatch -> $bestId (score=$bestScore)"
        )
    } else {
        Log.d(
            MIGRATION_LOG_TAG,
            "findProjectByFuzzyName targets=$normalizedTargets requireParentMatch=$requireParentMatch -> no match"
        )
    }

    return bestId
}

private fun SupportSQLiteDatabase.queryUniqueId(query: String, args: Array<String>): String? {
    val cursor = this.query(query, args)
    cursor.use {
        if (!it.moveToFirst()) return null
        val first = it.getString(it.getColumnIndexOrThrow("id"))
        return if (it.moveToNext()) null else first
    }
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    val cursor = query("PRAGMA table_info($table)")
    cursor.use {
        val nameIndex = it.getColumnIndex("name")
        while (it.moveToNext()) {
            if (it.getString(nameIndex) == column) {
                return true
            }
        }
    }
    return false
}
