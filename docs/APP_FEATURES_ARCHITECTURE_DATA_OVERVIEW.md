# ForwardApp Android: повний огляд фіч, архітектури та сутностей даних

Оновлено: 2026-02-08
Проєкт: `forwardapp-android`

## 1) Що це за система

ForwardApp — Android-додаток для персонального/операційного менеджменту життя та роботи.
Ключова ідея: **контексти** (раніше `projects`) + багаторежимний перегляд (backlog, inbox, dashboard, attachments, direction, notes, log, artifact, vet_case), денне планування, тактичні місії, AI-інструменти, локальна БД та синхронізація через backup/snapshot/Wi‑Fi.

---

## 2) Модульна структура (Gradle)

У `settings.gradle.kts` підключені модулі:
- `:app` — основний Android UI/feature-модуль.
- `:core-data-models` — єдине джерело моделей даних (entities, sync snapshots, mappers).
- `:core-data-interfaces` — контракти для sync/data-source API.
- `:sync` — реалізації синхронізації (on/off варіанти + mapper/репозиторії).

Техстек (основне):
- Kotlin + Jetpack Compose + Navigation Compose
- Room (DB v102), Hilt DI
- DataStore (налаштування), WorkManager
- Ktor/OkHttp/Retrofit (мережа), Gson
- Firebase (analytics/crashlytics/remote-config)

---

## 3) Архітектурні шари

## 3.1 `app` (presentation + application services)
- `features/*` — вертикальні фічі (UI + ViewModel + feature-specific domain/data).
- `data/repository/*` — репозиторії поверх DAO/сервісів.
- `database/AppDatabase.kt` — агрегатор Room сутностей та DAO.
- `core/*` — інфраструктура: navigation, capability/runtime, context session, DI, sync adapters, theme.

## 3.2 `core-data-models` (domain/data contracts)
- `core/data/models/entities/*` — Room-сутності, enums, UI-моделі, cross-ref.
- `core/data/models/sync/*` — backup/snapshot-моделі, DTO-мапінг, трансформації.

## 3.3 `core-data-interfaces`
- Інтерфейси sync API та локальних data source (`SyncApi`, `AttachmentsRepository`, `*LocalDataSource`).

## 3.4 `sync`
- Реалізації sync logic, merge/import/export, legacy migration.
- Variant split:
- `src/syncOn` — реальна синхронізація.
- `src/syncOff` — no-op реалізації.

---

## 4) Фічі (повний інвентар)

Пакети фіч у `app/src/main/java/com/romankozak/forwardappmobile/features`:
- `activitytracker` — трекінг активностей, XP/anti-XP.
- `ai` (`chat`, `insights`, `data`) — чати/події/інсайти.
- `attachments` (`specific_types`, `ui`, `data`) — бібліотека вкладень і редактори типів.
- `context_lab` — експериментальна контекстна гілка.
- `contexts` (`ui`, `data`, `di`) — ядро системи контекстів.
- `daymanagement` (`ui`, `utils`) — план/трекінг/аналітика дня.
- `dev_task` — окремі dev task екрани.
- `globalsearch` — глобальний пошук по сутностях.
- `lifestate` — екран стану життя/AI life management.
- `mainscreen` — Command Deck, вкладки Dashboard/Today/Tactics/Strategy/Core.
- `missions` (`data`, `domain`, `presentation`) — tactical missions.
- `recent` — недавні об’єкти.
- `reminders` (`list`, `dialogs`, `components`) — нагадування.
- `settings` — налаштування (в т.ч. Wi‑Fi sync settings).
- `sharing` — прийом external share.
- `strategicmanagement` — стратегічний менеджмент.
- `sync` (`selectiveimport`, `di`) — sync UI та selective import.
- `vet_case` — домен ветеринарних кейсів.

---

## 5) Навігація та екранна композиція

Головний NavHost: `core/navigation/routes/AppNavigation.kt`.
Ключові константи route:
- `MAIN_GRAPH_ROUTE`
- `COMMAND_DECK_ROUTE`
- `GOAL_LISTS_ROUTE`
- `AI_INSIGHTS_ROUTE`
- `LIFE_STATE_ROUTE`
- `SELECTIVE_IMPORT_ROUTE`
- `KANBAN_ROUTE`
- `VET_CASE_SUMMARY_ROUTE`
- `VET_CASE_HISTORY_ROUTE`

Типобезпечна абстракція `NavTarget`:
- context hierarchy/detail
- note document / checklist / global search
- settings / reminders / tracker / AI / tactical
- structure/preset/editor routes

`MainScreenLayout` = Command Deck з вкладками:
- Dashboard
- Today
- Tactics
- StrategicArc
- Strategy
- Core

Кожна вкладка має свій header + bottom panel + екран/флоу.

---

## 6) Capability/Feature архітектура

## 6.1 Runtime Feature Flags (`FeatureFlag` + `FeatureToggles`)
Флаги в `core/config/FeatureFlag.kt`:
- `AttachmentsLibrary`
- `ScriptsLibrary`
- `AllowSystemProjectMoves`
- `PlanningModes`
- `WifiSync`
- `StrategicManagement`
- `AiChat`
- `AiInsights`
- `AiLifeManagement`

`FeatureToggles`:
- має default map (залежно від `BuildConfig.IS_EXPERIMENTAL_BUILD`)
- підтримує runtime override через `StateFlow<Map<FeatureFlag, Boolean>>`

## 6.2 Capability Gate (контекстні можливості)
`CapabilityId` (`value class`) + registries/gate:
- `ContextCapabilitiesResolver` перетворює `ContextConfiguration` -> `Set<CapabilityId>`.
- `ContextViewPolicy.availableViews()` мапить capabilities у доступні `ContextViewMode`.
- `ContextSessionStore` зберігає session state контексту:
- `enabledCapabilities`
- `availableViews`
- `currentView`
- `ContextRoleRegistry` дає default capability sets для ролей (`default`, `development`, `vet_patient`).

Це ключовий механізм динамічного ввімкнення/вимкнення видів контексту.

---

## 7) Core підсистема контекстів

## 7.1 Context Hierarchy Screen
`features/contexts/ui/context_hierarchy_screen/*`
- ViewModel-компоновка через use-case шар:
- `SearchUseCase`, `PlanningUseCase`, `SyncUseCase`, `NavigationUseCase`, `SettingsUseCase`, `ThemingUseCase`, `ContextActionsUseCase`, `DialogUseCase`.
- Flat hierarchy + tree projection + planning mode (All/Today/etc).
- Bottom nav, recent sheets, search dialog, reveal/scroll навігація.

## 7.2 Context Detail Screen
`features/contexts/ui/context_screen/*`
- Один великий orchestration ViewModel (`ContextScreenViewModel`) + окремі handlers:
- `InputHandler`, `SelectionHandler`, `InboxHandler`, markdown handlers, navigation handlers.
- Керує:
- backlog/list content
- attachments list
- reminders
- current view mode
- artifact/doc editors
- capability-driven view availability через `contextSessionState`.

## 7.3 Context Settings
`features/contexts/ui/context_properties/*`
- `ContextSettingsViewModel` редагує метадані контексту і конфіг фіч.
- Модель фіч базується на:
- role preset (`basePresetCode`)
- legacy bool flags (`enableInbox`, `enableBacklog`, ...)
- `experimentalCapabilityIds`.
- Tabs: General / Display / Features / Evaluation / Reminders.

---

## 8) Інші функціональні блоки

## 8.1 Day Management
- `DayManagementScreen` з вкладками TRACK / PLAN / DASHBOARD / ANALYTICS.
- `DayPlanViewModel` + `DayTask`, `DayPlan`, `DailyMetric`, `RecurringTask`.
- Дії: add task, reorder, reminders, copy to today, navigation by day.

## 8.2 Tactical Missions
- `features/missions/*`.
- Сутності: `TacticalMission`, `TacticalMissionAttachmentCrossRef`.
- CRUD + прив’язка вкладень + статус/пріоритет/терміни.

## 8.3 Attachments
- Типи: note document, checklist, script, link та ін.
- Є library screen + контекстний перегляд + cross-ref до контекстів.

## 8.4 AI
- Conversation folders, conversations, messages.
- AI events/insights з окремими DAO/репозиторіями.
- Екрани: AI chat / insights / life state.

## 8.5 Global Search
- Пошук через FTS-таблиці + агреговані result models.

## 8.6 Sync
- Full backup, merge, attachments sync, Wi‑Fi sync, selective import.
- Legacy V1 -> snapshot V2 migration в `SyncMapper`.

---

## 9) Модель даних (Room) — центральний огляд

Головна БД: `AppDatabase` (version = 102).

Групи сутностей:

## 9.1 Контексти/структура/backlog
- `Context`
- `Goal`
- `BacklogItem`
- `BacklogOrder`
- `LinkItemEntity`
- `DirectionItemEntity`
- `ContextLog`
- `InboxRecord`
- `ContextConfiguration`
- `ContextStructureItem`
- `ContextRoleProfile`
- `ContextRoleProfileItem`
- `ContextArtifact`

## 9.2 Attachments
- `AttachmentEntity`
- `ContextAttachmentCrossRef`
- `ChecklistEntity`
- `ChecklistItemEntity`
- `NoteDocumentEntity`
- `ScriptEntity`
- `LegacyNoteEntity`

## 9.3 Day management
- `DayPlan`
- `DayTask`
- `DailyMetric`
- `RecurringTask`

## 9.4 Activity/Reminders/Recent
- `ActivityRecord`
- `Reminder`
- `RecentItem`

## 9.5 AI
- `ConversationFolderEntity`
- `ConversationEntity`
- `ChatMessageEntity`
- `AiEventEntity`
- `AiInsightEntity`

## 9.6 Tactical
- `TacticalMission`
- `TacticalMissionAttachmentCrossRef`

## 9.7 System/meta
- `SystemAppEntity`
- `LifeSystemStateEntity`

## 9.8 FTS сутності
- `GoalFts`
- `ContextsFts`
- `ActivityRecordFts`
- `LegacyNoteFts`
- `RecurringTaskFts`

DAOs підключені в `AppDatabase`: context/goal/list/direction/inbox/structure, activity/day/reminder/recent, ai, missions, attachments, script/system.

---

## 10) Сутності в `core-data-models` (розширений каталог)

Основні файли домену:
- `entities/Context.kt`
- `entities/ContextEntities.kt`
- `entities/ContextAdditionalModels.kt`
- `entities/ContextConfiguration.kt`
- `entities/DirectionItemEntity.kt`
- `entities/BacklogItemContent.kt`
- `entities/ChecklistEntity.kt`
- `entities/NoteDocumentEntity.kt`
- `entities/AttachmentModels.kt`
- `entities/Reminder.kt`
- `entities/RecentItem.kt`
- `entities/ActivityRecord.kt`
- `entities/ScriptEntity.kt`
- `entities/ContextArtifact.kt`
- `entities/SystemAppEntity.kt`
- `entities/LifeSystemStateEntity.kt`
- `entities/ai/*`
- `entities/day_management/*`
- `entities/tactical/*`

Ключові enum/value типи:
- `ContextViewMode`
- `RecentItemType`
- `MissionStatus`, `MissionPriority`
- `RecurrenceFrequency`, `RecurrenceRule`
- Scoring status values

---

## 11) Sync data model (V1 + V2 snapshots)

`core-data-models/core/data/models/sync/*`:
- backup container: `FullAppBackup`, `BackupDiff`, `LegacyBackupDiff`, `DatabaseContent`
- snapshot container: `SnapshotBundle`
- snapshots по доменах:
- `snapshots/context/*`
- `snapshots/attachments/*`
- `snapshots/day_management/*`
- `snapshots/ai/*`
- `snapshots/activity/*`
- `snapshots/reminders/*`
- `snapshots/tactical/*`
- mapper layer: `sync/mappers/*` + `SnapshotMapper`

`sync/SyncMapper.kt`:
- міграція legacy DB content у snapshot bundle
- normalizers (null/default normalization)
- unified updatedTs policy (LWW)
- auto-linking attachment crossrefs для checklist/doc у migration path

---

## 12) DI та runtime wiring

`core/di/*`:
- `DatabaseModule` — Room DB + DAO providers, migration chain.
- `RepositoryModule` — binds/provides repo/data-source sync wiring.
- `LogicModule` — capability registry, context controller, session store, view resolver.
- `NavigationModule` — dispatcher/navigation manager wiring.
- `NetworkModule` — HTTP/network clients.
- `ViewRegistryModule` — capability-bound view descriptors (kanban, vet summary/history).
- `ViewModelModule`, `AiModule`, `ScriptsModule`, `DispatchersModule`.

DI стек: Hilt Singleton graph + feature-level ViewModels.

---

## 13) Потоки стану та патерни керування станом

Використовується реактивний підхід на `StateFlow`/`SharedFlow`:
- ViewModel -> UI: `collectAsStateWithLifecycle`.
- Screen event bus: channels/shared flows для навігації і snackbars.
- Складні екрани (ContextScreen, HierarchyScreen) декомпозовані на use-cases/handlers.
- Навігація: `NavTarget` + `NavTargetRouter` + `EnhancedNavigationManager`.
- Context runtime: `ContextSessionStore.dispatch(SyncFromConfig/SelectView)`.

---

## 14) Поточний зріз архітектурних сильних сторін

- Чітке модульне розділення data-models / interfaces / sync / app.
- Винесений capability/view runtime, що дозволяє role-based і config-based варіативність UI.
- Велика Room-схема з підтримкою FTS і довгою історією міграцій.
- Sync шар підтримує legacy + snapshot формат, selective import і Wi‑Fi сценарії.
- Feature-oriented структура з окремими підсистемами (day management, tactical, AI).

---

## 15) Карта ключових файлів (для швидкого входу)

Архітектура/ядро:
- `app/src/main/java/com/romankozak/forwardappmobile/core/navigation/routes/AppNavigation.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextSessionStore.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextViewPolicy.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/di/LogicModule.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/di/DatabaseModule.kt`

Контексти:
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_hierarchy_screen/ContextHierarchyScreenViewModel.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenViewModel.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_properties/ContextSettingsViewModel.kt`

Дані:
- `app/src/main/java/com/romankozak/forwardappmobile/database/AppDatabase.kt`
- `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/entities/*`
- `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/*`

Sync:
- `sync/src/main/java/com/romankozak/forwardappmobile/sync/SyncMapper.kt`
- `sync/src/syncOn/java/com/romankozak/forwardappmobile/sync/*`
- `core-data-interfaces/src/main/java/com/romankozak/forwardappmobile/sync/*`

---

## 16) Короткий висновок

Поточна архітектура — це **feature-first Compose app** з потужним data-core (Room + snapshots), capability-driven контекстним runtime і окремим sync модулем для імпорту/експорту/мережевої синхронізації. Головна доменна вісь — `Context` та його конфігурація, від якої динамічно залежить набір активних видів, доступних сценаріїв і UI-поведінка.
