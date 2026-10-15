package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Physical MainBeacon Context-FK retirement.
 *
 * Schema 194 may contain ordinary historical Context refs which were not
 * covered by the reserved-System migration at schema 170.
 *
 * Validate every legacy association against the accepted Workspace owner
 * policy before writing or removing any persistence.
 *
 * Room owns the upgrade transaction. Any rejection must roll back the
 * complete migration, leaving the old database available for recovery.
 */
val MIGRATION_194_195 =
    object : Migration(194, 195) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val missingCanonicalRefs =
                db.query(
                    """
                    SELECT
                        legacy.beacon_id,
                        legacy.context_id,
                        legacy.ref_order,
                        historical.id,
                        historical.is_deleted,
                        owner.id,
                        owner.isDeleted,
                        owner.provenance,
                        owner.sourceContextId,
                        canonical.ref_order
                    FROM main_beacon_context_cross_ref AS legacy
                    LEFT JOIN contexts AS historical
                        ON historical.id = legacy.context_id
                    LEFT JOIN workspaces AS owner
                        ON owner.id = legacy.context_id
                    LEFT JOIN main_beacon_workspace_cross_ref AS canonical
                        ON canonical.beacon_id = legacy.beacon_id
                        AND canonical.workspace_id = legacy.context_id
                    ORDER BY
                        legacy.beacon_id,
                        legacy.context_id
                    """.trimIndent(),
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            val beaconId = cursor.getString(0)
                            val ownerId = cursor.getString(1)
                            val order = cursor.getLong(2)
                            val label = "MainBeacon $beaconId owner $ownerId"

                            check(beaconId.isNotBlank() && ownerId.isNotBlank()) {
                                "$label has a blank association identity"
                            }

                            // A physical Context FK must have historical
                            // Context evidence. Never repair broken FKs by
                            // guessing Workspace ownership.
                            check(!cursor.isNull(3)) {
                                "$label has no historical Context"
                            }

                            check(!cursor.isNull(5)) {
                                "$label has no Workspace"
                            }

                            val contextDeleted = cursor.getInt(4) != 0
                            val workspaceDeleted = cursor.getInt(6) != 0
                            val provenance = cursor.getString(7)
                            val sourceContextId =
                                if (cursor.isNull(8)) null else cursor.getString(8)

                            check(!workspaceDeleted) {
                                "$label has a deleted Workspace"
                            }

                            val canonicalOnly =
                                provenance == "CANONICAL_ONLY" &&
                                    sourceContextId == null

                            val contextBacked =
                                provenance == "CONTEXT_BACKED" &&
                                    sourceContextId == ownerId

                            val validOwner =
                                when {
                                    ownerId in FROZEN_SYSTEM_MAINBEACON_194 ->
                                        canonicalOnly
                                    contextDeleted ->
                                        canonicalOnly
                                    else ->
                                        contextBacked
                                }

                            check(validOwner) {
                                "$label has invalid Workspace provenance"
                            }

                            if (cursor.isNull(9)) {
                                add(Triple(beaconId, ownerId, order))
                            } else {
                                val existingOrder = cursor.getLong(9)
                                check(existingOrder == order) {
                                    "$label has divergent canonical order: " +
                                        "$existingOrder != $order"
                                }
                            }
                        }
                    }
                }

            // Every legacy row is now proven safe. Do not use REPLACE:
            // existing canonical rows and their ordering are authoritative.
            missingCanonicalRefs.forEach { (beaconId, ownerId, order) ->
                db.execSQL(
                    """
                    INSERT INTO main_beacon_workspace_cross_ref
                        (beacon_id, workspace_id, ref_order)
                    VALUES (?, ?, ?)
                    """.trimIndent(),
                    arrayOf(beaconId, ownerId, order),
                )
            }

            db.execSQL("DROP TABLE `main_beacon_context_cross_ref`")
        }
    }

/**
 * Frozen schema-194 system identity boundary.
 *
 * Do not substitute the evolving SystemContexts registry into an immutable
 * historical migration.
 */
private val FROZEN_SYSTEM_MAINBEACON_194 =
    setOf(
        "sys_personal-management",
        "sys_mode-about",
        "sys_mode-improve",
        "sys_mode-execution",
        "sys_mode-control",
        "sys_mode-recovery",
        "sys_mode-emergency",
        "sys_strategic",
        "sys_mission",
        "sys_long-term-strategy",
        "sys_strategic-programs",
        "sys_medium-term-strategy",
        "sys_active-quests",
        "sys_levels",
        "sys_week",
        "sys_inbox",
        "sys_strategic-inbox",
        "sys_strategic-review",
        "sys_main-beacons",
        "sys_today",
    )
