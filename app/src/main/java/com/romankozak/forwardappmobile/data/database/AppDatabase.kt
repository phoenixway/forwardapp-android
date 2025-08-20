// File: app/src/main/java/com/romankozak/forwardappmobile/data/database/AppDatabase.kt

package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.dao.RecentListDao
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.Converters
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.RecentListEntry

@Database(
    entities = [Goal::class, GoalList::class, GoalInstance::class, ActivityRecord::class, RecentListEntry::class],
    version = 14, // ✨ 1. ЗБІЛЬШЕНО ВЕРСІЮ ДО 14
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
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun recentListDao(): RecentListDao // Тепер цей DAO розпізнається

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
                    // ✨ 2. ДОДАНО НОВУ МІГРАЦІЮ 13 -> 14
                    .addMigrations(
                        MIGRATION_8_9,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14 // Додано міграцію для нової таблиці
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}