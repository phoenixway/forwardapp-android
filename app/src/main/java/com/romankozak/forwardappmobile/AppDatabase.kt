// AppDatabase.kt
package com.romankozak.forwardappmobile

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [Goal::class, GoalList::class, GoalInstance::class],
    version = 7, // ЗБІЛЬШЕНО: Версію оновлено до 7
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        // --- ДОДАНО: Правило для нової міграції з перейменуванням колонок ---
        AutoMigration(
            from = 6,
            to = 7,
            spec = AppDatabase.Migration6To7::class
        )
    ],
    exportSchema = true

)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // --- ДОДАНО: Специфікація для міграції ---
    @RenameColumn(tableName = "goal_instances", fromColumnName = "id", toColumnName = "instance_id")
    @RenameColumn(tableName = "goal_instances", fromColumnName = "orderIndex", toColumnName = "goal_order")
    class Migration6To7 : AutoMigrationSpec

    abstract fun goalListDao(): GoalListDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "forward_app_database"
                )
                    // Важливо: видаліть .fallbackToDestructiveMigration() для робочої версії,
                    // але для розробки це може бути корисно, якщо міграція не спрацює.
                    // .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}