# Опис поточної проблеми: Помилка компіляції KSP в `ProjectScreenViewModel`

Цей документ описує поточну проблему з компіляцією, з якою ми зіткнулися, щоб надати повний контекст для іншої мовної моделі.

## 1. Опис проблеми

Ми намагаємося змусити `ProjectScreenViewModel` компілюватися після перенесення на нову KMP-архітектуру. Однак ми постійно стикаємося з помилкою KSP (Kotlin Symbol Processing), пов'язаною з неможливістю знайти провайдер для `LegacyNotesRepository`.

Незважаючи на те, що ми перевірили, що `LegacyNotesRepository` надається в `RepositoryModule`, а `ProjectScreenModule` та `ProjectScreenViewModel` використовують правильні імпорти та типи, KSP все одно не може знайти відповідний провайдер.

## 2. Текст помилок

Основна помилка, яку ми отримуємо:

```
e: [ksp] Unresolved reference: <ERROR TYPE: LegacyNotesRepository>
/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/di/ProjectScreenModule.kt:25: provideProjectScreenViewModel(...)
/home/romankozak/studio/public/forwardapp-suit/forwardapp-android/apps/android/src/main/kotlin/com/romankozak/forwardappmobile/di/AppComponent.kt:33: projectScreenViewModel: com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
```

## 3. Значимі файли

Ось список файлів, які є релевантними до цієї проблеми:

*   `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/ProjectScreenViewModel.kt`
*   `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/di/ProjectScreenModule.kt`
*   `apps/android/src/main/kotlin/com/romankozak/forwardappmobile/di/AppComponent.kt`
*   `apps/android/src/main/kotlin/com/romankozak/forwardappmobile/di/RepositoryModule.kt`

## 4. Вміст релевантних файлів

### `ProjectScreenViewModel.kt`

```kotlin
package com.romankozak.forwardappmobile.features.projectscreen

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.repository.LegacyNotesRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.ProjectArtifact
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository.ListItemRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.RemindersRepository
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import java.util.Calendar

// TODO: [GM-31] This file needs to be refactored with the new KMP architecture.

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ResetSwipeState(val itemId: String) : UiEvent()
    data class ScrollTo(val index: Int) : UiEvent()
    data class NavigateBackAndReveal(val projectId: String) : UiEvent()
    data class HandleLinkClick(val link: RelatedLink) : UiEvent()
    data class OpenUri(val uri: String) : UiEvent()
    data object ScrollToLatestInboxRecord : UiEvent()
}

enum class GoalActionType {
    CreateInstance,
    MoveInstance,
    CopyGoal,
    AddLinkToList,
    ADD_LIST_SHORTCUT,
}

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val itemContent: Any) : GoalActionDialogState()
}

data class UiState(
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: Any = Any(),
    val newlyAddedItemId: String? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val inputValue: TextFieldValue = TextFieldValue(""),
    val resetTriggers: Map<String, Int> = emptyMap(),
    val swipedItemId: String? = null,
    val showAddWebLinkDialog: Boolean = false,
    val showAddObsidianLinkDialog: Boolean = false,
    val itemToHighlight: String? = null,
    val inboxRecordToHighlight: String? = null,
    val needsStateRefresh: Boolean = false,
    val currentView: Any = Any(),
    val isViewModePanelVisible: Boolean = false,
    val showRecentProjectsSheet: Boolean = false,
    val showImportFromMarkdownDialog: Boolean = false,
    val showImportBacklogFromMarkdownDialog: Boolean = false,
    val refreshTrigger: Int = 0,
    val detectedReminderSuggestion: String? = null,
    val detectedReminderCalendar: Calendar? = null,
    val nerState: Any = Any(),
    val recordForReminderDialog: ActivityRecord? = null,
    // val projectTimeMetrics: ProjectTimeMetrics? = null,
    val showShareDialog: Boolean = false,
    val showCreateNoteDocumentDialog: Boolean = false,
    val showRemindersDialog: Boolean = false,
    val itemForRemindersDialog: Any? = null,
    val remindersForDialog: List<Reminder> = emptyList(),
    val logEntryToEdit: ProjectExecutionLog? = null,
    val artifactToEdit: ProjectArtifact? = null,
    val selectedDashboardTab: Any = Any(),
    val showNoteDocumentEditor: Boolean = false,
    val showDisplayPropertiesDialog: Boolean = false,
    val showCheckboxes: Boolean = false,
) {
    val isSelectionModeActive: Boolean get() = selectedItemIds.isNotEmpty()
}

@Inject
class ProjectScreenViewModel(
  private val application: Application,
  private val projectRepository: ProjectRepository,
  // private val settingsRepository: SettingsRepository,
  // private val contextHandler: ContextHandler,
  // private val alarmScheduler: AlarmScheduler,
  // private val nerManager: NerManager,
  // private val reminderParser: ReminderParser,
  private val activityRepository: ActivityRecordsRepository,
  // private val projectMarkdownExporter: ProjectMarkdownExporter,
  private val savedStateHandle: SavedStateHandle,
  private val dayManagementRepository: DayPlanRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val goalRepository: GoalRepository,
  private val listItemRepository: ListItemRepository,
  private val noteDocumentRepository: NoteDocumentsRepository,
  private val checklistRepository: ChecklistRepository,
  private val reminderRepository: RemindersRepository,
  private val recentItemsRepository: RecentItemRepository,
  private val projectLogRepository: ProjectExecutionLogsRepository,
  private val projectArtifactRepository: ProjectArtifactRepository,
  private val legacyNotesRepository: LegacyNotesRepository,
  private val inboxRepository: InboxRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
}
```

### `ProjectScreenModule.kt`

```kotlin
package com.romankozak.forwardappmobile.features.projectscreen.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.features.notes.legacy.domain.repository.LegacyNotesRepository
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository.ListItemRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.RemindersRepository
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface ProjectScreenModule {
    val legacyNotesRepository: LegacyNotesRepository
    @Provides
    fun provideProjectScreenViewModel(
        application: Application,
        projectRepository: ProjectRepository,
        activityRepository: ActivityRecordsRepository,
        savedStateHandle: SavedStateHandle,
        dayManagementRepository: DayPlanRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        goalRepository: GoalRepository,
        listItemRepository: ListItemRepository,
        noteDocumentRepository: NoteDocumentsRepository,
        checklistRepository: ChecklistRepository,
        reminderRepository: RemindersRepository,
        recentItemsRepository: RecentItemRepository,
        projectLogRepository: ProjectExecutionLogsRepository,
        projectArtifactRepository: ProjectArtifactRepository,
        legacyNotesRepository: LegacyNotesRepository,
        inboxRepository: InboxRepository,
    ): ProjectScreenViewModel = ProjectScreenViewModel(
        application = application,
        projectRepository = projectRepository,
        activityRepository = activityRepository,
        savedStateHandle = savedStateHandle,
        dayManagementRepository = dayManagementRepository,
        ioDispatcher = ioDispatcher,
        goalRepository = goalRepository,
        listItemRepository = listItemRepository,
        noteDocumentRepository = noteDocumentRepository,
        checklistRepository = checklistRepository,
        reminderRepository = reminderRepository,
        recentItemsRepository = recentItemsRepository,
        projectLogRepository = projectLogRepository,
        projectArtifactRepository = projectArtifactRepository,
        legacyNotesRepository = legacyNotesRepository,
        inboxRepository = inboxRepository,
    )
}
```

### `AppComponent.kt`

```kotlin
package com.romankozak.forwardappmobile.di

import android.app.Application
import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import com.romankozak.forwardappmobile.features.mainscreen.MainScreenViewModel

@Scope
annotation class AndroidSingleton

@AndroidSingleton
@Component
abstract class AppComponent(
    // Передаємо Application як параметр компонента
    @get:Provides val application: Application,
    @get:Provides val savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : DatabaseModule,
    RepositoryModule,
    DispatcherModule,
    com.romankozak.forwardappmobile.features.mainscreen.di.MainScreenModule,
    com.romankozak.forwardappmobile.shared.features.aichat.di.AiChatModule,
    com.romankozak.forwardappmobile.shared.features.search.di.SearchModule,
    com.romankozak.forwardappmobile.features.projectscreen.di.ProjectScreenModule(
        legacyNotesRepository = provideLegacyNotesRepository(database, ioDispatcher)
    ) {

    @Provides
    @ApplicationContext
    fun provideApplicationContext(): Context = application.applicationContext

    // Entry points / factories
    abstract val mainScreenViewModel: MainScreenViewModel
    abstract val projectScreenViewModel: com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel

    companion object
}
```

### `RepositoryModule.kt`

```kotlin
package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.data.repository.AttachmentsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.domain.repository.AttachmentsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.repository.LinkItemsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.domain.repository.LinkItemsRepository
import com.romankozak.forwardappmobile.shared.features.activitytracker.data.repository.ActivityRecordsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.repository.LegacyNotesRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.repository.LegacyNotesRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository.DayPlanRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository.DayTaskRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayTaskRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.data.repository.DailyMetricsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.repository.DailyMetricsRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.repository.RecurringTaskRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.repository.RecurringTaskRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.data.repository.NoteDocumentsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.repository.ProjectExecutionLogsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.data.repository.ProjectArtifactRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.repository.InboxRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.data.repository.ChecklistRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import com.romankozak.forwardappmobile.shared.features.reminders.data.repository.RemindersRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.RemindersRepository
import com.romankozak.forwardappmobile.shared.features.projects.core.data.repository.ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.recent.data.repository.RecentItemRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import com.romankozak.forwardappmobile.shared.features.aichat.data.repository.ChatRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.aichat.data.repository.ConversationFolderRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ChatRepository
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ConversationFolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface RepositoryModule {

    @Provides
    @AndroidSingleton
    fun provideProjectRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectRepository = ProjectRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRecentItemRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemRepository = RecentItemRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideConversationFolderRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ConversationFolderRepository = ConversationFolderRepositoryImpl(database, ioDispatcher)



    @Provides
    @AndroidSingleton
    fun provideInboxRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): InboxRepository = InboxRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideLegacyNotesRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LegacyNotesRepository = LegacyNotesRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideNoteDocumentsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NoteDocumentsRepository = NoteDocumentsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideProjectArtifactRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectArtifactRepository = ProjectArtifactRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideProjectExecutionLogsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectExecutionLogsRepository = ProjectExecutionLogsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideAttachmentsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AttachmentsRepository = AttachmentsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideLinkItemsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LinkItemsRepository = LinkItemsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideChecklistRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ChecklistRepository = ChecklistRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideActivityRecordsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ActivityRecordsRepository = ActivityRecordsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRemindersRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RemindersRepository = RemindersRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideDayPlanRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DayPlanRepository = DayPlanRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideDayTaskRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DayTaskRepository = DayTaskRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRecurringTaskRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecurringTaskRepository = RecurringTaskRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideDailyMetricsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DailyMetricsRepository = DailyMetricsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideGoalRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepository = com.romankozak.forwardappmobile.shared.features.goals.data.repository.GoalRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideListItemRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository.ListItemRepository = com.romankozak.forwardappmobile.shared.features.projects.listitems.data.repository.ListItemRepositoryImpl(database, ioDispatcher)
}
```

## 5. Історія спроб вирішення

1.  **Початкова компіляція:** Перша спроба компіляції `ProjectScreenViewModel` після перенесення з `dev` гілки показала велику кількість помилок, пов'язаних з відсутніми класами та залежностями.
2.  **Коментування коду:** Ми почали коментувати проблемні частини коду, щоб досягти компіляції. Це включало старі хендлери (`InputHandler`, `SelectionHandler` і т.д.), стару систему навігації та виклики до репозиторіїв, які були змінені.
3.  **Виправлення імпортів:** Ми виправили кілька очевидних помилок в імпортах, наприклад, `RecentItemType` на `String`, `ReminderRepository` на `RemindersRepository`.
4.  **Проблема з `LegacyNotesRepository`:** Після виправлення попередніх помилок ми зіткнулися з постійною помилкою KSP, пов'язаною з `LegacyNotesRepository`.
5.  **Спроби вирішення проблеми з `LegacyNotesRepository`:**
    *   Перевірили, що `RepositoryModule` надає `LegacyNotesRepository`.
    *   Перевірили, що `ProjectScreenViewModel` та `ProjectScreenModule` використовують правильний тип `LegacyNotesRepository`.
    *   Спробували очистити білд (`make clean`).
    *   Спробували явно передати `LegacyNotesRepository` в `ProjectScreenModule` з `AppComponent`.
    *   Спробували успадкувати `ProjectScreenModule` від `RepositoryModule`.

Жоден з цих кроків не вирішив проблему. KSP вперто не може знайти провайдер для `LegacyNotesRepository` в контексті `ProjectScreenModule`.

## 6. План подальших кроків

1.  **Ізолювати проблему:** Створити мінімальний приклад, який відтворює проблему з `LegacyNotesRepository` та `kotlin-inject`. Це допоможе зрозуміти, чи проблема в нашому коді, чи в самій бібліотеці `kotlin-inject`.
2.  **Перевірити залежності:** Ще раз уважно перевірити всі залежності, пов'язані з `kotlin-inject` та KSP, на предмет конфліктів або неправильних версій.
3.  **Спростити DI граф:** Тимчасово видалити всі залежності з `ProjectScreenViewModel`, крім `LegacyNotesRepository`, щоб перевірити, чи не викликає конфлікт якась інша залежність.
4.  **Звернутися до документації `kotlin-inject`:** Можливо, ми пропустили якийсь важливий аспект конфігурації, особливо щодо взаємодії між модулями.

## 7. Додаткова інформація

Я можу надати будь-який додатковий код або деталі, які можуть знадобитися для вирішення цієї проблеми.
