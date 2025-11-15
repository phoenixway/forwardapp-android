package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.romankozak.forwardappmobile.shared.database.shared.newInstance
import com.romankozak.forwardappmobile.shared.database.shared.schema
import com.romankozak.forwardappmobile.shared.features.activitytracker.ActivityRecords
import com.romankozak.forwardappmobile.shared.features.activitytracker.ActivityRecordsQueries
import com.romankozak.forwardappmobile.shared.features.aichat.ChatMessages
import com.romankozak.forwardappmobile.shared.features.aichat.ChatMessagesQueries
import com.romankozak.forwardappmobile.shared.features.aichat.ConversationFolders
import com.romankozak.forwardappmobile.shared.features.aichat.ConversationFoldersQueries
import com.romankozak.forwardappmobile.shared.features.aichat.Conversations
import com.romankozak.forwardappmobile.shared.features.aichat.ConversationsQueries
import com.romankozak.forwardappmobile.shared.features.attachments.AttachmentsQueries
import com.romankozak.forwardappmobile.shared.features.attachments.ProjectAttachmentCrossRefQueries
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.LinkItems
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.LinkItemsQueries
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.ChecklistItems
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.ChecklistItemsQueries
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.ChecklistsQueries
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.LegacyNotes
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.LegacyNotesQueries
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocumentItems
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocumentItemsQueries
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocuments
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.NoteDocumentsQueries
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.DailyMetrics
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.DailyMetricsQueries
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayPlans
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayPlansQueries
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayTasksQueries
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.RecurringTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.RecurringTasksQueries
import com.romankozak.forwardappmobile.shared.features.projects.logs.ProjectExecutionLogsQueries
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.ProjectArtifacts
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.ProjectArtifactsQueries
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.InboxRecordsQueries
import com.romankozak.forwardappmobile.shared.features.recent.RecentItemsQueries
import com.romankozak.forwardappmobile.shared.features.reminders.Reminders
import com.romankozak.forwardappmobile.shared.features.reminders.RemindersQueries
import kotlin.Unit

public interface ForwardAppDatabase : Transacter {
  public val activityRecordsQueries: ActivityRecordsQueries

  public val attachmentsQueries: AttachmentsQueries

  public val chatMessagesQueries: ChatMessagesQueries

  public val checklistItemsQueries: ChecklistItemsQueries

  public val checklistsQueries: ChecklistsQueries

  public val conversationFoldersQueries: ConversationFoldersQueries

  public val conversationsQueries: ConversationsQueries

  public val dailyMetricsQueries: DailyMetricsQueries

  public val dayPlansQueries: DayPlansQueries

  public val dayTasksQueries: DayTasksQueries

  public val goalsQueries: GoalsQueries

  public val inboxRecordsQueries: InboxRecordsQueries

  public val legacyNotesQueries: LegacyNotesQueries

  public val linkItemsQueries: LinkItemsQueries

  public val listItemsQueries: ListItemsQueries

  public val noteDocumentItemsQueries: NoteDocumentItemsQueries

  public val noteDocumentsQueries: NoteDocumentsQueries

  public val notesQueries: NotesQueries

  public val projectArtifactsQueries: ProjectArtifactsQueries

  public val projectAttachmentCrossRefQueries: ProjectAttachmentCrossRefQueries

  public val projectExecutionLogsQueries: ProjectExecutionLogsQueries

  public val projectsQueries: ProjectsQueries

  public val recentItemsQueries: RecentItemsQueries

  public val recurringTasksQueries: RecurringTasksQueries

  public val remindersQueries: RemindersQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = ForwardAppDatabase::class.schema

    public operator fun invoke(
      driver: SqlDriver,
      ActivityRecordsAdapter: ActivityRecords.Adapter,
      ChatMessagesAdapter: ChatMessages.Adapter,
      ChecklistItemsAdapter: ChecklistItems.Adapter,
      ConversationFoldersAdapter: ConversationFolders.Adapter,
      ConversationsAdapter: Conversations.Adapter,
      DailyMetricsAdapter: DailyMetrics.Adapter,
      DayPlansAdapter: DayPlans.Adapter,
      DayTasksAdapter: DayTasks.Adapter,
      GoalsAdapter: Goals.Adapter,
      LegacyNotesAdapter: LegacyNotes.Adapter,
      LinkItemsAdapter: LinkItems.Adapter,
      ListItemsAdapter: ListItems.Adapter,
      NoteDocumentItemsAdapter: NoteDocumentItems.Adapter,
      NoteDocumentsAdapter: NoteDocuments.Adapter,
      NotesAdapter: Notes.Adapter,
      ProjectArtifactsAdapter: ProjectArtifacts.Adapter,
      ProjectsAdapter: Projects.Adapter,
      RecurringTasksAdapter: RecurringTasks.Adapter,
      RemindersAdapter: Reminders.Adapter,
    ): ForwardAppDatabase = ForwardAppDatabase::class.newInstance(driver, ActivityRecordsAdapter,
        ChatMessagesAdapter, ChecklistItemsAdapter, ConversationFoldersAdapter,
        ConversationsAdapter, DailyMetricsAdapter, DayPlansAdapter, DayTasksAdapter, GoalsAdapter,
        LegacyNotesAdapter, LinkItemsAdapter, ListItemsAdapter, NoteDocumentItemsAdapter,
        NoteDocumentsAdapter, NotesAdapter, ProjectArtifactsAdapter, ProjectsAdapter,
        RecurringTasksAdapter, RemindersAdapter)
  }
}
