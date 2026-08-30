package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Frozen schema-156 -> 157 KEY_PROBLEMS hard cutover.
 *
 * Historical migration semantics are deliberately private to this file.
 * Do not replace them with runtime repositories, capability registries,
 * parsers, codecs, or mutable role definitions.
 */
val MIGRATION_156_157 =
    object : Migration(156, 157) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCanonicalTables(db)

            check(canonicalTablesAreEmpty(db)) {
                "KEY_PROBLEMS cutover blocked: canonical schema already contains data"
            }

            val sources = loadLegacySources(db)
            val contexts = loadContexts(db)
            val workspaces = loadContextBackedWorkspaces(db)
            val configurations = loadConfigurations(db)
            val knownAttachmentIds = loadAttachmentIds(db)
            val existingCapabilities = loadKeyProblemsCapabilities(db)

            val diagnostics = mutableListOf<String>()
            val parsedSources = sources.map { parseSource(it, diagnostics) }

            val duplicateProblemIds =
                parsedSources
                    .flatMap { parsed -> parsed.problems.map { it.id to parsed.source.contextId } }
                    .groupBy({ it.first }, { it.second })
                    .filterValues { owners -> owners.size > 1 }

            duplicateProblemIds.forEach { (id, owners) ->
                diagnostics +=
                    "DUPLICATE_ISSUE_ID: $id occurs in legacy positions ${owners.joinToString()}"
            }

            val capabilityPlans = linkedMapOf<String, CapabilityPlan>()
            val capabilityIdsByWorkspace = mutableMapOf<String, String>()

            val contextsNeedingCapability =
                buildSet {
                    contexts.values
                        .filter { context ->
                            !context.isDeleted &&
                                isKeyProblemsEnabled(
                                    context = context,
                                    configuration = configurations[context.id],
                                    diagnostics = diagnostics,
                                )
                        }.forEach { add(it.id) }
                    sources.forEach { add(it.contextId) }
                }

            contextsNeedingCapability.forEach { contextId ->
                val context = contexts[contextId]
                if (context == null) {
                    diagnostics +=
                        "UNRESOLVED_OWNER_CONTEXT: Context $contextId does not exist"
                    return@forEach
                }

                val workspace = workspaces[contextId]
                if (workspace == null) {
                    diagnostics +=
                        "UNRESOLVED_OWNER_WORKSPACE: Context $contextId has no provenance-backed Workspace"
                    return@forEach
                }

                val enabled =
                    !context.isDeleted &&
                        isKeyProblemsEnabled(
                            context = context,
                            configuration = configurations[context.id],
                            diagnostics = diagnostics,
                        )

                val plan =
                    planCapability(
                        db = db,
                        context = context,
                        workspace = workspace,
                        existing = existingCapabilities[workspace.id],
                        enabled = enabled,
                        now = System.currentTimeMillis(),
                        diagnostics = diagnostics,
                    )

                if (plan != null) {
                    capabilityPlans[workspace.id] = plan
                    capabilityIdsByWorkspace[workspace.id] = plan.id
                }
            }

            val canonicalProblems = mutableListOf<Problem157>()
            val workspaceRefs = mutableListOf<WorkspaceRef157>()
            val attachmentRefs = mutableListOf<AttachmentRef157>()

            parsedSources.forEach { parsed ->
                val ownerContext = contexts[parsed.source.contextId]
                if (ownerContext == null) {
                    diagnostics +=
                        "UNRESOLVED_OWNER_CONTEXT: Context ${parsed.source.contextId} does not exist"
                    return@forEach
                }

                val workspace = workspaces[parsed.source.contextId]
                if (workspace == null) {
                    diagnostics +=
                        "UNRESOLVED_OWNER_WORKSPACE: Context ${parsed.source.contextId} has no provenance-backed Workspace"
                    return@forEach
                }

                if (parsed.problems.isNotEmpty() && ownerContext.isDeleted) {
                    diagnostics +=
                        "CONTRACT_VIOLATION: live KEY_PROBLEMS content belongs to deleted Context ${ownerContext.id}"
                    return@forEach
                }

                if (parsed.problems.isNotEmpty() && workspace.isDeleted) {
                    diagnostics +=
                        "CONTRACT_VIOLATION: live KEY_PROBLEMS content belongs to deleted Workspace ${workspace.id}"
                    return@forEach
                }

                val capabilityId = capabilityIdsByWorkspace[workspace.id]
                if (capabilityId == null) {
                    diagnostics +=
                        "UNRESOLVED_CAPABILITY_INSTANCE: Workspace ${workspace.id} has no KEY_PROBLEMS capability anchor"
                    return@forEach
                }

                parsed.problems
                    .sortedWith(compareBy<ParsedProblem156> { it.sourceOrder }.thenBy { it.sourceIndex })
                    .forEachIndexed { canonicalOrder, problem ->
                        canonicalProblems +=
                            Problem157(
                                id = problem.id,
                                workspaceId = workspace.id,
                                capabilityInstanceId = capabilityId,
                                title = problem.title,
                                description = problem.description,
                                status = problem.status,
                                problemOrder = canonicalOrder.toLong(),
                                createdAt = problem.createdAt,
                                updatedAt = problem.updatedAt,
                            )

                        problem.relatedContextIds.forEach { relatedContextId ->
                            val targetWorkspace = workspaces[relatedContextId]
                            if (targetWorkspace == null) {
                                diagnostics +=
                                    "UNRESOLVED_RELATED_WORKSPACE: issue ${problem.id} references Context $relatedContextId"
                            } else {
                                workspaceRefs +=
                                    WorkspaceRef157(
                                        id = workspaceRefId(problem.id, targetWorkspace.id),
                                        problemId = problem.id,
                                        targetWorkspaceId = targetWorkspace.id,
                                        createdAt = problem.createdAt,
                                        updatedAt = problem.updatedAt,
                                    )
                            }
                        }

                        problem.relatedAttachmentIds.forEach { attachmentId ->
                            if (attachmentId !in knownAttachmentIds) {
                                diagnostics +=
                                    "UNRESOLVED_ATTACHMENT: issue ${problem.id} references Attachment $attachmentId"
                            } else {
                                attachmentRefs +=
                                    AttachmentRef157(
                                        id = attachmentRefId(problem.id, attachmentId),
                                        problemId = problem.id,
                                        attachmentId = attachmentId,
                                        createdAt = problem.createdAt,
                                        updatedAt = problem.updatedAt,
                                    )
                            }
                        }
                    }
            }

            addCanonicalContractDiagnostics(
                problems = canonicalProblems,
                workspaceRefs = workspaceRefs,
                attachmentRefs = attachmentRefs,
                diagnostics = diagnostics,
            )

            val sourceProblemCount = parsedSources.sumOf { it.problems.size }
            if (canonicalProblems.size != sourceProblemCount) {
                diagnostics +=
                    "SOURCE_ACCOUNTING: parsed $sourceProblemCount problems but planned ${canonicalProblems.size}"
            }

            check(diagnostics.isEmpty()) {
                "KEY_PROBLEMS cutover blocked:\n${diagnostics.distinct().joinToString("\n")}"
            }

            capabilityPlans.values.forEach { applyCapabilityPlan(db, it) }
            insertProblems(db, canonicalProblems)
            insertWorkspaceRefs(db, workspaceRefs)
            insertAttachmentRefs(db, attachmentRefs)

            check(
                scalarLong(db, "SELECT COUNT(*) FROM workspace_problems") ==
                    canonicalProblems.size.toLong(),
            ) {
                "KEY_PROBLEMS cutover blocked: problem accounting mismatch after insert"
            }
            check(
                scalarLong(db, "SELECT COUNT(*) FROM workspace_problem_workspace_refs") ==
                    workspaceRefs.size.toLong(),
            ) {
                "KEY_PROBLEMS cutover blocked: workspace-ref accounting mismatch after insert"
            }
            check(
                scalarLong(db, "SELECT COUNT(*) FROM workspace_problem_attachment_refs") ==
                    attachmentRefs.size.toLong(),
            ) {
                "KEY_PROBLEMS cutover blocked: attachment-ref accounting mismatch after insert"
            }

            db.execSQL("DROP TABLE context_key_problems")
        }
    }

private fun createCanonicalTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS workspace_problems (
            id TEXT NOT NULL,
            workspaceId TEXT NOT NULL,
            capabilityInstanceId TEXT NOT NULL,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            status TEXT NOT NULL,
            problemOrder INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncedAt INTEGER,
            isDeleted INTEGER NOT NULL,
            version INTEGER NOT NULL,
            PRIMARY KEY(id)
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_problems_workspaceId ON workspace_problems(workspaceId)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_problems_capabilityInstanceId ON workspace_problems(capabilityInstanceId)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_problems_updatedAt ON workspace_problems(updatedAt)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_workspace_problems_isDeleted ON workspace_problems(isDeleted)")
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problems_capabilityInstanceId_problemOrder " +
            "ON workspace_problems(capabilityInstanceId, problemOrder)",
    )

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS workspace_problem_workspace_refs (
            id TEXT NOT NULL,
            problemId TEXT NOT NULL,
            targetWorkspaceId TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncedAt INTEGER,
            isDeleted INTEGER NOT NULL,
            version INTEGER NOT NULL,
            PRIMARY KEY(id)
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_workspace_refs_problemId " +
            "ON workspace_problem_workspace_refs(problemId)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_workspace_refs_targetWorkspaceId " +
            "ON workspace_problem_workspace_refs(targetWorkspaceId)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_workspace_refs_updatedAt " +
            "ON workspace_problem_workspace_refs(updatedAt)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_workspace_refs_isDeleted " +
            "ON workspace_problem_workspace_refs(isDeleted)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_workspace_refs_problemId_targetWorkspaceId " +
            "ON workspace_problem_workspace_refs(problemId, targetWorkspaceId)",
    )

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS workspace_problem_attachment_refs (
            id TEXT NOT NULL,
            problemId TEXT NOT NULL,
            attachmentId TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncedAt INTEGER,
            isDeleted INTEGER NOT NULL,
            version INTEGER NOT NULL,
            PRIMARY KEY(id)
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_attachment_refs_problemId " +
            "ON workspace_problem_attachment_refs(problemId)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_attachment_refs_attachmentId " +
            "ON workspace_problem_attachment_refs(attachmentId)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_attachment_refs_updatedAt " +
            "ON workspace_problem_attachment_refs(updatedAt)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_attachment_refs_isDeleted " +
            "ON workspace_problem_attachment_refs(isDeleted)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_workspace_problem_attachment_refs_problemId_attachmentId " +
            "ON workspace_problem_attachment_refs(problemId, attachmentId)",
    )
}

private fun canonicalTablesAreEmpty(db: SupportSQLiteDatabase): Boolean =
    scalarLong(db, "SELECT COUNT(*) FROM workspace_problems") == 0L &&
        scalarLong(db, "SELECT COUNT(*) FROM workspace_problem_workspace_refs") == 0L &&
        scalarLong(db, "SELECT COUNT(*) FROM workspace_problem_attachment_refs") == 0L

private fun loadLegacySources(db: SupportSQLiteDatabase): List<LegacySource156> =
    db.query(
        """
        SELECT context_id, payload_json, updated_at
        FROM context_key_problems
        ORDER BY context_id
        """.trimIndent(),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    LegacySource156(
                        contextId = cursor.getString(0),
                        payloadJson = cursor.getString(1),
                        updatedAt = cursor.getLong(2),
                    ),
                )
            }
        }
    }

private fun loadContexts(db: SupportSQLiteDatabase): Map<String, Context156> =
    db.query(
        """
        SELECT id, createdAt, is_deleted, role_code
        FROM contexts
        """.trimIndent(),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                put(
                    id,
                    Context156(
                        id = id,
                        createdAt = cursor.getLong(1),
                        isDeleted = cursor.getInt(2) != 0,
                        roleCode = if (cursor.isNull(3)) null else cursor.getString(3),
                    ),
                )
            }
        }
    }

private fun loadContextBackedWorkspaces(db: SupportSQLiteDatabase): Map<String, Workspace156> =
    db.query(
        """
        SELECT id, sourceContextId, isDeleted
        FROM workspaces
        WHERE provenance = 'CONTEXT_BACKED'
          AND sourceContextId IS NOT NULL
        """.trimIndent(),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(
                    cursor.getString(1),
                    Workspace156(
                        id = cursor.getString(0),
                        sourceContextId = cursor.getString(1),
                        isDeleted = cursor.getInt(2) != 0,
                    ),
                )
            }
        }
    }

private fun loadConfigurations(db: SupportSQLiteDatabase): Map<String, Configuration156> =
    db.query(
        """
        SELECT contextId, base_preset_code, experimental_capability_ids, apply_mode
        FROM context_structures
        """.trimIndent(),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(
                    cursor.getString(0),
                    Configuration156(
                        contextId = cursor.getString(0),
                        basePresetCode = if (cursor.isNull(1)) null else cursor.getString(1),
                        experimentalCapabilityIds = cursor.getString(2),
                        applyMode = cursor.getString(3),
                    ),
                )
            }
        }
    }

private fun loadAttachmentIds(db: SupportSQLiteDatabase): Set<String> =
    db.query("SELECT id FROM attachments").use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }

private fun loadKeyProblemsCapabilities(db: SupportSQLiteDatabase): Map<String, Capability156> =
    db.query(
        """
        SELECT
            id,
            workspaceId,
            capabilityOrder,
            state,
            configurationVersion,
            configuration,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        FROM workspace_capability_instances
        WHERE capabilityType = 'KEY_PROBLEMS'
          AND instanceKey = 'default'
        """.trimIndent(),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                val workspaceId = cursor.getString(1)
                check(workspaceId !in this) {
                    "KEY_PROBLEMS cutover blocked: duplicate default capability for Workspace $workspaceId"
                }
                put(
                    workspaceId,
                    Capability156(
                        id = cursor.getString(0),
                        workspaceId = workspaceId,
                        capabilityOrder = cursor.getLong(2),
                        state = cursor.getString(3),
                        configurationVersion = cursor.getInt(4),
                        configuration = cursor.getString(5),
                        createdAt = cursor.getLong(6),
                        updatedAt = cursor.getLong(7),
                        syncedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
                        isDeleted = cursor.getInt(9) != 0,
                        version = cursor.getLong(10),
                    ),
                )
            }
        }
    }

private fun parseSource(
    source: LegacySource156,
    diagnostics: MutableList<String>,
): ParsedSource156 {
    val root =
        runCatching { JsonParser.parseString(source.payloadJson) }.getOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: run {
                diagnostics += "INVALID_JSON: Context ${source.contextId} payload must be a JSON object"
                return ParsedSource156(source, emptyList())
            }

    val tracker = root.get("issues")
    if (tracker != null) {
        if (!tracker.isJsonArray) {
            diagnostics += "INVALID_PAYLOAD_SHAPE: Context ${source.contextId} issues must be an array"
            return ParsedSource156(source, emptyList())
        }

        val parsed = parseTracker(source, tracker.asJsonArray, diagnostics)
        if (parsed.isNotEmpty() || diagnostics.any { it.contains("Context ${source.contextId}") }) {
            return ParsedSource156(source, parsed)
        }
    }

    return ParsedSource156(source, parseLegacy(source, root, diagnostics))
}

private fun parseTracker(
    source: LegacySource156,
    rawIssues: JsonArray,
    diagnostics: MutableList<String>,
): List<ParsedProblem156> {
    val rawIds =
        rawIssues.mapNotNull { element ->
            element.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.requiredString("id")
        }

    rawIds.groupingBy { it }.eachCount()
        .filterValues { it > 1 }
        .keys
        .forEach { id ->
            diagnostics +=
                "DUPLICATE_ISSUE_ID: Context ${source.contextId} contains duplicate issue $id"
        }

    return buildList {
        rawIssues.forEachIndexed { index, element ->
            val item =
                element.takeIf { it.isJsonObject }?.asJsonObject
                    ?: run {
                        diagnostics +=
                            "INVALID_ISSUE: Context ${source.contextId} issue $index must be an object"
                        return@forEachIndexed
                    }

            val id =
                item.requiredString("id")
                    ?: run {
                        diagnostics +=
                            "BLANK_ISSUE_ID: Context ${source.contextId} issue $index has no non-blank id"
                        return@forEachIndexed
                    }

            val title = item.optionalString("title", "", source, id, diagnostics) ?: return@forEachIndexed
            val description =
                item.optionalString("description", "", source, id, diagnostics)
                    ?: return@forEachIndexed
            val status = parseStatus(item, source, id, diagnostics) ?: return@forEachIndexed
            val contextIds =
                item.optionalStringArray("relatedContextIds", source, id, diagnostics)
                    ?: return@forEachIndexed
            val attachmentIds =
                item.optionalStringArray("relatedAttachmentIds", source, id, diagnostics)
                    ?: return@forEachIndexed
            val order = item.optionalLong("order", 0L, source, id, diagnostics) ?: return@forEachIndexed
            val createdAt =
                item.optionalLong("createdAt", source.updatedAt, source, id, diagnostics)
                    ?: return@forEachIndexed
            val updatedAt =
                item.optionalLong("updatedAt", source.updatedAt, source, id, diagnostics)
                    ?: return@forEachIndexed

            val dateTime = item.get("dateTime")
            if (dateTime != null && dateTime !is JsonNull && !dateTime.isJsonNull) {
                val value = dateTime.longOrNull()
                if (value == null) {
                    diagnostics +=
                        "INVALID_ISSUE: Context ${source.contextId} issue $id dateTime must be an integer or null"
                } else {
                    diagnostics +=
                        "DATE_TIME_REQUIRES_DECISION: Context ${source.contextId} issue $id has dateTime=$value"
                }
            }

            val normalizedTitle = title.trim()
            val normalizedDescription = description.trim()
            val normalizedContextIds = contextIds.map(String::trim).distinct()
            val normalizedAttachmentIds = attachmentIds.map(String::trim).distinct()

            if (
                normalizedTitle.isEmpty() &&
                normalizedDescription.isEmpty() &&
                normalizedContextIds.isEmpty() &&
                normalizedAttachmentIds.isEmpty()
            ) {
                diagnostics +=
                    "EMPTY_ISSUE: Context ${source.contextId} issue $id has no content or relations"
            }

            add(
                ParsedProblem156(
                    id = id,
                    title = normalizedTitle,
                    description = normalizedDescription,
                    status = status,
                    relatedContextIds = normalizedContextIds,
                    relatedAttachmentIds = normalizedAttachmentIds,
                    sourceOrder = order,
                    sourceIndex = index,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                ),
            )
        }
    }
}

private fun parseLegacy(
    source: LegacySource156,
    root: JsonObject,
    diagnostics: MutableList<String>,
): List<ParsedProblem156> {
    val description =
        root.optionalString("description", "", source, null, diagnostics)
            ?: return emptyList()
    val contextIds =
        root.optionalStringArray("focusContextIds", source, null, diagnostics)
            ?: return emptyList()

    val normalizedDescription = description.trim()
    val normalizedContextIds = contextIds.map(String::trim).distinct()

    if (normalizedDescription.isEmpty() && normalizedContextIds.isEmpty()) {
        return emptyList()
    }

    val title =
        normalizedDescription
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(80)
            ?.trim()
            .orEmpty()
            .ifBlank { "Imported issue" }

    return listOf(
        ParsedProblem156(
            id = "legacy-${source.contextId}",
            title = title,
            description = normalizedDescription,
            status = "OPEN",
            relatedContextIds = normalizedContextIds,
            relatedAttachmentIds = emptyList(),
            sourceOrder = 0L,
            sourceIndex = 0,
            createdAt = source.updatedAt,
            updatedAt = source.updatedAt,
        ),
    )
}

private fun parseStatus(
    item: JsonObject,
    source: LegacySource156,
    id: String,
    diagnostics: MutableList<String>,
): String? {
    val raw =
        item.optionalString("status", "OPEN", source, id, diagnostics)
            ?: return null

    if (raw !in KEY_PROBLEM_STATUSES) {
        diagnostics +=
            "UNKNOWN_STATUS: Context ${source.contextId} issue $id has status $raw"
        return null
    }
    return raw
}

private fun JsonObject.requiredString(key: String): String? =
    get(key)
        ?.takeIf { it.isJsonPrimitive }
        ?.asJsonPrimitive
        ?.takeIf(JsonPrimitive::isString)
        ?.asString
        ?.trim()
        ?.takeIf(String::isNotEmpty)

private fun JsonObject.optionalString(
    key: String,
    default: String,
    source: LegacySource156,
    issueId: String?,
    diagnostics: MutableList<String>,
): String? {
    val element = get(key) ?: return default
    val primitive = element.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
    if (primitive == null || !primitive.isString) {
        diagnostics +=
            "INVALID_ISSUE: Context ${source.contextId} ${issueLabel(issueId)} $key must be a string"
        return null
    }
    return primitive.asString
}

private fun JsonObject.optionalStringArray(
    key: String,
    source: LegacySource156,
    issueId: String?,
    diagnostics: MutableList<String>,
): List<String>? {
    val element = get(key) ?: return emptyList()
    if (!element.isJsonArray) {
        diagnostics +=
            "INVALID_ISSUE: Context ${source.contextId} ${issueLabel(issueId)} $key must contain only strings"
        return null
    }

    val values =
        element.asJsonArray.map { item ->
            item.takeIf { it.isJsonPrimitive }
                ?.asJsonPrimitive
                ?.takeIf(JsonPrimitive::isString)
                ?.asString
        }

    if (values.any { it == null }) {
        diagnostics +=
            "INVALID_ISSUE: Context ${source.contextId} ${issueLabel(issueId)} $key must contain only strings"
        return null
    }

    val normalized = values.filterNotNull().map(String::trim)
    if (normalized.any(String::isEmpty)) {
        diagnostics +=
            "INVALID_ISSUE: Context ${source.contextId} ${issueLabel(issueId)} $key must not contain blank ids"
        return null
    }
    return normalized
}

private fun JsonObject.optionalLong(
    key: String,
    default: Long,
    source: LegacySource156,
    issueId: String,
    diagnostics: MutableList<String>,
): Long? {
    val element = get(key) ?: return default
    val value = element.longOrNull()
    if (value == null) {
        diagnostics +=
            "INVALID_ISSUE: Context ${source.contextId} issue $issueId $key must be an integer"
    }
    return value
}

private fun JsonElement.longOrNull(): Long? =
    runCatching {
        takeIf { it.isJsonPrimitive }
            ?.asJsonPrimitive
            ?.asString
            ?.toLong()
    }.getOrNull()

private fun issueLabel(issueId: String?): String =
    if (issueId == null) "legacy payload" else "issue $issueId"

private fun isKeyProblemsEnabled(
    context: Context156,
    configuration: Configuration156?,
    diagnostics: MutableList<String>,
): Boolean {
    if (context.isDeleted) return false

    val basePresetCode =
        configuration?.basePresetCode ?: context.roleCode ?: DEFAULT_ROLE_CODE
    val applyMode = configuration?.applyMode ?: DEFAULT_APPLY_MODE
    val roleEnabled =
        !applyMode.equals(APPLY_MODE_OVERRIDE, ignoreCase = true) &&
            basePresetCode.trim().lowercase(Locale.ROOT) == CRISIS_CASE_ROLE_CODE

    val experimentalEnabled =
        configuration?.let {
            parseExperimentalCapabilities(
                raw = it.experimentalCapabilityIds,
                contextId = context.id,
                diagnostics = diagnostics,
            ).any { id -> id.trim().lowercase(Locale.ROOT) == KEY_PROBLEMS_LEGACY_ID }
        } ?: false

    return roleEnabled || experimentalEnabled
}

private fun parseExperimentalCapabilities(
    raw: String,
    contextId: String,
    diagnostics: MutableList<String>,
): List<String> {
    val root =
        runCatching { JsonParser.parseString(raw) }.getOrNull()
            ?: run {
                diagnostics +=
                    "INVALID_CAPABILITY_CONFIGURATION: Context $contextId experimental capability ids are malformed"
                return emptyList()
            }

    fun decode(element: JsonElement): String? =
        when {
            element.isJsonPrimitive -> element.asJsonPrimitive.asString
            element.isJsonObject ->
                element.asJsonObject
                    .get("raw")
                    ?.takeIf { it.isJsonPrimitive && !it.isJsonNull }
                    ?.asJsonPrimitive
                    ?.asString
            else -> null
        }

    return when {
        root.isJsonArray -> {
            val decoded = root.asJsonArray.map(::decode)
            if (decoded.any { it == null }) {
                diagnostics +=
                    "INVALID_CAPABILITY_CONFIGURATION: Context $contextId experimental capability ids contain unsupported values"
            }
            decoded.filterNotNull()
        }
        root.isJsonPrimitive || root.isJsonObject -> listOfNotNull(decode(root))
        else -> {
            diagnostics +=
                "INVALID_CAPABILITY_CONFIGURATION: Context $contextId experimental capability ids have unsupported shape"
            emptyList()
        }
    }
}

private fun planCapability(
    db: SupportSQLiteDatabase,
    context: Context156,
    workspace: Workspace156,
    existing: Capability156?,
    enabled: Boolean,
    now: Long,
    diagnostics: MutableList<String>,
): CapabilityPlan? {
    if (workspace.sourceContextId != context.id) {
        diagnostics +=
            "UNRESOLVED_OWNER_WORKSPACE: Workspace ${workspace.id} is not provenance-backed by Context ${context.id}"
        return null
    }

    if (existing != null) {
        return CapabilityPlan(
            id = existing.id,
            workspaceId = workspace.id,
            createdAt = existing.createdAt,
            updatedAt = now,
            version = existing.version + if (existing.matchesDesired(enabled)) 0L else 1L,
            state = if (enabled) KEY_PROBLEMS_ACTIVE_STATE else existing.state,
            isDeleted = !enabled,
            writeRequired = !existing.matchesDesired(enabled),
        )
    }

    val id =
        stableWorkspaceId(
            "CAPABILITY:${context.id}:$KEY_PROBLEMS_CAPABILITY_TYPE:$DEFAULT_INSTANCE_KEY",
        )

    db.query(
        """
        SELECT workspaceId, capabilityType, instanceKey
        FROM workspace_capability_instances
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(id),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            diagnostics +=
                "CANONICAL_ID_COLLISION: deterministic capability id $id is already occupied"
            return null
        }
    }

    return CapabilityPlan(
        id = id,
        workspaceId = workspace.id,
        createdAt = context.createdAt,
        updatedAt = now,
        version = 1L,
        state = KEY_PROBLEMS_ACTIVE_STATE,
        isDeleted = !enabled,
        writeRequired = true,
    )
}

private fun Capability156.matchesDesired(enabled: Boolean): Boolean =
    capabilityOrder == KEY_PROBLEMS_CAPABILITY_ORDER &&
        (!enabled || state == KEY_PROBLEMS_ACTIVE_STATE) &&
        configurationVersion == KEY_PROBLEMS_CONFIGURATION_VERSION &&
        configuration == KEY_PROBLEMS_CONFIGURATION &&
        isDeleted == !enabled

private fun applyCapabilityPlan(
    db: SupportSQLiteDatabase,
    plan: CapabilityPlan,
) {
    if (!plan.writeRequired) return

    val exists =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM workspace_capability_instances WHERE id = '${sqlLiteral(plan.id)}'",
        ) == 1L

    if (exists) {
        db.execSQL(
            """
            UPDATE workspace_capability_instances
            SET
                workspaceId = ?,
                capabilityType = ?,
                instanceKey = ?,
                capabilityOrder = ?,
                state = ?,
                configurationVersion = ?,
                configuration = ?,
                updatedAt = ?,
                syncedAt = NULL,
                isDeleted = ?,
                version = ?
            WHERE id = ?
            """.trimIndent(),
            arrayOf<Any?>(
                plan.workspaceId,
                KEY_PROBLEMS_CAPABILITY_TYPE,
                DEFAULT_INSTANCE_KEY,
                KEY_PROBLEMS_CAPABILITY_ORDER,
                plan.state,
                KEY_PROBLEMS_CONFIGURATION_VERSION,
                KEY_PROBLEMS_CONFIGURATION,
                plan.updatedAt,
                boolInt(plan.isDeleted),
                plan.version,
                plan.id,
            ),
        )
    } else {
        db.execSQL(
            """
            INSERT INTO workspace_capability_instances (
                id,
                workspaceId,
                capabilityType,
                instanceKey,
                capabilityOrder,
                state,
                configurationVersion,
                configuration,
                createdAt,
                updatedAt,
                syncedAt,
                isDeleted,
                version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                plan.id,
                plan.workspaceId,
                KEY_PROBLEMS_CAPABILITY_TYPE,
                DEFAULT_INSTANCE_KEY,
                KEY_PROBLEMS_CAPABILITY_ORDER,
                plan.state,
                KEY_PROBLEMS_CONFIGURATION_VERSION,
                KEY_PROBLEMS_CONFIGURATION,
                plan.createdAt,
                plan.updatedAt,
                boolInt(plan.isDeleted),
                plan.version,
            ),
        )
    }
}

private fun addCanonicalContractDiagnostics(
    problems: List<Problem157>,
    workspaceRefs: List<WorkspaceRef157>,
    attachmentRefs: List<AttachmentRef157>,
    diagnostics: MutableList<String>,
) {
    problems.groupingBy { it.id }.eachCount()
        .filterValues { it > 1 }
        .keys
        .forEach { diagnostics += "CANONICAL_ID_COLLISION: duplicate problem id $it" }

    workspaceRefs.groupingBy { it.id }.eachCount()
        .filterValues { it > 1 }
        .keys
        .forEach { diagnostics += "CANONICAL_ID_COLLISION: duplicate workspace-ref id $it" }

    attachmentRefs.groupingBy { it.id }.eachCount()
        .filterValues { it > 1 }
        .keys
        .forEach { diagnostics += "CANONICAL_ID_COLLISION: duplicate attachment-ref id $it" }

    problems.groupBy { it.capabilityInstanceId }
        .forEach { (capabilityId, owned) ->
            owned.groupingBy { it.problemOrder }.eachCount()
                .filterValues { it > 1 }
                .keys
                .forEach { order ->
                    diagnostics +=
                        "CONTRACT_VIOLATION: capability $capabilityId has duplicate live order $order"
                }
        }

    val problemIds = problems.mapTo(hashSetOf()) { it.id }
    workspaceRefs.filter { it.problemId !in problemIds }.forEach {
        diagnostics += "CONTRACT_VIOLATION: workspace ref ${it.id} has no problem"
    }
    attachmentRefs.filter { it.problemId !in problemIds }.forEach {
        diagnostics += "CONTRACT_VIOLATION: attachment ref ${it.id} has no problem"
    }

    val linked =
        workspaceRefs.mapTo(hashSetOf()) { it.problemId } +
            attachmentRefs.mapTo(hashSetOf()) { it.problemId }

    problems.filter {
        it.title.isBlank() && it.description.isBlank() && it.id !in linked
    }.forEach {
        diagnostics += "CONTRACT_VIOLATION: problem ${it.id} has no content or relations"
    }
}

private fun insertProblems(
    db: SupportSQLiteDatabase,
    rows: List<Problem157>,
) {
    rows.forEach { row ->
        db.execSQL(
            """
            INSERT INTO workspace_problems (
                id,
                workspaceId,
                capabilityInstanceId,
                title,
                description,
                status,
                problemOrder,
                createdAt,
                updatedAt,
                syncedAt,
                isDeleted,
                version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(
                row.id,
                row.workspaceId,
                row.capabilityInstanceId,
                row.title,
                row.description,
                row.status,
                row.problemOrder,
                row.createdAt,
                row.updatedAt,
            ),
        )
    }
}

private fun insertWorkspaceRefs(
    db: SupportSQLiteDatabase,
    rows: List<WorkspaceRef157>,
) {
    rows.forEach { row ->
        db.execSQL(
            """
            INSERT INTO workspace_problem_workspace_refs (
                id,
                problemId,
                targetWorkspaceId,
                createdAt,
                updatedAt,
                syncedAt,
                isDeleted,
                version
            ) VALUES (?, ?, ?, ?, ?, NULL, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(
                row.id,
                row.problemId,
                row.targetWorkspaceId,
                row.createdAt,
                row.updatedAt,
            ),
        )
    }
}

private fun insertAttachmentRefs(
    db: SupportSQLiteDatabase,
    rows: List<AttachmentRef157>,
) {
    rows.forEach { row ->
        db.execSQL(
            """
            INSERT INTO workspace_problem_attachment_refs (
                id,
                problemId,
                attachmentId,
                createdAt,
                updatedAt,
                syncedAt,
                isDeleted,
                version
            ) VALUES (?, ?, ?, ?, ?, NULL, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(
                row.id,
                row.problemId,
                row.attachmentId,
                row.createdAt,
                row.updatedAt,
            ),
        )
    }
}

private fun workspaceRefId(
    problemId: String,
    workspaceId: String,
): String =
    "KEY_PROBLEM_WORKSPACE_REF:${problemId.length}:$problemId:${workspaceId.length}:$workspaceId"

private fun attachmentRefId(
    problemId: String,
    attachmentId: String,
): String =
    "KEY_PROBLEM_ATTACHMENT_REF:${problemId.length}:$problemId:${attachmentId.length}:$attachmentId"

private fun stableWorkspaceId(name: String): String =
    uuidV5(
        namespace = UUID.fromString(KEY_PROBLEMS_NAMESPACE_UUID),
        name = "WORKSPACE:$name",
    ).toString()

private fun uuidV5(
    namespace: UUID,
    name: String,
): UUID {
    val namespaceBytes =
        ByteBuffer.allocate(16)
            .putLong(namespace.mostSignificantBits)
            .putLong(namespace.leastSignificantBits)
            .array()
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(namespaceBytes)
    digest.update(name.toByteArray(StandardCharsets.UTF_8))
    val hash = digest.digest()
    hash[6] = ((hash[6].toInt() and 0x0f) or 0x50).toByte()
    hash[8] = ((hash[8].toInt() and 0x3f) or 0x80).toByte()
    val bytes = ByteBuffer.wrap(hash.copyOf(16))
    return UUID(bytes.long, bytes.long)
}

private fun scalarLong(
    db: SupportSQLiteDatabase,
    sql: String,
): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected scalar result for: $sql" }
        cursor.getLong(0)
    }

private fun sqlLiteral(value: String): String = value.replace("'", "''")

private fun boolInt(value: Boolean): Int = if (value) 1 else 0

private data class LegacySource156(
    val contextId: String,
    val payloadJson: String,
    val updatedAt: Long,
)

private data class ParsedSource156(
    val source: LegacySource156,
    val problems: List<ParsedProblem156>,
)

private data class ParsedProblem156(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val relatedContextIds: List<String>,
    val relatedAttachmentIds: List<String>,
    val sourceOrder: Long,
    val sourceIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

private data class Context156(
    val id: String,
    val createdAt: Long,
    val isDeleted: Boolean,
    val roleCode: String?,
)

private data class Workspace156(
    val id: String,
    val sourceContextId: String,
    val isDeleted: Boolean,
)

private data class Configuration156(
    val contextId: String,
    val basePresetCode: String?,
    val experimentalCapabilityIds: String,
    val applyMode: String,
)

private data class Capability156(
    val id: String,
    val workspaceId: String,
    val capabilityOrder: Long,
    val state: String,
    val configurationVersion: Int,
    val configuration: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

private data class CapabilityPlan(
    val id: String,
    val workspaceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val state: String,
    val isDeleted: Boolean,
    val writeRequired: Boolean,
)

private data class Problem157(
    val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val title: String,
    val description: String,
    val status: String,
    val problemOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

private data class WorkspaceRef157(
    val id: String,
    val problemId: String,
    val targetWorkspaceId: String,
    val createdAt: Long,
    val updatedAt: Long,
)

private data class AttachmentRef157(
    val id: String,
    val problemId: String,
    val attachmentId: String,
    val createdAt: Long,
    val updatedAt: Long,
)

private val KEY_PROBLEM_STATUSES =
    setOf("OPEN", "IN_PROGRESS", "BLOCKED", "RESOLVED", "CLOSED")

private const val KEY_PROBLEMS_NAMESPACE_UUID = "1ae36c1a-cb9d-5e7c-8b3a-3bca70de4830"
private const val KEY_PROBLEMS_CAPABILITY_TYPE = "KEY_PROBLEMS"
private const val KEY_PROBLEMS_CAPABILITY_ORDER = 3L
private const val KEY_PROBLEMS_ACTIVE_STATE = "ACTIVE"
private const val KEY_PROBLEMS_CONFIGURATION_VERSION = 1
private const val KEY_PROBLEMS_CONFIGURATION = "{}"
private const val KEY_PROBLEMS_LEGACY_ID = "key_problems"
private const val DEFAULT_INSTANCE_KEY = "default"
private const val DEFAULT_ROLE_CODE = "default"
private const val CRISIS_CASE_ROLE_CODE = "crisis_case"
private const val DEFAULT_APPLY_MODE = "ADDITIVE"
private const val APPLY_MODE_OVERRIDE = "OVERRIDE"
