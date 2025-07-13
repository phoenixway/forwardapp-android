// AppDatabase.kt
package com.romankozak.forwardappmobile

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Goal::class, GoalList::class, GoalInstance::class],
    version = 5, // ЗБІЛЬШЕНО: Версію оновлено до 5
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5) // ДОДАНО: Правило для нової міграції
    ],
    exportSchema = true

)
@TypeConverters(Converters::class) // ДОДАНО: Реєструємо конвертер на рівні БД
abstract class AppDatabase : RoomDatabase() {
    // ... решта коду без змін
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}