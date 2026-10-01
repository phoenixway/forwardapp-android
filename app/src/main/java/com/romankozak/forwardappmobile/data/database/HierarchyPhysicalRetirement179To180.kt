package com.romankozak.forwardappmobile.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal object HierarchyPhysicalRetirement179To180 {
    const val WORKSPACES = "workspaces"
    const val MAIN_BEACONS = "main_beacons"

    private const val NEW_WORKSPACES = "workspaces_h6_new"
    private const val NEW_MAIN_BEACONS = "main_beacons_h6_new"

    internal data class ChildSpec(
        val name: String,
        val backup: String,
        val keys: List<String>,
    )

    val survivingChildren =
        listOf(
            ChildSpec(
                "main_beacon_group_members",
                "h6_backup_main_beacon_group_members",
                listOf("group_id", "beacon_id"),
            ),
            ChildSpec(
                "main_beacon_context_cross_ref",
                "h6_backup_main_beacon_context_cross_ref",
                listOf("beacon_id", "context_id"),
            ),
            ChildSpec(
                "main_beacon_workspace_cross_ref",
                "h6_backup_main_beacon_workspace_cross_ref",
                listOf("beacon_id", "workspace_id"),
            ),
            ChildSpec(
                "main_beacon_attachment_cross_ref",
                "h6_backup_main_beacon_attachment_cross_ref",
                listOf("beacon_id", "attachment_id"),
            ),
            ChildSpec(
                "main_beacon_level_statuses",
                "h6_backup_main_beacon_level_statuses",
                listOf("id"),
            ),
            ChildSpec(
                "workspace_connections",
                "h6_backup_workspace_connections",
                listOf("id"),
            ),
            ChildSpec(
                "workspace_backlog_entries",
                "h6_backup_workspace_backlog_entries",
                listOf("id"),
            ),
            ChildSpec(
                "day_tasks",
                "h6_backup_day_tasks",
                listOf("id"),
            ),
            ChildSpec(
                "system_apps",
                "h6_backup_system_apps",
                listOf("id"),
            ),
            ChildSpec(
                "tactical_missions",
                "h6_backup_tactical_missions",
                listOf("id"),
            ),
            ChildSpec(
                "workspace_tag_refs",
                "h6_backup_workspace_tag_refs",
                listOf("workspaceId", "normalizedTag"),
            ),
            ChildSpec(
                "system_workspace_tag_seed_states",
                "h6_backup_system_workspace_tag_seed_states",
                listOf("workspaceId"),
            ),
        )

    val retainedTables =
        buildList {
            add(WORKSPACES)
            add(MAIN_BEACONS)
            addAll(survivingChildren.map { it.name })
        }

    private val expectedWorkspaceInbound =
        setOf(
            "main_beacon_workspace_cross_ref",
            "workspace_connections",
            "workspace_backlog_entries",
            "day_tasks",
            "system_apps",
            "tactical_missions",
            "workspace_tag_refs",
            "system_workspace_tag_seed_states",
        )

    private val expectedMainBeaconInbound =
        setOf(
            "main_beacon_group_members",
            "main_beacon_parent_links",
            "main_beacon_context_cross_ref",
            "main_beacon_workspace_cross_ref",
            "main_beacon_attachment_cross_ref",
            "main_beacon_level_statuses",
        )

    fun requireExpectedInboundForeignKeys(db: SupportSQLiteDatabase) {
        val workspaceInbound = inboundForeignKeyTables(db, WORKSPACES)
        require(workspaceInbound == expectedWorkspaceInbound) {
            "Unexpected Workspace inbound FK set: $workspaceInbound"
        }

        val beaconInbound = inboundForeignKeyTables(db, MAIN_BEACONS)
        require(beaconInbound == expectedMainBeaconInbound) {
            "Unexpected MainBeacon inbound FK set: $beaconInbound"
        }
    }

    fun createReplacementParents(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `$NEW_WORKSPACES` (
                `id` TEXT NOT NULL,
                `nameOverride` TEXT,
                `descriptionOverride` TEXT,
                `roleCode` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `syncedAt` INTEGER,
                `isDeleted` INTEGER NOT NULL,
                `version` INTEGER NOT NULL,
                `provenance` TEXT NOT NULL DEFAULT 'CONTEXT_BACKED',
                `sourceContextId` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `$NEW_MAIN_BEACONS` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `why_it_matters` TEXT,
                `success_shape` TEXT,
                `failure_shape` TEXT,
                `anti_goal` TEXT,
                `decision_impact` TEXT,
                `readiness_status` TEXT NOT NULL,
                `blocker_text` TEXT,
                `next_action_text` TEXT,
                `beacon_order` INTEGER NOT NULL DEFAULT 0,
                `is_expanded` INTEGER NOT NULL DEFAULT 1,
                `updatedAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }

    fun copyReplacementParents(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `$NEW_WORKSPACES`(
                id, nameOverride, descriptionOverride, roleCode,
                createdAt, updatedAt, syncedAt, isDeleted, version,
                provenance, sourceContextId
            )
            SELECT
                id, nameOverride, descriptionOverride, roleCode,
                createdAt, updatedAt, syncedAt, isDeleted, version,
                provenance, sourceContextId
            FROM `$WORKSPACES`
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT INTO `$NEW_MAIN_BEACONS`(
                id, title, description, why_it_matters, success_shape,
                failure_shape, anti_goal, decision_impact, readiness_status,
                blocker_text, next_action_text, beacon_order, is_expanded,
                updatedAt, createdAt
            )
            SELECT
                id, title, description, why_it_matters, success_shape,
                failure_shape, anti_goal, decision_impact, readiness_status,
                blocker_text, next_action_text, beacon_order, is_expanded,
                updatedAt, createdAt
            FROM `$MAIN_BEACONS`
            """.trimIndent(),
        )
    }

    fun assertReplacementParentsPreserved(db: SupportSQLiteDatabase) {
        assertCountAndKeysPreserved(
            db = db,
            source = WORKSPACES,
            target = NEW_WORKSPACES,
            keys = listOf("id"),
        )
        assertCountAndKeysPreserved(
            db = db,
            source = MAIN_BEACONS,
            target = NEW_MAIN_BEACONS,
            keys = listOf("id"),
        )
    }

    fun backupSurvivingChildren(db: SupportSQLiteDatabase) {
        survivingChildren.forEach { child ->
            db.execSQL(
                "CREATE TABLE `${child.backup}` AS SELECT * FROM `${child.name}`",
            )
            assertCountAndKeysPreserved(
                db = db,
                source = child.name,
                target = child.backup,
                keys = child.keys,
            )
        }
    }

    fun dropOldChildrenAndRetiredLinks(db: SupportSQLiteDatabase) {
        survivingChildren.forEach { child ->
            db.execSQL("DROP TABLE `${child.name}`")
        }
        db.execSQL("DROP TABLE `context_parent_links`")
        db.execSQL("DROP TABLE `main_beacon_parent_links`")

        require(inboundForeignKeyTables(db, WORKSPACES).isEmpty()) {
            "Workspace retirement still has inbound foreign keys"
        }
        require(inboundForeignKeyTables(db, MAIN_BEACONS).isEmpty()) {
            "MainBeacon retirement still has inbound foreign keys"
        }
    }

    fun replaceParentTables(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE `$WORKSPACES`")
        db.execSQL("DROP TABLE `$MAIN_BEACONS`")
        db.execSQL("ALTER TABLE `$NEW_WORKSPACES` RENAME TO `$WORKSPACES`")
        db.execSQL("ALTER TABLE `$NEW_MAIN_BEACONS` RENAME TO `$MAIN_BEACONS`")
    }

    fun createSurvivingChildrenPart1(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `main_beacon_group_members` (
                `group_id` TEXT NOT NULL,
                `beacon_id` TEXT NOT NULL,
                `member_order` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`group_id`, `beacon_id`),
                FOREIGN KEY(`group_id`) REFERENCES `main_beacon_groups`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `main_beacon_context_cross_ref` (
                `beacon_id` TEXT NOT NULL,
                `context_id` TEXT NOT NULL,
                `ref_order` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`beacon_id`, `context_id`),
                FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`context_id`) REFERENCES `contexts`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `main_beacon_workspace_cross_ref` (
                `beacon_id` TEXT NOT NULL,
                `workspace_id` TEXT NOT NULL,
                `ref_order` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`beacon_id`, `workspace_id`),
                FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`workspace_id`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `main_beacon_attachment_cross_ref` (
                `beacon_id` TEXT NOT NULL,
                `attachment_id` TEXT NOT NULL,
                PRIMARY KEY(`beacon_id`, `attachment_id`),
                FOREIGN KEY(`beacon_id`) REFERENCES `main_beacons`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`attachment_id`) REFERENCES `attachments`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `main_beacon_level_statuses` (
                `id` TEXT NOT NULL,
                `main_beacon_id` TEXT NOT NULL,
                `level_type` TEXT NOT NULL,
                `general_status` TEXT NOT NULL,
                `sync_status` TEXT NOT NULL,
                `blocker_text` TEXT,
                `next_action_text` TEXT,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`main_beacon_id`) REFERENCES `main_beacons`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `workspace_connections` (
                `id` TEXT NOT NULL,
                `workspaceId` TEXT NOT NULL,
                `capabilityInstanceId` TEXT NOT NULL,
                `attachmentId` TEXT NOT NULL,
                `connectionOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `syncedAt` INTEGER,
                `isDeleted` INTEGER NOT NULL,
                `version` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`capabilityInstanceId`) REFERENCES `workspace_capability_instances`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`attachmentId`) REFERENCES `attachments`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }

    fun createSurvivingChildrenPart2(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `workspace_backlog_entries` (
                `id` TEXT NOT NULL,
                `workspaceId` TEXT NOT NULL,
                `capabilityInstanceId` TEXT NOT NULL,
                `targetKind` TEXT NOT NULL,
                `targetId` TEXT NOT NULL,
                `entryOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `syncedAt` INTEGER,
                `isDeleted` INTEGER NOT NULL,
                `version` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`capabilityInstanceId`) REFERENCES `workspace_capability_instances`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `day_tasks` (
                `id` TEXT NOT NULL,
                `dayPlanId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `goalId` TEXT,
                `projectId` TEXT,
                `linkedProjectIds` TEXT,
                `linkedAttachmentIds` TEXT,
                `activityRecordId` TEXT,
                `recurrenceSeriesId` TEXT,
                `recurrenceOccurrenceDayKey` TEXT,
                `recurrenceSourceSeriesVersion` INTEGER,
                `taskType` TEXT,
                `entityId` TEXT,
                `order` INTEGER NOT NULL,
                `priority` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `scheduledTime` INTEGER,
                `estimatedDurationMinutes` INTEGER,
                `actualDurationMinutes` INTEGER,
                `dueTime` INTEGER,
                `executionStrictness` TEXT NOT NULL DEFAULT 'NORMAL',
                `valueImportance` REAL NOT NULL DEFAULT 0.0,
                `valueImpact` REAL NOT NULL DEFAULT 0.0,
                `effort` REAL NOT NULL DEFAULT 0.0,
                `cost` REAL NOT NULL DEFAULT 0.0,
                `risk` REAL NOT NULL DEFAULT 0.0,
                `location` TEXT,
                `tags` TEXT,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER,
                `syncedAt` INTEGER,
                `isDeleted` INTEGER NOT NULL,
                `version` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `points` INTEGER NOT NULL DEFAULT 0,
                `project_workspace_id` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`dayPlanId`) REFERENCES `day_plans`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`projectId`) REFERENCES `contexts`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`project_workspace_id`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`activityRecordId`) REFERENCES `activity_records`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `system_apps` (
                `id` TEXT NOT NULL,
                `system_key` TEXT NOT NULL,
                `app_type` TEXT NOT NULL,
                `workspace_id` TEXT NOT NULL,
                `note_document_id` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `version` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workspace_id`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`note_document_id`) REFERENCES `note_documents`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `tactical_missions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `startTime` INTEGER,
                `deadline` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `projectId` TEXT,
                `linkedProjectIds` TEXT,
                `linkedAttachmentIds` TEXT,
                `mission_order` INTEGER NOT NULL DEFAULT 0,
                `week_key` TEXT NOT NULL DEFAULT '',
                `iteration_id` TEXT,
                `carried_from_mission_id` INTEGER,
                `order_in_week` INTEGER NOT NULL DEFAULT 0,
                `order_in_slot` INTEGER,
                `mission_stream_id` TEXT,
                `activity_slot_context_id` TEXT,
                `source_type` TEXT NOT NULL DEFAULT 'MANUAL',
                `source_context_id` TEXT,
                `source_backlog_item_id` TEXT,
                `source_arc_quest_id` TEXT,
                `created_at` INTEGER NOT NULL DEFAULT 0,
                `updated_at` INTEGER,
                `synced_at` INTEGER,
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `version` INTEGER NOT NULL DEFAULT 0,
                `project_workspace_id` TEXT,
                FOREIGN KEY(`projectId`) REFERENCES `contexts`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`project_workspace_id`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `workspace_tag_refs` (
                `workspaceId` TEXT NOT NULL,
                `normalizedTag` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `syncedAt` INTEGER,
                `isDeleted` INTEGER NOT NULL,
                `version` INTEGER NOT NULL,
                PRIMARY KEY(`workspaceId`, `normalizedTag`),
                FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `system_workspace_tag_seed_states` (
                `workspaceId` TEXT NOT NULL,
                `seededAt` INTEGER NOT NULL,
                `legacyIngressClosedAt` INTEGER,
                PRIMARY KEY(`workspaceId`),
                FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }

    fun createSurvivingChildren(db: SupportSQLiteDatabase) {
        createSurvivingChildrenPart1(db)
        createSurvivingChildrenPart2(db)
    }

    fun createSurvivingIndices(db: SupportSQLiteDatabase) {
        survivingIndexDdl.forEach(db::execSQL)
    }

    private val survivingIndexDdl =
        listOf(
            "CREATE INDEX `index_workspaces_updatedAt` ON `workspaces` (`updatedAt`)",
            "CREATE INDEX `index_workspaces_isDeleted` ON `workspaces` (`isDeleted`)",
            "CREATE UNIQUE INDEX `index_workspaces_sourceContextId` ON `workspaces` (`sourceContextId`)",
            "CREATE INDEX `index_main_beacons_readiness_status` ON `main_beacons` (`readiness_status`)",

            "CREATE INDEX `index_main_beacon_group_members_beacon_id` ON `main_beacon_group_members` (`beacon_id`)",
            "CREATE INDEX `index_main_beacon_group_members_group_id_member_order` ON `main_beacon_group_members` (`group_id`, `member_order`)",

            "CREATE INDEX `index_main_beacon_context_cross_ref_context_id` ON `main_beacon_context_cross_ref` (`context_id`)",
            "CREATE INDEX `index_main_beacon_context_cross_ref_beacon_id_ref_order` ON `main_beacon_context_cross_ref` (`beacon_id`, `ref_order`)",

            "CREATE INDEX `index_main_beacon_workspace_cross_ref_workspace_id` ON `main_beacon_workspace_cross_ref` (`workspace_id`)",
            "CREATE INDEX `index_main_beacon_workspace_cross_ref_beacon_id_ref_order` ON `main_beacon_workspace_cross_ref` (`beacon_id`, `ref_order`)",

            "CREATE INDEX `index_main_beacon_attachment_cross_ref_attachment_id` ON `main_beacon_attachment_cross_ref` (`attachment_id`)",

            "CREATE INDEX `index_main_beacon_level_statuses_main_beacon_id` ON `main_beacon_level_statuses` (`main_beacon_id`)",
            "CREATE UNIQUE INDEX `index_main_beacon_level_statuses_main_beacon_id_level_type` ON `main_beacon_level_statuses` (`main_beacon_id`, `level_type`)",

            "CREATE INDEX `index_workspace_connections_workspaceId` ON `workspace_connections` (`workspaceId`)",
            "CREATE INDEX `index_workspace_connections_capabilityInstanceId` ON `workspace_connections` (`capabilityInstanceId`)",
            "CREATE INDEX `index_workspace_connections_attachmentId` ON `workspace_connections` (`attachmentId`)",
            "CREATE INDEX `index_workspace_connections_updatedAt` ON `workspace_connections` (`updatedAt`)",
            "CREATE INDEX `index_workspace_connections_isDeleted` ON `workspace_connections` (`isDeleted`)",
            "CREATE INDEX `index_workspace_connections_capabilityInstanceId_connectionOrder` ON `workspace_connections` (`capabilityInstanceId`, `connectionOrder`)",
            "CREATE UNIQUE INDEX `index_workspace_connections_capabilityInstanceId_attachmentId` ON `workspace_connections` (`capabilityInstanceId`, `attachmentId`)",

            "CREATE INDEX `index_workspace_backlog_entries_workspaceId` ON `workspace_backlog_entries` (`workspaceId`)",
            "CREATE INDEX `index_workspace_backlog_entries_capabilityInstanceId` ON `workspace_backlog_entries` (`capabilityInstanceId`)",
            "CREATE INDEX `index_workspace_backlog_entries_targetKind_targetId` ON `workspace_backlog_entries` (`targetKind`, `targetId`)",
            "CREATE INDEX `index_workspace_backlog_entries_updatedAt` ON `workspace_backlog_entries` (`updatedAt`)",
            "CREATE INDEX `index_workspace_backlog_entries_isDeleted` ON `workspace_backlog_entries` (`isDeleted`)",
            "CREATE INDEX `index_workspace_backlog_entries_capabilityInstanceId_entryOrder` ON `workspace_backlog_entries` (`capabilityInstanceId`, `entryOrder`)",
            "CREATE INDEX `index_workspace_backlog_entries_capabilityInstanceId_targetKind_targetId` ON `workspace_backlog_entries` (`capabilityInstanceId`, `targetKind`, `targetId`)",

            "CREATE INDEX `index_day_tasks_dayPlanId` ON `day_tasks` (`dayPlanId`)",
            "CREATE INDEX `index_day_tasks_goalId` ON `day_tasks` (`goalId`)",
            "CREATE INDEX `index_day_tasks_projectId` ON `day_tasks` (`projectId`)",
            "CREATE INDEX `index_day_tasks_project_workspace_id` ON `day_tasks` (`project_workspace_id`)",
            "CREATE INDEX `index_day_tasks_activityRecordId` ON `day_tasks` (`activityRecordId`)",
            "CREATE INDEX `index_day_tasks_scheduledTime` ON `day_tasks` (`scheduledTime`)",
            "CREATE INDEX `index_day_tasks_recurrenceSeriesId_recurrenceOccurrenceDayKey` ON `day_tasks` (`recurrenceSeriesId`, `recurrenceOccurrenceDayKey`)",

            "CREATE UNIQUE INDEX `index_system_apps_system_key` ON `system_apps` (`system_key`)",
            "CREATE INDEX `index_system_apps_workspace_id` ON `system_apps` (`workspace_id`)",
            "CREATE INDEX `index_system_apps_note_document_id` ON `system_apps` (`note_document_id`)",

            "CREATE INDEX `index_tactical_missions_projectId` ON `tactical_missions` (`projectId`)",
            "CREATE INDEX `index_tactical_missions_project_workspace_id` ON `tactical_missions` (`project_workspace_id`)",
            "CREATE INDEX `index_tactical_missions_week_key` ON `tactical_missions` (`week_key`)",
            "CREATE INDEX `index_tactical_missions_iteration_id` ON `tactical_missions` (`iteration_id`)",
            "CREATE INDEX `index_tactical_missions_mission_stream_id` ON `tactical_missions` (`mission_stream_id`)",
            "CREATE INDEX `index_tactical_missions_activity_slot_context_id` ON `tactical_missions` (`activity_slot_context_id`)",
            "CREATE INDEX `index_tactical_missions_source_backlog_item_id_week_key` ON `tactical_missions` (`source_backlog_item_id`, `week_key`)",

            "CREATE INDEX `index_workspace_tag_refs_normalizedTag` ON `workspace_tag_refs` (`normalizedTag`)",
            "CREATE INDEX `index_workspace_tag_refs_updatedAt` ON `workspace_tag_refs` (`updatedAt`)",
            "CREATE INDEX `index_workspace_tag_refs_isDeleted` ON `workspace_tag_refs` (`isDeleted`)",
        )

    fun restoreSurvivingChildren(db: SupportSQLiteDatabase) {
        survivingChildren.forEach { child ->
            db.execSQL("INSERT INTO `${child.name}` SELECT * FROM `${child.backup}`")
            assertCountAndKeysPreserved(
                db = db,
                source = child.backup,
                target = child.name,
                keys = child.keys,
            )
            db.execSQL("DROP TABLE `${child.backup}`")
        }
    }

    fun requirePhysicalRetirementShape(db: SupportSQLiteDatabase) {
        require(!tableExists(db, "context_parent_links"))
        require(!tableExists(db, "main_beacon_parent_links"))

        require(!columnExists(db, WORKSPACES, "parentWorkspaceId"))
        require(!columnExists(db, WORKSPACES, "workspaceOrder"))
        require(!columnExists(db, MAIN_BEACONS, "parent_beacon_id"))
        require(columnExists(db, MAIN_BEACONS, "beacon_order"))
    }

    fun requireForeignKeyCheckClean(db: SupportSQLiteDatabase) {
        db.query("PRAGMA foreign_key_check").use { cursor ->
            require(!cursor.moveToFirst()) {
                "Physical hierarchy retirement failed PRAGMA foreign_key_check"
            }
        }
    }

    fun requireIntegrityCheckOk(db: SupportSQLiteDatabase) {
        val result =
            db.query("PRAGMA integrity_check").use { cursor ->
                require(cursor.moveToFirst())
                cursor.getString(0)
            }
        require(result.equals("ok", ignoreCase = true)) {
            "Physical hierarchy retirement integrity_check failed: $result"
        }
    }

    fun count(
        db: SupportSQLiteDatabase,
        table: String,
    ): Long =
        db.query("SELECT COUNT(*) FROM `${quoteIdentifier(table)}`").use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun inboundForeignKeyTables(
        db: SupportSQLiteDatabase,
        parentTable: String,
    ): Set<String> {
        val tables =
            db.query(
                """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name NOT LIKE 'sqlite_%'
                ORDER BY name
                """.trimIndent(),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }

        return buildSet {
            tables.forEach { table ->
                db.query(
                    "PRAGMA foreign_key_list(`${quoteIdentifier(table)}`)",
                ).use { cursor ->
                    val targetIndex = cursor.getColumnIndexOrThrow("table")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(targetIndex) == parentTable) add(table)
                    }
                }
            }
        }
    }

    private fun assertCountAndKeysPreserved(
        db: SupportSQLiteDatabase,
        source: String,
        target: String,
        keys: List<String>,
    ) {
        require(count(db, source) == count(db, target)) {
            "Row-count mismatch while rebuilding $source"
        }

        val projection =
            keys.joinToString(", ") { "`${quoteIdentifier(it)}`" }

        require(
            !db.query(
                """
                SELECT $projection FROM `${quoteIdentifier(source)}`
                EXCEPT
                SELECT $projection FROM `${quoteIdentifier(target)}`
                LIMIT 1
                """.trimIndent(),
            ).use { it.moveToFirst() },
        ) {
            "Key loss while rebuilding $source"
        }

        require(
            !db.query(
                """
                SELECT $projection FROM `${quoteIdentifier(target)}`
                EXCEPT
                SELECT $projection FROM `${quoteIdentifier(source)}`
                LIMIT 1
                """.trimIndent(),
            ).use { it.moveToFirst() },
        ) {
            "Unexpected keys while rebuilding $source"
        }
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        db.query(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
            arrayOf<Any?>(table),
        ).use { it.moveToFirst() }

    private fun columnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean =
        db.query("PRAGMA table_info(`${quoteIdentifier(table)}`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return@use true
            }
            false
        }

    private fun quoteIdentifier(value: String): String =
        value.replace("`", "``")
}
