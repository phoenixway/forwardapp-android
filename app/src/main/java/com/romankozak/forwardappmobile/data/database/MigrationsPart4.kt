@file:Suppress("MaxLineLength")

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the shadow canonical Orientation/Aspect/Workspace persistence boundary. */
val MIGRATION_149_150 =
    object : Migration(149, 150) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createOrientationCoreTables(db)
            createOrientationAssessmentTables(db)
            createOrientationRelationTables(db)
            createOrientationBootstrapTables(db)
        }
    }

/** Adds canonical Workspace identity and its local compatibility-bootstrap diagnostics. */
val MIGRATION_150_151 =
    object : Migration(150, 151) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspaces` (
                    `id` TEXT NOT NULL, `nameOverride` TEXT, `descriptionOverride` TEXT,
                    `parentWorkspaceId` TEXT, `roleCode` TEXT, `workspaceOrder` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            createIndices(db, "workspaces", "parentWorkspaceId", "updatedAt", "isDeleted")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspaces_parentWorkspaceId_workspaceOrder` " +
                    "ON `workspaces` (`parentWorkspaceId`, `workspaceOrder`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspace_bootstrap_state` (
                    `id` INTEGER NOT NULL, `version` INTEGER NOT NULL, `status` TEXT NOT NULL,
                    `completedAt` INTEGER, `comparedAt` INTEGER, PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspace_bootstrap_issues` (
                    `id` TEXT NOT NULL, `contextId` TEXT NOT NULL, `code` TEXT NOT NULL,
                    `detail` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `resolvedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            createIndices(db, "workspace_bootstrap_issues", "contextId")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_workspace_bootstrap_issues_contextId_code` " +
                    "ON `workspace_bootstrap_issues` (`contextId`, `code`)",
            )
        }
    }


/** Adds Workspace provenance before canonical-only Workspace creation. */
val MIGRATION_151_152 =
    object : Migration(151, 152) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `workspaces` ADD COLUMN `provenance` TEXT NOT NULL DEFAULT 'CONTEXT_BACKED'")
            db.execSQL("ALTER TABLE `workspaces` ADD COLUMN `sourceContextId` TEXT")
            db.execSQL("UPDATE `workspaces` SET `sourceContextId` = `id` WHERE `sourceContextId` IS NULL")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workspaces_sourceContextId` ON `workspaces` (`sourceContextId`)")
        }
    }


/**
 * Adds the transitional EXECUTION_LOG Workspace owner slot.
 *
 * No migration-time backfill is attempted because provenance-aware Workspace
 * bootstrap does not run inside Room migrations and Context ids can collide
 * with canonical-only Workspace ids.
 */
val MIGRATION_152_153 =
    object : Migration(152, 153) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `context_execution_logs` ADD COLUMN `workspaceId` TEXT")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_context_execution_logs_workspaceId` " +
                    "ON `context_execution_logs` (`workspaceId`)",
            )
        }
    }


/**
 * Makes the legacy Context locator nullable so the same EXECUTION_LOG
 * collection can later host canonical-only Workspace-owned rows.
 *
 * Existing rows are copied unchanged. No canonical-only row is created here,
 * no ownership is inferred, and the legacy Context foreign key is retained.
 * A Workspace FK is intentionally deferred because current restore ordering
 * does not guarantee Workspace rows exist when log rows are inserted.
 */
val MIGRATION_153_154 =
    object : Migration(153, 154) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE `context_execution_logs_new` (
                    `id` TEXT NOT NULL,
                    `contextId` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `details` TEXT,
                    `updatedAt` INTEGER,
                    `synced_at` INTEGER,
                    `is_deleted` INTEGER NOT NULL DEFAULT 0,
                    `version` INTEGER NOT NULL DEFAULT 0,
                    `workspaceId` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`contextId`) REFERENCES `contexts`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `context_execution_logs_new` (
                    `id`, `contextId`, `timestamp`, `type`, `description`,
                    `details`, `updatedAt`, `synced_at`, `is_deleted`,
                    `version`, `workspaceId`
                )
                SELECT
                    `id`, `contextId`, `timestamp`, `type`, `description`,
                    `details`, `updatedAt`, `synced_at`, `is_deleted`,
                    `version`, `workspaceId`
                FROM `context_execution_logs`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `context_execution_logs`")
            db.execSQL(
                "ALTER TABLE `context_execution_logs_new` " +
                    "RENAME TO `context_execution_logs`",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_context_execution_logs_contextId` " +
                    "ON `context_execution_logs` (`contextId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_context_execution_logs_workspaceId` " +
                    "ON `context_execution_logs` (`workspaceId`)",
            )
        }
    }

private fun createOrientationCoreTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `managed_subjects` (
            `id` TEXT NOT NULL, `subjectType` TEXT NOT NULL,
            `title` TEXT NOT NULL, `description` TEXT,
            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
            `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL,
            PRIMARY KEY(`id`), CHECK(`subjectType` IN ('ORIENTATION', 'ASPECT'))
        )
        """.trimIndent(),
    )
    createIndices(db, "managed_subjects", "subjectType", "updatedAt", "isDeleted")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `orientations` (
            `subjectId` TEXT NOT NULL, `kind` TEXT NOT NULL,
            `lifecycle` TEXT, `lifecycleOrigin` TEXT NOT NULL,
            PRIMARY KEY(`subjectId`),
            FOREIGN KEY(`subjectId`) REFERENCES `managed_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    createIndices(db, "orientations", "kind", "lifecycle")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `aspects` (
            `subjectId` TEXT NOT NULL, `parentAspectId` TEXT,
            `aspectOrder` INTEGER NOT NULL, `archived` INTEGER NOT NULL,
            PRIMARY KEY(`subjectId`),
            FOREIGN KEY(`subjectId`) REFERENCES `managed_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    createIndices(db, "aspects", "parentAspectId")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_aspects_parentAspectId_aspectOrder` ON `aspects` (`parentAspectId`, `aspectOrder`)")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `legacy_subject_mappings` (
            `id` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT NOT NULL,
            `subjectId` TEXT NOT NULL, `migrationVersion` INTEGER NOT NULL, `state` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
            `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`subjectId`) REFERENCES `managed_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_legacy_subject_mappings_sourceType_sourceId` ON `legacy_subject_mappings` (`sourceType`, `sourceId`)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_legacy_subject_mappings_subjectId` ON `legacy_subject_mappings` (`subjectId`)")
    createIndices(db, "legacy_subject_mappings", "state")
}

private fun createOrientationAssessmentTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `orientation_assessments` (
            `orientationId` TEXT NOT NULL, `revisionId` TEXT NOT NULL,
            `importanceValue` TEXT, `importanceOrigin` TEXT NOT NULL,
            `impactValue` TEXT, `impactOrigin` TEXT NOT NULL,
            `breadthValue` TEXT, `breadthOrigin` TEXT NOT NULL,
            `expectedSpanValue` TEXT, `expectedSpanOrigin` TEXT NOT NULL,
            `targetWindowValue` TEXT, `targetWindowOrigin` TEXT NOT NULL,
            `attentionTierValue` TEXT, `attentionTierOrigin` TEXT NOT NULL,
            `commitmentValue` TEXT, `commitmentOrigin` TEXT NOT NULL,
            `confidenceValue` TEXT, `confidenceOrigin` TEXT NOT NULL,
            `provenanceJson` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
            `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL,
            PRIMARY KEY(`orientationId`),
            FOREIGN KEY(`orientationId`) REFERENCES `orientations`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_orientation_assessments_revisionId` ON `orientation_assessments` (`revisionId`)")
    createIndices(db, "orientation_assessments", "importanceValue", "impactValue", "attentionTierValue")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `orientation_assessment_revisions` (
            `id` TEXT NOT NULL, `orientationId` TEXT NOT NULL,
            `effectiveFrom` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL,
            `source` TEXT NOT NULL, `reason` TEXT, `assessmentJson` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
            `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`orientationId`) REFERENCES `orientations`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_orientation_assessment_revisions_orientationId_effectiveFrom` ON `orientation_assessment_revisions` (`orientationId`, `effectiveFrom`)")
    createIndices(db, "orientation_assessment_revisions", "recordedAt")
}

private fun createOrientationRelationTables(db: SupportSQLiteDatabase) {
    createRelationTable(
        db = db,
        table = "orientation_relations",
        columns = "`fromOrientationId` TEXT NOT NULL, `toOrientationId` TEXT NOT NULL, `relationType` TEXT NOT NULL, `relationOrder` INTEGER",
        indices = listOf("fromOrientationId", "toOrientationId"),
        uniqueColumns = "`fromOrientationId`, `toOrientationId`, `relationType`",
    )
    createRelationTable(
        db = db,
        table = "aspect_orientation_refs",
        columns = "`aspectId` TEXT NOT NULL, `orientationId` TEXT NOT NULL, `relationType` TEXT NOT NULL, `isPrimary` INTEGER NOT NULL, `refOrder` INTEGER NOT NULL",
        indices = listOf("aspectId", "orientationId"),
        uniqueColumns = "`aspectId`, `orientationId`, `relationType`",
    )
    createRelationTable(
        db = db,
        table = "workspace_bindings",
        columns = "`workspaceId` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `bindingType` TEXT NOT NULL, `isPrimary` INTEGER NOT NULL, `bindingOrder` INTEGER NOT NULL",
        indices = listOf("workspaceId", "subjectId"),
        uniqueColumns = "`workspaceId`, `subjectId`, `bindingType`",
    )
    createRelationTable(
        db = db,
        table = "workspace_capability_instances",
        columns = "`workspaceId` TEXT NOT NULL, `capabilityType` TEXT NOT NULL, `instanceKey` TEXT NOT NULL, `capabilityOrder` INTEGER NOT NULL, `state` TEXT NOT NULL, `configurationVersion` INTEGER NOT NULL, `configuration` TEXT NOT NULL",
        indices = listOf("workspaceId"),
        uniqueColumns = "`workspaceId`, `capabilityType`, `instanceKey`",
    )
    createRelationTable(
        db = db,
        table = "saved_orientation_views",
        columns = "`title` TEXT NOT NULL, `filterAstVersion` INTEGER NOT NULL, `filterJson` TEXT NOT NULL, `sortSpecification` TEXT NOT NULL, `grouping` TEXT, `visibleFieldsJson` TEXT NOT NULL",
        indices = listOf("updatedAt", "isDeleted"),
        uniqueColumns = null,
    )
}

private fun createRelationTable(
    db: SupportSQLiteDatabase,
    table: String,
    columns: String,
    indices: List<String>,
    uniqueColumns: String?,
) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `$table` (
            `id` TEXT NOT NULL, $columns,
            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
            `syncedAt` INTEGER, `isDeleted` INTEGER NOT NULL, `version` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )
    createIndices(db, table, *indices.toTypedArray())
    if (uniqueColumns != null) {
        val suffix = uniqueColumns.replace("`", "").replace(", ", "_")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_$suffix` ON `$table` ($uniqueColumns)")
    }
}

private fun createOrientationBootstrapTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `orientation_bootstrap_state` (
            `id` INTEGER NOT NULL, `version` INTEGER NOT NULL, `status` TEXT NOT NULL,
            `completedAt` INTEGER, `comparedAt` INTEGER, PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `orientation_bootstrap_issues` (
            `id` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT NOT NULL,
            `code` TEXT NOT NULL, `detail` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
            `resolvedAt` INTEGER, PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )
    createIndices(db, "orientation_bootstrap_issues", "sourceType")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_orientation_bootstrap_issues_sourceType_sourceId_code` ON `orientation_bootstrap_issues` (`sourceType`, `sourceId`, `code`)")
}

private fun createIndices(db: SupportSQLiteDatabase, table: String, vararg columns: String) {
    columns.forEach { column ->
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_$column` ON `$table` (`$column`)")
    }
}

/**
 * Adds canonical DIRECTION placement persistence.
 *
 * Legacy direction_items remain the compatibility authority. This migration
 * intentionally creates no projected rows because Workspace/Orientation
 * provenance must be validated by the capability-specific materializer.
 */
val MIGRATION_154_155 =
    object : Migration(154, 155) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspace_direction_entries` (
                    `id` TEXT NOT NULL,
                    `workspaceId` TEXT NOT NULL,
                    `capabilityInstanceId` TEXT NOT NULL,
                    `orientationId` TEXT,
                    `targetWorkspaceId` TEXT,
                    `labelOverride` TEXT,
                    `entryOrder` INTEGER NOT NULL,
                    `provenance` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_direction_entries_workspaceId` " +
                    "ON `workspace_direction_entries` (`workspaceId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_direction_entries_capabilityInstanceId` " +
                    "ON `workspace_direction_entries` (`capabilityInstanceId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_direction_entries_orientationId` " +
                    "ON `workspace_direction_entries` (`orientationId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_direction_entries_targetWorkspaceId` " +
                    "ON `workspace_direction_entries` (`targetWorkspaceId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_direction_entries_provenance` " +
                    "ON `workspace_direction_entries` (`provenance`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_workspace_direction_entries_workspaceId_capabilityInstanceId_entryOrder` " +
                    "ON `workspace_direction_entries` (`workspaceId`, `capabilityInstanceId`, `entryOrder`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workspace_direction_entry_issues` (
                    `id` TEXT NOT NULL,
                    `sourceDirectionItemId` TEXT NOT NULL,
                    `code` TEXT NOT NULL,
                    `detail` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `resolvedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workspace_direction_entry_issues_sourceDirectionItemId` " +
                    "ON `workspace_direction_entry_issues` (`sourceDirectionItemId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_workspace_direction_entry_issues_sourceDirectionItemId_code` " +
                    "ON `workspace_direction_entry_issues` (`sourceDirectionItemId`, `code`)",
            )
        }
    }
