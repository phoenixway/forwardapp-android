// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/database/AppDatabase.kt ---
package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.*

@Database(
    entities = [
        Goal::class,
        GoalList::class,
        // Note::class, // <-- ВИДАЛЕНО
        ListItem::class,
        ActivityRecord::class,
        ActivityRecordFts::class,
        RecentListEntry::class,
        LinkItemEntity::class,
        InboxRecord::class,
        ChatMessageEntity::class,
    ],
    version = 25, // <-- ЗМІНЕНО
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
    // abstract fun noteDao(): NoteDao // <-- ВИДАЛЕНО
    abstract fun listItemDao(): ListItemDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun recentListDao(): RecentListDao
    abstract fun linkItemDao(): LinkItemDao
    abstract fun inboxRecordDao(): InboxRecordDao
    abstract fun chatDao(): ChatDao


    /* companion object {
         @Volatile
         private var INSTANCE: AppDatabase? = null

         fun getDatabase(context: Context): AppDatabase {
             return INSTANCE ?: synchronized(this) {
                 val instance = Room.databaseBuilder(
                     context.applicationContext,
                     AppDatabase::class.java,
                     "forward_app_database"
                 )
                     .addMigrations(
                         MIGRATION_8_9,
                         MIGRATION_10_11,
                         MIGRATION_11_12,
                         MIGRATION_12_13,
                         MIGRATION_13_14,
                         MIGRATION_14_15,
                         MIGRATION_15_16,
                         MIGRATION_16_17,
                         MIGRATION_17_18,
                         MIGRATION_18_19,
                         MIGRATION_19_20,
                         MIGRATION_20_21,
                         MIGRATION_21_22,
                         MIGRATION_22_23,
                         MIGRATION_23_24 // <-- ДОДАНО
                     )
                     .build()
                 INSTANCE = instance
                 instance
             }
         }
     }*/
}