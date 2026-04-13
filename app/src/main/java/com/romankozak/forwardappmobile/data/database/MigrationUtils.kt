@file:Suppress("TooManyFunctions")

package com.romankozak.forwardappmobile.data.database

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.core.data.models.entities.ReservedContextKeys
import java.text.Normalizer
import java.util.UUID

private const val MIGRATION_LOG_TAG = "ForwardMigration"
private const val SHORT_NAME_THRESHOLD = 4
private const val MEDIUM_NAME_THRESHOLD = 8
private const val LONG_NAME_THRESHOLD = 16
private const val SHORT_NAME_DISTANCE = 1
private const val MEDIUM_NAME_DISTANCE = 2
private const val LONG_NAME_DIVISOR = 4
private const val EXTRA_LONG_NAME_DIVISOR = 5
private const val MIN_LONG_NAME_DISTANCE = 2
private const val MIN_EXTRA_LONG_DISTANCE = 3

private data class ProjectDiscoveryScope(
    val parentScopeCandidates: List<String?>,
    val parentCandidates: List<String>,
    val parentScopesFallback: List<String?>,
)

private data class ReservedProjectSpec(
    val key: String,
    val defaultName: String,
    val parentId: String?,
    val legacyNames: List<String> = emptyList(),
    val legacyNamePatterns: List<String> = emptyList(),
    val legacyParentIds: List<String?> = emptyList(),
    val fuzzyNameCandidates: List<String> = emptyList(),
    val createIfMissing: Boolean = true,
)

private data class FuzzyProjectCandidate(
    val id: String,
    val score: Int,
)

private data class ReservedProjectIds(
    val personalManagementId: String,
    val strategicId: String,
    val weekId: String,
    val todayId: String?,
    val mainBeaconsId: String?,
    val mediumTermId: String?,
)

private data class OptionalCoreReservedProjectIds(
    val todayId: String?,
    val mainBeaconsId: String?,
    val mediumTermId: String?,
)

private data class FuzzyProjectEvaluationInput(
    val projectId: String,
    val projectName: String?,
    val parentId: String?,
    val normalizedTargets: List<String>,
    val parentCandidates: List<String?>,
    val requireParentMatch: Boolean,
)

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
    db.execSQL(
        """
        UPDATE projects
        SET name = 'medium-term-strategy'
        WHERE name IN ('Середньострокова програма', 'medium-term-program', 'medium-term-programs')
        """.trimIndent(),
    )
    db.execSQL("UPDATE projects SET name = 'active-quests' WHERE name = 'Активні квести'")
    db.execSQL("UPDATE projects SET name = 'strategic-inbox' WHERE name IN ('Стратегічні цілі', 'strategic-goals')")
    db.execSQL("UPDATE projects SET name = 'strategic-review' WHERE name = 'Стратегічний огляд'")
    db.execSQL("UPDATE projects SET name = 'main-beacons' WHERE name IN ('Головні маяки')")
    db.execSQL(
        """

        """.trimIndent(),
    )
    db.execSQL(
        """
        UPDATE projects
        SET name = 'strategic-programs'
        WHERE name IN ('strategic-program', 'strategic-programs')
        """.trimIndent(),
    )
    db.execSQL(
        """
        DELETE FROM projects
        WHERE name LIKE 'main-beacons-realization%'
           OR system_key = 'main-beacons-realization'
        """.trimIndent(),
    )

    Log.d(
        MIGRATION_LOG_TAG,
        "normalizeSpecialProjectNames: personal-management, strategic, " +
            "medium-term-strategy and reserved_group aliases normalized " +
            "(main-beacons-realization removed)",
    )
}

private fun migrateSpecialProjectsLegacy(db: SupportSQLiteDatabase) {
    val personalManagementProjectId =
        db.querySingleString(
            query = "SELECT id FROM projects WHERE project_type = 'SYSTEM' LIMIT 1",
            column = "id",
        )
    Log.d(MIGRATION_LOG_TAG, "legacy personalManagementProjectId: $personalManagementProjectId")

    personalManagementProjectId ?: return

    db.execSQL(
        "UPDATE projects SET name = 'personal-management' WHERE id = ?",
        arrayOf(personalManagementProjectId),
    )

    val strategicGroupId =
        db.querySingleString(
            query = "SELECT id FROM projects WHERE parentId = ? AND name = 'strategic' LIMIT 1",
            args = arrayOf(personalManagementProjectId),
            column = "id",
        )

    strategicGroupId?.let { alignLegacyStrategicProjects(db, it) }

    db.execSQL(
        "UPDATE projects SET parentId = ? WHERE name = 'medium-term-strategy'",
        arrayOf(personalManagementProjectId),
    )

    val weekProjectId = ensureLegacyWeekProject(db, personalManagementProjectId)
    db.execSQL(
        "UPDATE projects SET parentId = ? WHERE name = 'active-quests'",
        arrayOf(weekProjectId),
    )
}

private fun migrateSpecialProjectsWithSystemKeys(db: SupportSQLiteDatabase) {
    val ids = ensureCoreReservedProjects(db) ?: return
    ensureInboxProject(db, ids)
    ensureStrategicReservedChildren(db, ids.strategicId)

    val strategicProgramsId =
        ensureProjectWithKey(
            db,
            ReservedProjectSpec(
                key = ReservedContextKeys.STRATEGIC_PROGRAMS,
                defaultName = "strategic-programs",
                parentId = ids.strategicId,
                legacyParentIds = listOf(ids.strategicId),
                legacyNames = listOf("strategic-programs", "strategic-program"),
                legacyNamePatterns = listOf("%strategic%program%"),
            ),
        )

    val activeQuestsId =
        ensureProjectWithKey(
            db,
            ReservedProjectSpec(
                key = ReservedContextKeys.ACTIVE_QUESTS,
                defaultName = "active-quests",
                parentId = ids.weekId,
                legacyNames = listOf("active-quests", "Активні квести"),
                legacyNamePatterns = listOf("active-quests%"),
            ),
        )

    cleanUpReservedDuplicates(
        db = db,
        ids = ids,
        strategicProgramsId = strategicProgramsId,
        activeQuestsId = activeQuestsId,
    )
}

private fun ensureProjectWithKey(
    db: SupportSQLiteDatabase,
    spec: ReservedProjectSpec,
): String? {
    var discoveryStrategy: String? = null
    val discoveryScope = buildProjectDiscoveryScope(spec.parentId, spec.legacyParentIds)

    var targetId = db.queryUniqueId("SELECT id FROM projects WHERE system_key = ? LIMIT 2", arrayOf(spec.key))
    if (targetId != null) discoveryStrategy = "system_key"

    if (targetId == null) {
        targetId =
            findLegacyProjectId(
                db = db,
                scope = discoveryScope,
                legacyNames = spec.legacyNames,
                legacyNamePatterns = spec.legacyNamePatterns,
            ).also {
                if (it != null) {
                    discoveryStrategy = if (spec.legacyNames.isNotEmpty()) "legacy_name" else "legacy_pattern"
                }
            }
    }

    if (targetId == null) {
        val fuzzyTargets = buildFuzzyTargets(spec.fuzzyNameCandidates, spec.legacyNames, spec.defaultName, spec.key)
        val fuzzyMatch =
            findFuzzyProjectCandidate(
                db = db,
                fuzzyTargets = fuzzyTargets,
                parentScopeCandidates = discoveryScope.parentScopeCandidates,
            )
        targetId = fuzzyMatch?.id
        if (fuzzyMatch != null) {
            discoveryStrategy = if (fuzzyMatch.score == 0) "fuzzy_parent" else "fuzzy_any"
        }
    }

    if (targetId == null && spec.createIfMissing) {
        targetId = UUID.randomUUID().toString()
        db.execSQL(
            """
            INSERT INTO projects (id, name, parentId, is_expanded, createdAt, scoring_status, system_key)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(targetId, spec.defaultName, spec.parentId, 0, System.currentTimeMillis(), "NOT_ASSESSED", spec.key),
        )
        discoveryStrategy = "created"
    }

    targetId ?: return null

    db.execSQL("UPDATE projects SET system_key = ? WHERE id = ?", arrayOf(spec.key, targetId))
    alignProjectParent(db, targetId, spec.parentId)

    Log.d(
        MIGRATION_LOG_TAG,
        "ensureProjectWithKey[${spec.key}]: resolved $targetId via ${discoveryStrategy ?: "unknown"} (parent=${spec.parentId})",
    )

    return targetId
}

private fun ensureCoreReservedProjects(db: SupportSQLiteDatabase): ReservedProjectIds? {
    val personalManagementId =
        ensureRequiredReservedProject(
            db = db,
            spec =
                ReservedProjectSpec(
                    key = ReservedContextKeys.PERSONAL_MANAGEMENT,
                    defaultName = "personal-management",
                    parentId = null,
                    legacyNames = listOf("personal-management", "special", "Спеціальні"),
                ),
        ) ?: return null

    val strategicId =
        ensureRequiredReservedProject(
            db = db,
            spec =
                ReservedProjectSpec(
                    key = ReservedContextKeys.STRATEGIC,
                    defaultName = "strategic",
                    parentId = personalManagementId,
                    legacyNames = listOf("strategic", "Стратегічні"),
                ),
        ) ?: return null

    val weekId =
        ensureRequiredReservedProject(
            db = db,
            spec =
                ReservedProjectSpec(
                    key = ReservedContextKeys.WEEK,
                    defaultName = "week",
                    parentId = personalManagementId,
                    legacyNames = listOf("week"),
                    legacyNamePatterns = listOf("week%", "Week%"),
                ),
        ) ?: return null

    val optionalIds =
        ensureOptionalCoreReservedProjects(
            db = db,
            personalManagementId = personalManagementId,
            strategicId = strategicId,
        )

    return ReservedProjectIds(
        personalManagementId = personalManagementId,
        strategicId = strategicId,
        weekId = weekId,
        todayId = optionalIds.todayId,
        mainBeaconsId = optionalIds.mainBeaconsId,
        mediumTermId = optionalIds.mediumTermId,
    )
}

private fun ensureRequiredReservedProject(
    db: SupportSQLiteDatabase,
    spec: ReservedProjectSpec,
): String? = ensureProjectWithKey(db, spec)

private fun ensureOptionalCoreReservedProjects(
    db: SupportSQLiteDatabase,
    personalManagementId: String,
    strategicId: String,
): OptionalCoreReservedProjectIds =
    OptionalCoreReservedProjectIds(
        todayId =
            ensureProjectWithKey(
                db,
                ReservedProjectSpec(
                    key = ReservedContextKeys.TODAY,
                    defaultName = "today",
                    parentId = personalManagementId,
                    legacyNames = listOf("today"),
                ),
            ),
        mainBeaconsId =
            ensureProjectWithKey(
                db,
                ReservedProjectSpec(
                    key = ReservedContextKeys.MAIN_BEACONS,
                    defaultName = "main-beacons",
                    parentId = personalManagementId,
                    legacyNames = listOf("main-beacons"),
                    legacyNamePatterns = listOf("main-beacons%"),
                ),
            ),
        mediumTermId =
            ensureProjectWithKey(
                db,
                ReservedProjectSpec(
                    key = ReservedContextKeys.MEDIUM_TERM_STRATEGY,
                    defaultName = "medium-term-strategy",
                    parentId = personalManagementId,
                    legacyParentIds = listOf(strategicId),
                    legacyNames =
                        listOf(
                            "medium-term-strategy",
                            "medium-term-program",
                            "medium-term-programs",
                            "Середньострокова програма",
                        ),
                    legacyNamePatterns = listOf("%medium-term%", "%medium%strategy%", "%Середнь%"),
                ),
            ),
    )

private fun ensureInboxProject(
    db: SupportSQLiteDatabase,
    ids: ReservedProjectIds,
) {
    ensureProjectWithKey(
        db,
        ReservedProjectSpec(
            key = ReservedContextKeys.INBOX,
            defaultName = "inbox",
            parentId = ids.todayId ?: ids.personalManagementId,
            legacyNames = listOf("inbox", "Вхідні"),
            legacyNamePatterns = listOf("inbox%", "Inbox%"),
            fuzzyNameCandidates = listOf("inbox"),
        ),
    )

    ids.todayId?.let { todayId ->
        db.execSQL(
            """
            UPDATE projects
               SET parentId = ?
             WHERE system_key = ?
               AND (parentId IS NULL OR parentId = ?)
            """.trimIndent(),
            arrayOf(todayId, ReservedContextKeys.INBOX, ids.personalManagementId),
        )
    }
}

private fun ensureStrategicReservedChildren(
    db: SupportSQLiteDatabase,
    strategicId: String,
) {
    listOf(
        ReservedProjectSpec(
            key = ReservedContextKeys.STRATEGIC_INBOX,
            defaultName = "strategic-inbox",
            parentId = strategicId,
            legacyNames = listOf("strategic-inbox", "strategic-goals", "Стратегічні цілі"),
        ),
        ReservedProjectSpec(
            key = ReservedContextKeys.STRATEGIC_REVIEW,
            defaultName = "strategic-review",
            parentId = strategicId,
            legacyNames = listOf("strategic-review", "Стратегічний огляд"),
        ),
        ReservedProjectSpec(
            key = ReservedContextKeys.MISSION,
            defaultName = "mission",
            parentId = strategicId,
            legacyParentIds = listOf(strategicId),
            legacyNames = listOf("mission", "Місія"),
        ),
        ReservedProjectSpec(
            key = ReservedContextKeys.LONG_TERM_STRATEGY,
            defaultName = "long-term-strategy",
            parentId = strategicId,
            legacyParentIds = listOf(strategicId),
            legacyNames = listOf("long-term-strategy", "Довгострокова стратегія"),
        ),
    ).forEach { spec ->
        ensureProjectWithKey(db, spec)
    }
}

private fun cleanUpReservedDuplicates(
    db: SupportSQLiteDatabase,
    ids: ReservedProjectIds,
    strategicProgramsId: String?,
    activeQuestsId: String?,
) {
    cleanUpDuplicateReservedProjects(db, "week", ids.weekId)
    cleanUpDuplicateReservedProjects(db, "main-beacons", ids.mainBeaconsId)
    cleanUpDuplicateReservedProjects(db, "today", ids.todayId)
    cleanUpDuplicateReservedProjects(db, "medium-term-strategy", ids.mediumTermId)
    cleanUpDuplicateReservedProjects(db, "strategic-programs", strategicProgramsId)
    cleanUpDuplicateReservedProjects(db, "active-quests", activeQuestsId)
}

private fun alignLegacyStrategicProjects(
    db: SupportSQLiteDatabase,
    strategicGroupId: String,
) {
    db.execSQL(
        "UPDATE projects SET parentId = ? WHERE name = 'long-term-strategy'",
        arrayOf(strategicGroupId),
    )

    val strategicPrograms =
        db.querySingleRow(
            query = "SELECT id, parentId FROM projects WHERE name = 'strategic-programs' LIMIT 1",
            columns = listOf("id", "parentId"),
        )

    if (strategicPrograms == null) {
        insertLegacyProject(
            db = db,
            id = UUID.randomUUID().toString(),
            name = "strategic-programs",
            parentId = strategicGroupId,
        )
        return
    }

    val strategicProgramsId = strategicPrograms["id"]
    val currentParentId = strategicPrograms["parentId"]
    if (strategicProgramsId != null && currentParentId != strategicGroupId) {
        db.execSQL(
            "UPDATE projects SET parentId = ? WHERE id = ?",
            arrayOf(strategicGroupId, strategicProgramsId),
        )
    }
}

private fun ensureLegacyWeekProject(
    db: SupportSQLiteDatabase,
    personalManagementProjectId: String,
): String {
    val existingWeekId =
        db.querySingleString(
            query = "SELECT id FROM projects WHERE parentId = ? AND name = 'week' LIMIT 1",
            args = arrayOf(personalManagementProjectId),
            column = "id",
        )
    if (existingWeekId != null) return existingWeekId

    val newWeekId = UUID.randomUUID().toString()
    insertLegacyProject(
        db = db,
        id = newWeekId,
        name = "week",
        parentId = personalManagementProjectId,
    )
    return newWeekId
}

private fun insertLegacyProject(
    db: SupportSQLiteDatabase,
    id: String,
    name: String,
    parentId: String?,
) {
    db.execSQL(
        """
        INSERT INTO projects (id, name, parentId, is_expanded, createdAt, scoring_status)
        VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            id,
            name,
            parentId,
            0,
            System.currentTimeMillis(),
            "NOT_ASSESSED",
        ),
    )
}

private fun buildProjectDiscoveryScope(
    parentId: String?,
    legacyParentIds: List<String?>,
): ProjectDiscoveryScope {
    val parentScopeCandidates = (listOf(parentId) + legacyParentIds).distinct()
    val parentCandidates = parentScopeCandidates.filterNotNull()
    val parentScopesFallback =
        if (parentScopeCandidates.isEmpty()) {
            listOf<String?>(null)
        } else {
            parentScopeCandidates
        }
    return ProjectDiscoveryScope(
        parentScopeCandidates = parentScopeCandidates,
        parentCandidates = parentCandidates,
        parentScopesFallback = parentScopesFallback,
    )
}

private fun findLegacyProjectId(
    db: SupportSQLiteDatabase,
    scope: ProjectDiscoveryScope,
    legacyNames: List<String>,
    legacyNamePatterns: List<String>,
): String? {
    return findLegacyProjectIdByNames(db, scope.parentCandidates, legacyNames)
        ?: findLegacyProjectIdByPatterns(db, scope.parentScopesFallback, legacyNamePatterns)
}

private fun findLegacyProjectIdByNames(
    db: SupportSQLiteDatabase,
    parentCandidates: List<String>,
    legacyNames: List<String>,
): String? {
    if (legacyNames.isEmpty()) return null

    if (parentCandidates.isNotEmpty()) {
        parentCandidates.forEach { parent ->
            val placeholders = legacyNames.joinToString(",") { "?" }
            val args = arrayOf(parent) + legacyNames.toTypedArray()
            db.queryUniqueId(
                "SELECT id FROM projects WHERE parentId = ? AND name IN ($placeholders) LIMIT 2",
                args,
            )?.let { return it }
        }
    }

    val placeholders = legacyNames.joinToString(",") { "?" }
    return db.queryUniqueId(
        "SELECT id FROM projects WHERE name IN ($placeholders) LIMIT 2",
        legacyNames.toTypedArray(),
    )
}

private fun findLegacyProjectIdByPatterns(
    db: SupportSQLiteDatabase,
    parentScopesFallback: List<String?>,
    legacyNamePatterns: List<String>,
): String? {
    if (legacyNamePatterns.isEmpty()) return null

    parentScopesFallback.forEach { parent ->
        legacyNamePatterns.forEach { pattern ->
            val condition =
                buildString {
                    if (parent != null) append("parentId = ? AND ")
                    append("name LIKE ? AND system_key IS NULL")
                }
            val args = mutableListOf<String>()
            if (parent != null) args += parent
            args += pattern
            db.queryUniqueId(
                "SELECT id FROM projects WHERE $condition LIMIT 2",
                args.toTypedArray(),
            )?.let { return it }
        }
    }
    return null
}

private fun buildFuzzyTargets(
    fuzzyNameCandidates: List<String>,
    legacyNames: List<String>,
    defaultName: String,
    key: String,
): List<String> =
    (fuzzyNameCandidates + legacyNames + listOf(defaultName, key))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

private fun findFuzzyProjectCandidate(
    db: SupportSQLiteDatabase,
    fuzzyTargets: List<String>,
    parentScopeCandidates: List<String?>,
): FuzzyProjectCandidate? {
    if (fuzzyTargets.isEmpty()) return null

    db.findProjectByFuzzyName(
        targetNames = fuzzyTargets,
        parentCandidates = parentScopeCandidates,
        requireParentMatch = parentScopeCandidates.isNotEmpty(),
    )?.let { return FuzzyProjectCandidate(id = it, score = 0) }

    return db.findProjectByFuzzyName(
        targetNames = fuzzyTargets,
        parentCandidates = parentScopeCandidates,
        requireParentMatch = false,
    )?.let { FuzzyProjectCandidate(id = it, score = 1) }
}

private fun alignProjectParent(
    db: SupportSQLiteDatabase,
    targetId: String,
    parentId: String?,
) {
    val currentParentId =
        db.querySingleString(
            query = "SELECT parentId FROM projects WHERE id = ?",
            args = arrayOf(targetId),
            column = "parentId",
        )
    val normalizedCurrentParent = currentParentId?.takeIf { it.isNotBlank() && !it.equals("null", true) }

    if (parentId != null) {
        if (normalizedCurrentParent == null) {
            db.execSQL("UPDATE projects SET parentId = ? WHERE id = ?", arrayOf(parentId, targetId))
        }
    } else if (normalizedCurrentParent != null) {
        db.execSQL("UPDATE projects SET parentId = NULL WHERE id = ?", arrayOf(targetId))
    }
}

private fun cleanUpDuplicateReservedProjects(
    db: SupportSQLiteDatabase,
    defaultName: String,
    canonicalId: String?,
) {
    canonicalId ?: return

    val deleteQuery =
        """
        DELETE FROM projects
        WHERE name = ?
          AND system_key IS NULL
          AND id != ?
        """.trimIndent()
    db.execSQL(deleteQuery, arrayOf(defaultName, canonicalId))
}

private val DIACRITICS_REGEX = "\\p{Mn}+".toRegex()
private val NON_ALPHANUMERIC_REGEX = "[^a-z0-9]+".toRegex()

private fun normalizeProjectName(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val withoutDiacritics =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
    val normalized = NON_ALPHANUMERIC_REGEX.replace(withoutDiacritics, "-").trim('-')
    return normalized.ifBlank { null }
}

private fun computeFuzzyThreshold(target: String): Int {
    return when {
        target.length <= SHORT_NAME_THRESHOLD -> SHORT_NAME_DISTANCE
        target.length <= MEDIUM_NAME_THRESHOLD -> MEDIUM_NAME_DISTANCE
        target.length <= LONG_NAME_THRESHOLD -> maxOf(MIN_LONG_NAME_DISTANCE, target.length / LONG_NAME_DIVISOR)
        else -> maxOf(MIN_EXTRA_LONG_DISTANCE, target.length / EXTRA_LONG_NAME_DIVISOR)
    }
}

private fun levenshteinDistance(
    left: String,
    right: String,
): Int {
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
    requireParentMatch: Boolean,
): String? {
    val normalizedTargets = targetNames.mapNotNull { normalizeProjectName(it) }.distinct()
    if (normalizedTargets.isEmpty()) return null

    val cursor = query("SELECT id, parentId, name FROM projects WHERE system_key IS NULL")
    var bestCandidate: FuzzyProjectCandidate? = null

    cursor.use {
        val idIndex = it.getColumnIndexOrThrow("id")
        val nameIndex = it.getColumnIndexOrThrow("name")
        val parentIndex = it.getColumnIndexOrThrow("parentId")
        while (it.moveToNext()) {
            val rowCandidate =
                evaluateFuzzyProjectCandidate(
                    input =
                        FuzzyProjectEvaluationInput(
                            projectId = it.getString(idIndex),
                            projectName = it.getString(nameIndex),
                            parentId = it.getString(parentIndex),
                            normalizedTargets = normalizedTargets,
                            parentCandidates = parentCandidates,
                            requireParentMatch = requireParentMatch,
                        ),
                )
            if (rowCandidate != null && (bestCandidate == null || rowCandidate.score < bestCandidate!!.score)) {
                bestCandidate = rowCandidate
            }
        }
    }

    if (bestCandidate != null) {
        Log.d(
            MIGRATION_LOG_TAG,
            "findProjectByFuzzyName targets=$normalizedTargets " +
                "requireParentMatch=$requireParentMatch -> ${bestCandidate!!.id} (score=${bestCandidate!!.score})",
        )
    } else {
        Log.d(
            MIGRATION_LOG_TAG,
            "findProjectByFuzzyName targets=$normalizedTargets requireParentMatch=$requireParentMatch -> no match",
        )
    }

    return bestCandidate?.id
}

private fun evaluateFuzzyProjectCandidate(
    input: FuzzyProjectEvaluationInput,
): FuzzyProjectCandidate? {
    val parentMatches =
        input.parentCandidates.isEmpty() || input.parentCandidates.contains(input.parentId)
    if (input.requireParentMatch && !parentMatches) return null

    val normalizedName = normalizeProjectName(input.projectName) ?: return null
    return input.normalizedTargets
        .mapNotNull { target ->
            buildFuzzyCandidate(
                projectId = input.projectId,
                normalizedName = normalizedName,
                target = target,
                parentMatches = parentMatches,
            )
        }.minByOrNull { it.score }
}

private fun buildFuzzyCandidate(
    projectId: String,
    normalizedName: String,
    target: String,
    parentMatches: Boolean,
): FuzzyProjectCandidate? {
    val distance = levenshteinDistance(normalizedName, target)
    val threshold = computeFuzzyThreshold(target)
    if (distance > threshold) return null

    val penalty = if (parentMatches) 0 else 1
    return FuzzyProjectCandidate(
        id = projectId,
        score = distance + penalty,
    )
}

private fun SupportSQLiteDatabase.querySingleString(
    query: String,
    args: Array<Any?> = emptyArray(),
    column: String,
): String? {
    query(query, args).use { cursor ->
        if (!cursor.moveToFirst()) return null
        return cursor.getString(cursor.getColumnIndexOrThrow(column))
    }
}

private fun SupportSQLiteDatabase.querySingleRow(
    query: String,
    args: Array<Any?> = emptyArray(),
    columns: List<String>,
): Map<String, String?>? {
    query(query, args).use { cursor ->
        if (!cursor.moveToFirst()) return null
        return columns.associateWith { column ->
            cursor.getString(cursor.getColumnIndexOrThrow(column))
        }
    }
}

private fun SupportSQLiteDatabase.queryUniqueId(
    query: String,
    args: Array<String>,
): String? {
    val cursor = this.query(query, args)
    cursor.use {
        if (!it.moveToFirst()) return null
        val first = it.getString(it.getColumnIndexOrThrow("id"))
        return if (it.moveToNext()) null else first
    }
}

internal fun SupportSQLiteDatabase.hasColumn(
    table: String,
    column: String,
): Boolean {
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
