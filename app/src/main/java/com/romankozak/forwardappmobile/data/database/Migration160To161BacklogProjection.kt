package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * BACKLOG Stage 3 projection-storage foundation.
 *
 * Adds a rebuildable local cache for hashtag-routed Goal appearances.
 *
 * Projection identity is deterministic so deleting and rebuilding the cache
 * cannot invalidate external references such as tactical mission sources.
 *
 * Context-backed explicit Backlog authority deliberately remains list_items.
 * Legacy association rows are retained as migration/accounting evidence.
 */
val MIGRATION_160_161 =
    object : Migration(160, 161) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS backlog_goal_association_links (
                    projection_id TEXT NOT NULL,
                    goal_id TEXT NOT NULL,
                    context_id TEXT NOT NULL,
                    owner_context_id TEXT NOT NULL,
                    association_tag TEXT,
                    item_order INTEGER NOT NULL,
                    linked_at INTEGER NOT NULL,
                    PRIMARY KEY(goal_id, context_id),
                    FOREIGN KEY(goal_id) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(context_id) REFERENCES contexts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            db.execSQL(
                "CREATE INDEX index_backlog_goal_association_links_context_id " +
                    "ON backlog_goal_association_links(context_id)",
            )
            db.execSQL(
                "CREATE INDEX index_backlog_goal_association_links_goal_id " +
                    "ON backlog_goal_association_links(goal_id)",
            )
            db.execSQL(
                "CREATE INDEX index_backlog_goal_association_links_owner_context_id_goal_id " +
                    "ON backlog_goal_association_links(owner_context_id, goal_id)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX index_backlog_goal_association_links_projection_id " +
                    "ON backlog_goal_association_links(projection_id)",
            )

            /*
             * Tactical missions persisted the old list_items association id.
             * Rewrite only references that provably point at hashtag-derived
             * GOAL appearances. Explicit placement references stay untouched.
             */
            db.execSQL(
                """
                UPDATE tactical_missions
                SET source_backlog_item_id = (
                    SELECT
                        'goal_association:' || li.entityId || ':' || li.context_id
                    FROM list_items li
                    WHERE li.id = tactical_missions.source_backlog_item_id
                      AND li.itemType = 'GOAL'
                      AND li.association_owner_context_id IS NOT NULL
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1
                    FROM list_items li
                    WHERE li.id = tactical_missions.source_backlog_item_id
                      AND li.itemType = 'GOAL'
                      AND li.association_owner_context_id IS NOT NULL
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO backlog_goal_association_links(
                    projection_id,
                    goal_id,
                    context_id,
                    owner_context_id,
                    association_tag,
                    item_order,
                    linked_at
                )
                SELECT
                    'goal_association:' || li.entityId || ':' || li.context_id,
                    li.entityId,
                    li.context_id,
                    li.association_owner_context_id,
                    li.association_tag,
                    li.item_order,
                    COALESCE(li.updatedAt, 0)
                FROM list_items li
                JOIN goals g ON g.id = li.entityId
                JOIN contexts c ON c.id = li.context_id
                WHERE li.itemType = 'GOAL'
                  AND li.association_owner_context_id IS NOT NULL
                  AND li.is_deleted = 0
                  AND g.is_deleted = 0
                  AND c.is_deleted = 0
                """.trimIndent(),
            )
        }
    }
