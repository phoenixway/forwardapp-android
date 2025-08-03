package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.database.models.Converters
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao

@Database(
    entities = [Goal::class, GoalList::class, GoalInstance::class],
    version = 11, // Версія оновлена, це правильно
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10)
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
                    // ✅ Цей рядок виправляє помилку, реєструючи обидві міграції
                    .addMigrations(MIGRATION_8_9, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}