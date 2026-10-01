package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyMigrationEstablisher
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyMigrationPersistenceAdapter

/**
 * H6.E6k-L3d physical retirement of obsolete hierarchy storage.
 *
 * Ordering is intentional:
 * 1. establish/converge canonical H1 from schema-179 evidence;
 * 2. prove marker v3 + ESTABLISHED + canonical H1 invariants;
 * 3. snapshot and rebuild every surviving FK dependent;
 * 4. destroy obsolete physical hierarchy fields/tables;
 * 5. restore dependents and indexes;
 * 6. prove physical shape, FKs, SQLite integrity and canonical H1 again.
 */
val MIGRATION_179_180 =
    object : Migration(179, 180) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val retirement = HierarchyPhysicalRetirement179To180

            CanonicalHierarchyMigrationEstablisher().establish(
                db = db,
                now = now,
            )

            val canonicalGate = CanonicalHierarchyMigrationPersistenceAdapter()
            canonicalGate.validateCurrentState(db)

            // Room owns the surrounding migration transaction and may execute
            // migrations with SQLite FK enforcement temporarily disabled.
            // Validate the FK graph before rebuilding, then prove actual
            // referential integrity with foreign_key_check afterwards.
            retirement.requireExpectedInboundForeignKeys(db)

            val originalCounts =
                retirement.retainedTables.associateWith { table ->
                    retirement.count(db, table)
                }

            retirement.createReplacementParents(db)
            retirement.copyReplacementParents(db)
            retirement.assertReplacementParentsPreserved(db)

            retirement.backupSurvivingChildren(db)
            retirement.dropOldChildrenAndRetiredLinks(db)
            retirement.replaceParentTables(db)

            retirement.createSurvivingChildren(db)
            retirement.restoreSurvivingChildren(db)
            retirement.createSurvivingIndices(db)

            originalCounts.forEach { (table, expectedCount) ->
                require(retirement.count(db, table) == expectedCount) {
                    "Physical hierarchy retirement changed row count for $table"
                }
            }

            retirement.requirePhysicalRetirementShape(db)
            retirement.requireForeignKeyCheckClean(db)
            retirement.requireIntegrityCheckOk(db)

            canonicalGate.validateCurrentState(db)
        }
    }
