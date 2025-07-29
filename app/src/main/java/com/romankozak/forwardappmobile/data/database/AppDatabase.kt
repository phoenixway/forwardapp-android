package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.romankozak.forwardappmobile.data.database.models.Converters
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao

@Database(
    entities = [Goal::class, GoalList::class, GoalInstance::class],
    version = 9, // ЗБІЛЬШЕНО: Версію оновлено до 9
    autoMigrations = [
        // Попередні авто-міграції залишаються
        AutoMigration(from = 7, to = 8)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

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
                    // ДОДАНО: Підключаємо нашу ручну міграцію
                    .addMigrations(MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
