package com.romankozak.forwardappmobile.data.database

import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.database.AppDatabase

/**
 * Test-only historical storage seeding.
 *
 * These helpers deliberately bypass production DAOs so tests can characterize
 * marker-gated V1 capture without retaining runtime legacy hierarchy writers.
 */
internal fun AppDatabase.seedHistoricalContextParentLinks(
    links: List<ContextParentLink>,
) {
    val sqlite = openHelper.writableDatabase
    sqlite.beginTransaction()
    try {
        links.forEach { link ->
            sqlite.execSQL(
                """
                INSERT OR REPLACE INTO context_parent_links (
                    parent_context_id,
                    child_context_id,
                    link_order,
                    createdAt,
                    updatedAt,
                    synced_at,
                    is_deleted,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    link.parentContextId,
                    link.childContextId,
                    link.order,
                    link.createdAt,
                    link.updatedAt,
                    link.syncedAt,
                    if (link.isDeleted) 1 else 0,
                    link.version,
                ),
            )
        }
        sqlite.setTransactionSuccessful()
    } finally {
        sqlite.endTransaction()
    }
}

internal fun AppDatabase.readHistoricalContextParentLinks(): List<ContextParentLink> =
    openHelper.readableDatabase
        .query(
            """
            SELECT
                parent_context_id,
                child_context_id,
                link_order,
                createdAt,
                updatedAt,
                synced_at,
                is_deleted,
                version
            FROM context_parent_links
            ORDER BY parent_context_id, child_context_id
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ContextParentLink(
                            parentContextId = cursor.getString(0),
                            childContextId = cursor.getString(1),
                            order = cursor.getLong(2),
                            createdAt = cursor.getLong(3),
                            updatedAt = cursor.getLongOrNull(4),
                            syncedAt = cursor.getLongOrNull(5),
                            isDeleted = cursor.getInt(6) != 0,
                            version = cursor.getLong(7),
                        ),
                    )
                }
            }
        }

internal fun AppDatabase.seedHistoricalMainBeaconParentLinks(
    links: List<MainBeaconParentLink>,
) {
    val sqlite = openHelper.writableDatabase
    sqlite.beginTransaction()
    try {
        links.forEach { link ->
            sqlite.execSQL(
                """
                INSERT OR REPLACE INTO main_beacon_parent_links (
                    parent_beacon_id,
                    child_beacon_id,
                    link_order,
                    updatedAt,
                    createdAt
                ) VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    link.parentBeaconId,
                    link.childBeaconId,
                    link.order,
                    link.updatedAt,
                    link.createdAt,
                ),
            )
        }
        sqlite.setTransactionSuccessful()
    } finally {
        sqlite.endTransaction()
    }
}

private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? =
    if (isNull(columnIndex)) null else getLong(columnIndex)
