package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Extends canonical Orientation assessment persistence with evaluation inputs.
 *
 * Existing canonical rows deliberately start with UNSET scoring axes and an
 * UNASSESSED evaluation state. Legacy Goal/Context scoring values remain
 * compatibility-owned until the later authority-migration slice.
 */
val MIGRATION_185_186 =
    object : Migration(185, 186) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `effortValue` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `effortOrigin` TEXT NOT NULL DEFAULT 'UNSET'",
            )
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `costValue` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `costOrigin` TEXT NOT NULL DEFAULT 'UNSET'",
            )
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `riskValue` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `riskOrigin` TEXT NOT NULL DEFAULT 'UNSET'",
            )
            db.execSQL(
                "ALTER TABLE `orientation_assessments` ADD COLUMN `evaluationState` TEXT NOT NULL DEFAULT 'UNASSESSED'",
            )
        }
    }
