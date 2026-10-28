package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_205_206 = object : Migration(205, 206) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS planning_commitment_relations (
            id TEXT NOT NULL PRIMARY KEY, sourceCommitmentId TEXT NOT NULL, targetCommitmentId TEXT NOT NULL,
            relationType TEXT NOT NULL, relationOrder INTEGER NOT NULL, createdAt INTEGER NOT NULL,
            updatedAt INTEGER, syncedAt INTEGER, isDeleted INTEGER NOT NULL, version INTEGER NOT NULL,
            FOREIGN KEY(sourceCommitmentId) REFERENCES planning_commitments(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(targetCommitmentId) REFERENCES planning_commitments(id) ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX index_planning_commitment_relations_sourceCommitmentId ON planning_commitment_relations(sourceCommitmentId)")
        db.execSQL("CREATE INDEX index_planning_commitment_relations_targetCommitmentId_relationType_isDeleted_relationOrder_id ON planning_commitment_relations(targetCommitmentId,relationType,isDeleted,relationOrder,id)")
        db.execSQL("CREATE INDEX index_planning_commitment_relations_updatedAt ON planning_commitment_relations(updatedAt)")
        db.execSQL("CREATE INDEX index_planning_commitment_relations_syncedAt ON planning_commitment_relations(syncedAt)")
    }
}
