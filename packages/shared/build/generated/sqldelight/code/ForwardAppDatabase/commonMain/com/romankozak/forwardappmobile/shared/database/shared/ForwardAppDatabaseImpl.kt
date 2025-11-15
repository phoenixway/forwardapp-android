package com.romankozak.forwardappmobile.shared.database.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.database.GoalsQueries
import com.romankozak.forwardappmobile.shared.database.ListItems
import com.romankozak.forwardappmobile.shared.database.ListItemsQueries
import com.romankozak.forwardappmobile.shared.database.Notes
import com.romankozak.forwardappmobile.shared.database.NotesQueries
import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.database.ProjectsQueries
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
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<ForwardAppDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = ForwardAppDatabaseImpl.Schema

internal fun KClass<ForwardAppDatabase>.newInstance(
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
): ForwardAppDatabase = ForwardAppDatabaseImpl(driver, ActivityRecordsAdapter, ChatMessagesAdapter,
    ChecklistItemsAdapter, ConversationFoldersAdapter, ConversationsAdapter, DailyMetricsAdapter,
    DayPlansAdapter, DayTasksAdapter, GoalsAdapter, LegacyNotesAdapter, LinkItemsAdapter,
    ListItemsAdapter, NoteDocumentItemsAdapter, NoteDocumentsAdapter, NotesAdapter,
    ProjectArtifactsAdapter, ProjectsAdapter, RecurringTasksAdapter, RemindersAdapter)

private class ForwardAppDatabaseImpl(
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
) : TransacterImpl(driver), ForwardAppDatabase {
  override val activityRecordsQueries: ActivityRecordsQueries = ActivityRecordsQueries(driver,
      ActivityRecordsAdapter)

  override val attachmentsQueries: AttachmentsQueries = AttachmentsQueries(driver)

  override val chatMessagesQueries: ChatMessagesQueries = ChatMessagesQueries(driver,
      ChatMessagesAdapter)

  override val checklistItemsQueries: ChecklistItemsQueries = ChecklistItemsQueries(driver,
      ChecklistItemsAdapter)

  override val checklistsQueries: ChecklistsQueries = ChecklistsQueries(driver)

  override val conversationFoldersQueries: ConversationFoldersQueries =
      ConversationFoldersQueries(driver, ConversationFoldersAdapter)

  override val conversationsQueries: ConversationsQueries = ConversationsQueries(driver,
      ConversationsAdapter, ChatMessagesAdapter)

  override val dailyMetricsQueries: DailyMetricsQueries = DailyMetricsQueries(driver,
      DailyMetricsAdapter)

  override val dayPlansQueries: DayPlansQueries = DayPlansQueries(driver, DayPlansAdapter)

  override val dayTasksQueries: DayTasksQueries = DayTasksQueries(driver, DayTasksAdapter)

  override val goalsQueries: GoalsQueries = GoalsQueries(driver, GoalsAdapter)

  override val inboxRecordsQueries: InboxRecordsQueries = InboxRecordsQueries(driver)

  override val legacyNotesQueries: LegacyNotesQueries = LegacyNotesQueries(driver,
      LegacyNotesAdapter)

  override val linkItemsQueries: LinkItemsQueries = LinkItemsQueries(driver, LinkItemsAdapter,
      ListItemsAdapter)

  override val listItemsQueries: ListItemsQueries = ListItemsQueries(driver, ListItemsAdapter)

  override val noteDocumentItemsQueries: NoteDocumentItemsQueries = NoteDocumentItemsQueries(driver,
      NoteDocumentItemsAdapter)

  override val noteDocumentsQueries: NoteDocumentsQueries = NoteDocumentsQueries(driver,
      NoteDocumentsAdapter)

  override val notesQueries: NotesQueries = NotesQueries(driver, NotesAdapter)

  override val projectArtifactsQueries: ProjectArtifactsQueries = ProjectArtifactsQueries(driver,
      ProjectArtifactsAdapter)

  override val projectAttachmentCrossRefQueries: ProjectAttachmentCrossRefQueries =
      ProjectAttachmentCrossRefQueries(driver)

  override val projectExecutionLogsQueries: ProjectExecutionLogsQueries =
      ProjectExecutionLogsQueries(driver)

  override val projectsQueries: ProjectsQueries = ProjectsQueries(driver, ProjectsAdapter)

  override val recentItemsQueries: RecentItemsQueries = RecentItemsQueries(driver)

  override val recurringTasksQueries: RecurringTasksQueries = RecurringTasksQueries(driver,
      RecurringTasksAdapter)

  override val remindersQueries: RemindersQueries = RemindersQueries(driver, RemindersAdapter)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 12

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE ActivityRecords (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    name TEXT NOT NULL,
          |    description TEXT,
          |    createdAt INTEGER NOT NULL,
          |    startTime INTEGER,
          |    endTime INTEGER,
          |    totalTimeSpentMinutes INTEGER,
          |    tags TEXT,
          |    relatedLinks TEXT,
          |    isCompleted INTEGER NOT NULL,
          |    activityType TEXT NOT NULL,
          |    parentProjectId TEXT
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Attachments (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    attachmentType TEXT NOT NULL,
          |    entityId TEXT NOT NULL,
          |    ownerProjectId TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ChatMessages (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    conversationId INTEGER NOT NULL,
          |    text TEXT NOT NULL,
          |    isFromUser INTEGER NOT NULL,
          |    isError INTEGER NOT NULL DEFAULT 0,
          |    timestamp INTEGER NOT NULL,
          |    isStreaming INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY(conversationId) REFERENCES Conversations(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ChecklistItems (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    checklistId TEXT NOT NULL,
          |    content TEXT NOT NULL,
          |    isChecked INTEGER NOT NULL DEFAULT 0,
          |    itemOrder INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Checklists (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    name TEXT NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ConversationFolders (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Conversations (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    title TEXT NOT NULL,
          |    creationTimestamp INTEGER NOT NULL,
          |    folderId INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE DailyMetrics (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    dayPlanId TEXT NOT NULL,
          |    date INTEGER NOT NULL,
          |    tasksPlanned INTEGER NOT NULL DEFAULT 0,
          |    tasksCompleted INTEGER NOT NULL DEFAULT 0,
          |    completionRate REAL NOT NULL DEFAULT 0.0,
          |    totalPlannedTime INTEGER NOT NULL DEFAULT 0,
          |    totalActiveTime INTEGER NOT NULL DEFAULT 0,
          |    completedPoints INTEGER NOT NULL DEFAULT 0,
          |    totalBreakTime INTEGER NOT NULL DEFAULT 0,
          |    morningEnergyLevel INTEGER,
          |    eveningEnergyLevel INTEGER,
          |    overallMood TEXT,
          |    stressLevel INTEGER,
          |    customMetrics TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE DayPlans (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    date INTEGER NOT NULL,
          |    name TEXT,
          |    status TEXT NOT NULL DEFAULT 'PLANNED',
          |    reflection TEXT,
          |    energyLevel INTEGER,
          |    mood TEXT,
          |    weatherConditions TEXT,
          |    totalPlannedMinutes INTEGER NOT NULL DEFAULT 0,
          |    totalCompletedMinutes INTEGER NOT NULL DEFAULT 0,
          |    completionPercentage REAL NOT NULL DEFAULT 0.0,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE DayTasks (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    dayPlanId TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT,
          |    goalId TEXT,
          |    projectId TEXT,
          |    activityRecordId TEXT,
          |    recurringTaskId TEXT,
          |    taskType TEXT,
          |    entityId TEXT,
          |    "order" INTEGER NOT NULL DEFAULT 0,
          |    priority TEXT NOT NULL DEFAULT 'MEDIUM',
          |    status TEXT NOT NULL DEFAULT 'NOT_STARTED',
          |    completed INTEGER NOT NULL DEFAULT 0,
          |    scheduledTime INTEGER,
          |    estimatedDurationMinutes INTEGER,
          |    actualDurationMinutes INTEGER,
          |    dueTime INTEGER,
          |    valueImportance REAL NOT NULL DEFAULT 0.0,
          |    valueImpact REAL NOT NULL DEFAULT 0.0,
          |    effort REAL NOT NULL DEFAULT 0.0,
          |    cost REAL NOT NULL DEFAULT 0.0,
          |    risk REAL NOT NULL DEFAULT 0.0,
          |    location TEXT,
          |    tags TEXT,
          |    notes TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER,
          |    completedAt INTEGER,
          |    nextOccurrenceTime INTEGER,
          |    points INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY(dayPlanId) REFERENCES DayPlans(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Goals (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    text TEXT NOT NULL,
          |    description TEXT,
          |    completed INTEGER NOT NULL DEFAULT 0,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER,
          |    tags TEXT,
          |    relatedLinks TEXT,
          |    valueImportance REAL NOT NULL DEFAULT 0.0,
          |    valueImpact REAL NOT NULL DEFAULT 0.0,
          |    effort REAL NOT NULL DEFAULT 0.0,
          |    cost REAL NOT NULL DEFAULT 0.0,
          |    risk REAL NOT NULL DEFAULT 0.0,
          |    weightEffort REAL NOT NULL DEFAULT 1.0,
          |    weightCost REAL NOT NULL DEFAULT 1.0,
          |    weightRisk REAL NOT NULL DEFAULT 1.0,
          |    rawScore REAL NOT NULL DEFAULT 0.0,
          |    displayScore INTEGER NOT NULL DEFAULT 0,
          |    scoringStatus TEXT NOT NULL,
          |    parentValueImportance REAL,
          |    impactOnParentGoal REAL,
          |    timeCost REAL,
          |    financialCost REAL,
          |    markdown TEXT
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE InboxRecords (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    text TEXT NOT NULL,
          |    createdAt INTEGER NOT NULL,
          |    itemOrder INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE LegacyNotes (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    content TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE LinkItems (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    linkData TEXT NOT NULL,
          |    createdAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ListItems (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    itemOrder INTEGER NOT NULL DEFAULT 0,
          |    entityId TEXT,
          |    itemType TEXT
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE NoteDocumentItems (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    listId TEXT NOT NULL,
          |    parentId TEXT,
          |    content TEXT NOT NULL,
          |    isCompleted INTEGER NOT NULL,
          |    itemOrder INTEGER NOT NULL,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE NoteDocuments (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    name TEXT NOT NULL,
          |    content TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    lastCursorPosition INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Notes (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    content TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ProjectArtifacts (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    content TEXT NOT NULL,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ProjectAttachmentCrossRef (
          |    projectId TEXT NOT NULL,
          |    attachmentId TEXT NOT NULL,
          |    attachmentOrder INTEGER NOT NULL,
          |    PRIMARY KEY(projectId, attachmentId)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ProjectExecutionLogs (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    projectId TEXT NOT NULL,
          |    timestamp INTEGER NOT NULL,
          |    type TEXT NOT NULL,
          |    description TEXT NOT NULL,
          |    details TEXT
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Projects (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    name TEXT NOT NULL,
          |    description TEXT,
          |    parentId TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER,
          |    tags TEXT,
          |    relatedLinks TEXT,
          |    isExpanded INTEGER NOT NULL DEFAULT 1,
          |    goalOrder INTEGER NOT NULL DEFAULT 0,
          |    isAttachmentsExpanded INTEGER NOT NULL DEFAULT 0,
          |    defaultViewMode TEXT,
          |    isCompleted INTEGER NOT NULL DEFAULT 0,
          |    isProjectManagementEnabled INTEGER NOT NULL DEFAULT 0,
          |    projectStatus TEXT,
          |    projectStatusText TEXT,
          |    projectLogLevel INTEGER,
          |    totalTimeSpentMinutes INTEGER DEFAULT 0,
          |    valueImportance REAL NOT NULL DEFAULT 1,
          |    valueImpact REAL NOT NULL DEFAULT 1,
          |    effort REAL NOT NULL DEFAULT 1,
          |    cost REAL NOT NULL DEFAULT 1,
          |    risk REAL NOT NULL DEFAULT 1,
          |    weightEffort REAL NOT NULL DEFAULT 1,
          |    weightCost REAL NOT NULL DEFAULT 1,
          |    weightRisk REAL NOT NULL DEFAULT 1,
          |    rawScore REAL NOT NULL DEFAULT 0,
          |    displayScore INTEGER DEFAULT 0,
          |    scoringStatus TEXT,
          |    showCheckboxes INTEGER NOT NULL DEFAULT 0,
          |    projectType TEXT,
          |    reservedGroup TEXT
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE RecentItems (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    type TEXT NOT NULL,
          |    lastAccessed INTEGER NOT NULL,
          |    displayName TEXT NOT NULL,
          |    target TEXT NOT NULL,
          |    isPinned INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE RecurringTasks (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    description TEXT,
          |    goalId TEXT,
          |    duration INTEGER,
          |    priority TEXT NOT NULL DEFAULT 'MEDIUM',
          |    points INTEGER NOT NULL DEFAULT 0,
          |    frequency TEXT NOT NULL,
          |    "interval" INTEGER NOT NULL DEFAULT 1,
          |    daysOfWeek TEXT,
          |    startDate INTEGER NOT NULL,
          |    endDate INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Reminders (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    entityId TEXT NOT NULL,
          |    entityType TEXT NOT NULL,
          |    reminderTime INTEGER NOT NULL,
          |    status TEXT NOT NULL,
          |    creationTime INTEGER NOT NULL,
          |    snoozeUntil INTEGER
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TRIGGER activityrecords_ai AFTER INSERT ON ActivityRecords BEGIN
          |    INSERT INTO ActivityRecordsFts(id, name, description)
          |    VALUES (new.id, new.name, new.description);
          |END
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TRIGGER activityrecords_au AFTER UPDATE ON ActivityRecords BEGIN
          |    DELETE FROM ActivityRecordsFts WHERE id = old.id;
          |    INSERT INTO ActivityRecordsFts(id, name, description)
          |    VALUES (new.id, new.name, new.description);
          |END
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TRIGGER activityrecords_ad AFTER DELETE ON ActivityRecords BEGIN
          |    DELETE FROM ActivityRecordsFts WHERE id = old.id;
          |END
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE ActivityRecordsFts USING fts5(
          |    id UNINDEXED,
          |    name,
          |    description
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE GoalsFts USING fts5(
          |    id,
          |    text,
          |    description,
          |    content='Goals',
          |    content_rowid='id'
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE NotesFts USING fts5(
          |    id,
          |    title,
          |    content,
          |    content='Notes',
          |    content_rowid='id'
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE ProjectsFts USING fts5(
          |    id,
          |    name,
          |    description,
          |    content='Projects',
          |    content_rowid='id'
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE RecurringTasksFts USING fts5(
          |    id,
          |    title,
          |    description,
          |    content='RecurringTasks',
          |    content_rowid='id'
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    private fun migrateInternal(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
    ): QueryResult.Value<Unit> {
      if (oldVersion <= 1 && newVersion > 1) {
        driver.execute(null, """
            |CREATE TABLE Checklists (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    projectId TEXT NOT NULL,
            |    name TEXT NOT NULL
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TABLE ChecklistItems (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    checklistId TEXT NOT NULL,
            |    content TEXT NOT NULL,
            |    isChecked INTEGER NOT NULL DEFAULT 0,
            |    itemOrder INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
      }
      if (oldVersion <= 2 && newVersion > 2) {
        driver.execute(null, """
            |CREATE TABLE Attachments (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    attachmentType TEXT NOT NULL,
            |    entityId TEXT NOT NULL,
            |    ownerProjectId TEXT,
            |    createdAt INTEGER NOT NULL,
            |    updatedAt INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TABLE ProjectAttachmentCrossRef (
            |    projectId TEXT NOT NULL,
            |    attachmentId TEXT NOT NULL,
            |    attachmentOrder INTEGER NOT NULL,
            |    PRIMARY KEY(projectId, attachmentId)
            |)
            """.trimMargin(), 0)
      }
      if (oldVersion <= 3 && newVersion > 3) {
        driver.execute(null, """
            |CREATE TABLE ProjectArtifacts (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    projectId TEXT NOT NULL,
            |    content TEXT NOT NULL,
            |    createdAt INTEGER NOT NULL,
            |    updatedAt INTEGER NOT NULL
            |)
            """.trimMargin(), 0)
      }
      if (oldVersion <= 4 && newVersion > 4) {
        driver.execute(null, """
            |CREATE TABLE ActivityRecords (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    name TEXT NOT NULL,
            |    description TEXT,
            |    createdAt INTEGER NOT NULL,
            |    startTime INTEGER,
            |    endTime INTEGER,
            |    totalTimeSpentMinutes INTEGER,
            |    tags TEXT,
            |    relatedLinks TEXT,
            |    isCompleted INTEGER NOT NULL,
            |    activityType TEXT NOT NULL,
            |    parentProjectId TEXT
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE VIRTUAL TABLE ActivityRecordsFts USING fts5(
            |    id UNINDEXED,
            |    name,
            |    description
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER activityrecords_ai AFTER INSERT ON ActivityRecords BEGIN
            |    INSERT INTO ActivityRecordsFts(id, name, description)
            |    VALUES (new.id, new.name, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER activityrecords_au AFTER UPDATE ON ActivityRecords BEGIN
            |    DELETE FROM ActivityRecordsFts WHERE id = old.id;
            |    INSERT INTO ActivityRecordsFts(id, name, description)
            |    VALUES (new.id, new.name, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER activityrecords_ad AFTER DELETE ON ActivityRecords BEGIN
            |    DELETE FROM ActivityRecordsFts WHERE id = old.id;
            |END
            """.trimMargin(), 0)
      }
      if (oldVersion <= 5 && newVersion > 5) {
        driver.execute(null, """
            |CREATE TABLE Reminders (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    entityId TEXT NOT NULL,
            |    entityType TEXT NOT NULL,
            |    reminderTime INTEGER NOT NULL,
            |    status TEXT NOT NULL,
            |    creationTime INTEGER NOT NULL,
            |    snoozeUntil INTEGER
            |)
            """.trimMargin(), 0)
      }
      if (oldVersion <= 6 && newVersion > 6) {
        driver.execute(null, """
            |CREATE TABLE DayPlans (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    date INTEGER NOT NULL,
            |    name TEXT,
            |    status TEXT NOT NULL DEFAULT 'PLANNED',
            |    reflection TEXT,
            |    energyLevel INTEGER,
            |    mood TEXT,
            |    weatherConditions TEXT,
            |    totalPlannedMinutes INTEGER NOT NULL DEFAULT 0,
            |    totalCompletedMinutes INTEGER NOT NULL DEFAULT 0,
            |    completionPercentage REAL NOT NULL DEFAULT 0.0,
            |    createdAt INTEGER NOT NULL,
            |    updatedAt INTEGER
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TABLE DayTasks (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    dayPlanId TEXT NOT NULL,
            |    title TEXT NOT NULL,
            |    description TEXT,
            |    goalId TEXT,
            |    projectId TEXT,
            |    activityRecordId TEXT,
            |    recurringTaskId TEXT,
            |    taskType TEXT,
            |    entityId TEXT,
            |    "order" INTEGER NOT NULL DEFAULT 0,
            |    priority TEXT NOT NULL DEFAULT 'MEDIUM',
            |    status TEXT NOT NULL DEFAULT 'NOT_STARTED',
            |    completed INTEGER NOT NULL DEFAULT 0,
            |    scheduledTime INTEGER,
            |    estimatedDurationMinutes INTEGER,
            |    actualDurationMinutes INTEGER,
            |    dueTime INTEGER,
            |    valueImportance REAL NOT NULL DEFAULT 0.0,
            |    valueImpact REAL NOT NULL DEFAULT 0.0,
            |    effort REAL NOT NULL DEFAULT 0.0,
            |    cost REAL NOT NULL DEFAULT 0.0,
            |    risk REAL NOT NULL DEFAULT 0.0,
            |    location TEXT,
            |    tags TEXT,
            |    notes TEXT,
            |    createdAt INTEGER NOT NULL,
            |    updatedAt INTEGER,
            |    completedAt INTEGER,
            |    nextOccurrenceTime INTEGER,
            |    points INTEGER NOT NULL DEFAULT 0,
            |    FOREIGN KEY(dayPlanId) REFERENCES DayPlans(id) ON DELETE CASCADE
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TABLE DailyMetrics (
            |    id TEXT NOT NULL PRIMARY KEY,
            |    dayPlanId TEXT NOT NULL,
            |    date INTEGER NOT NULL,
            |    tasksPlanned INTEGER NOT NULL DEFAULT 0,
            |    tasksCompleted INTEGER NOT NULL DEFAULT 0,
            |    completionRate REAL NOT NULL DEFAULT 0.0,
            |    totalPlannedTime INTEGER NOT NULL DEFAULT 0,
            |    totalActiveTime INTEGER NOT NULL DEFAULT 0,
            |    completedPoints INTEGER NOT NULL DEFAULT 0,
            |    totalBreakTime INTEGER NOT NULL DEFAULT 0,
            |    morningEnergyLevel INTEGER,
            |    eveningEnergyLevel INTEGER,
            |    overallMood TEXT,
            |    stressLevel INTEGER,
            |    customMetrics TEXT,
            |    createdAt INTEGER NOT NULL,
            |    updatedAt INTEGER
            |)
            """.trimMargin(), 0)
      }
      if (oldVersion <= 7 && newVersion > 7) {
      }
      if (oldVersion <= 8 && newVersion > 8) {
      }
      if (oldVersion <= 9 && newVersion > 9) {
      }
      if (oldVersion <= 10 && newVersion > 10) {
        driver.execute(null, """
            |CREATE TABLE Conversations (
            |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            |    title TEXT NOT NULL,
            |    creationTimestamp INTEGER NOT NULL,
            |    folderId INTEGER
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TABLE ChatMessages (
            |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            |    conversationId INTEGER NOT NULL,
            |    text TEXT NOT NULL,
            |    isFromUser INTEGER NOT NULL,
            |    isError INTEGER NOT NULL DEFAULT 0,
            |    timestamp INTEGER NOT NULL,
            |    isStreaming INTEGER NOT NULL DEFAULT 0,
            |    FOREIGN KEY(conversationId) REFERENCES Conversations(id) ON DELETE CASCADE
            |)
            """.trimMargin(), 0)
      }
      if (oldVersion <= 11 && newVersion > 11) {
        driver.execute(null, """
            |CREATE VIRTUAL TABLE GoalsFts USING fts5(
            |    id,
            |    text,
            |    description,
            |    content='Goals',
            |    content_rowid='id'
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER goals_after_insert AFTER INSERT ON Goals BEGIN
            |    INSERT INTO GoalsFts(id, text, description)
            |    VALUES (new.id, new.text, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER goals_after_delete AFTER DELETE ON Goals BEGIN
            |    DELETE FROM GoalsFts WHERE id = old.id;
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER goals_after_update AFTER UPDATE ON Goals BEGIN
            |    DELETE FROM GoalsFts WHERE id = old.id;
            |    INSERT INTO GoalsFts(id, text, description)
            |    VALUES (new.id, new.text, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE VIRTUAL TABLE ProjectsFts USING fts5(
            |    id,
            |    name,
            |    description,
            |    content='Projects',
            |    content_rowid='id'
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER projects_after_insert AFTER INSERT ON Projects BEGIN
            |    INSERT INTO ProjectsFts(id, name, description)
            |    VALUES (new.id, new.name, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER projects_after_delete AFTER DELETE ON Projects BEGIN
            |    DELETE FROM ProjectsFts WHERE id = old.id;
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER projects_after_update AFTER UPDATE ON Projects BEGIN
            |    DELETE FROM ProjectsFts WHERE id = old.id;
            |    INSERT INTO ProjectsFts(id, name, description)
            |    VALUES (new.id, new.name, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE VIRTUAL TABLE NotesFts USING fts5(
            |    id,
            |    title,
            |    content,
            |    content='Notes',
            |    content_rowid='id'
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER notes_after_insert AFTER INSERT ON Notes BEGIN
            |    INSERT INTO NotesFts(id, title, content)
            |    VALUES (new.id, new.title, new.content);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER notes_after_delete AFTER DELETE ON Notes BEGIN
            |    DELETE FROM NotesFts WHERE id = old.id;
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER notes_after_update AFTER UPDATE ON Notes BEGIN
            |    DELETE FROM NotesFts WHERE id = old.id;
            |    INSERT INTO NotesFts(id, title, content)
            |    VALUES (new.id, new.title, new.content);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE VIRTUAL TABLE RecurringTasksFts USING fts5(
            |    id,
            |    title,
            |    description,
            |    content='RecurringTasks',
            |    content_rowid='id'
            |)
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER recurring_tasks_after_insert AFTER INSERT ON RecurringTasks BEGIN
            |    INSERT INTO RecurringTasksFts(id, title, description)
            |    VALUES (new.id, new.title, new.description);
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER recurring_tasks_after_delete AFTER DELETE ON RecurringTasks BEGIN
            |    DELETE FROM RecurringTasksFts WHERE id = old.id;
            |END
            """.trimMargin(), 0)
        driver.execute(null, """
            |CREATE TRIGGER recurring_tasks_after_update AFTER UPDATE ON RecurringTasks BEGIN
            |    DELETE FROM RecurringTasksFts WHERE id = old.id;
            |    INSERT INTO RecurringTasksFts(id, title, description)
            |    VALUES (new.id, new.title, new.description);
            |END
            """.trimMargin(), 0)
      }
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
      var lastVersion = oldVersion

      callbacks.filter { it.afterVersion in oldVersion until newVersion }
      .sortedBy { it.afterVersion }
      .forEach { callback ->
        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
        callback.block(driver)
        lastVersion = callback.afterVersion + 1
      }

      if (lastVersion < newVersion) {
        migrateInternal(driver, lastVersion, newVersion)
      }
      return QueryResult.Unit
    }
  }
}
