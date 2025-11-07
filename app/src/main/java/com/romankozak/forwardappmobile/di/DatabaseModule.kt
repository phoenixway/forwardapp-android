package com.romankozak.forwardappmobile.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.AttachmentQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ChecklistQueries
import com.romankozak.forwardappmobile.shared.database.DayPlanQueries
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.LegacyNoteQueriesQueries
import com.romankozak.forwardappmobile.shared.database.LinkItemQueries
import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import com.romankozak.forwardappmobile.shared.database.NoteDocumentQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ProjectQueries
import com.romankozak.forwardappmobile.shared.database.ProjectArtifactQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogQueriesQueries
import com.romankozak.forwardappmobile.shared.database.RecentItemQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ReminderQueriesQueries
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideForwardAppDatabase(@ApplicationContext context: Context): ForwardAppDatabase {
        val driver = AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = context,
            name = "forward_app.db",
            callback = object : AndroidSqliteDriver.Callback(ForwardAppDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
                override fun onCorruption(db: SupportSQLiteDatabase) {
                    super.onCorruption(db)
                    // TODO: add logging and crash reporting
                    // TODO: show user a message that the database is corrupted and needs to be reinstalled
                    context.deleteDatabase("forward_app.db")
                }
            }
        )
        return ForwardAppDatabase(driver)
    }

    @Provides
    fun provideAttachmentQueries(db: ForwardAppDatabase): AttachmentQueriesQueries = db.attachmentQueriesQueries

    @Provides
    fun provideChecklistQueries(db: ForwardAppDatabase): ChecklistQueries = db.checklistQueries

    @Provides
    fun provideDayPlanQueries(db: ForwardAppDatabase): DayPlanQueries = db.dayPlanQueries

    @Provides
    fun provideLegacyNoteQueries(db: ForwardAppDatabase): LegacyNoteQueriesQueries = db.legacyNoteQueriesQueries

    @Provides
    fun provideLinkItemQueries(db: ForwardAppDatabase): LinkItemQueries = db.linkItemQueries

    @Provides
    fun provideListItemQueries(db: ForwardAppDatabase): ListItemQueries = db.listItemQueries

    @Provides
    fun provideNoteDocumentQueries(db: ForwardAppDatabase): NoteDocumentQueriesQueries = db.noteDocumentQueriesQueries

    @Provides
    fun provideProjectQueries(db: ForwardAppDatabase): ProjectQueries = db.projectQueries

    @Provides
    fun provideProjectArtifactQueries(db: ForwardAppDatabase): ProjectArtifactQueriesQueries = db.projectArtifactQueriesQueries

    @Provides
    fun provideProjectExecutionLogQueries(db: ForwardAppDatabase): ProjectExecutionLogQueriesQueries = db.projectExecutionLogQueriesQueries

    @Provides
    fun provideRecentItemQueries(db: ForwardAppDatabase): RecentItemQueriesQueries = db.recentItemQueriesQueries

    @Provides
    fun provideReminderQueries(db: ForwardAppDatabase): ReminderQueriesQueries = db.reminderQueriesQueries
}
