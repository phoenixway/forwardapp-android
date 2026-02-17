# Огляд підсистеми User Awareness і аналітики стану користувача

Дата: 2026-02-17

## 1. Призначення

Підсистема `User Awareness` задає глобальний режим стану користувача і впливає на:
- інтерпретацію продуктивності;
- тон/логіку підказок;
- агреговану аналітику за контекстами;
- AI Insights (реакції на ризикові сигнали).

Підтримувані стани:
- `NORMAL`
- `CRISIS` (рівень 1..3, optional label)
- `EXHAUSTION`
- `UNPRODUCTIVE`

## 2. Дані і збереження

### 2.1 Інтервали стану користувача

Таблиця: `user_state_intervals`

Поля:
- `id`
- `stateType`
- `crisisLevel`
- `label`
- `source`
- `createdFromActivityId`
- `startedAt`
- `endedAt` (`null` => активний)

Реалізація:
- Entity: `core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/entities/UserStateIntervalEntity.kt`
- DAO: `app/src/main/java/com/romankozak/forwardappmobile/data/dao/UserStateIntervalDao.kt`
- Міграція: `MIGRATION_105_106` у `app/src/main/java/com/romankozak/forwardappmobile/data/database/Migrations.kt`

### 2.2 Аудит в activity-трекері

В `activity_records` додані поля:
- `rawNoteText`
- `noteText`
- `stateEventType`
- `stateEventCrisisLevel`
- `stateEventLabel`
- `stateEventApplied`

Це дозволяє зберігати сирий текст, очищений текст і факт/деталі застосованої команди стану.

## 3. Ввід через slash-команди

Модуль:
- `app/src/main/java/com/romankozak/forwardappmobile/domain/userawareness/StateSlashCommandParser.kt`

Підтримка:
- `/normal`
- `/exhaustion`
- `/unproductive`
- `/crisis [level?] [label?]`

Правила:
- команда може бути будь-де в нотатці;
- якщо команд декілька, перемагає остання;
- `/crisis` без рівня => рівень `1`;
- невалідний рівень (`/crisis 7`) ігнорується;
- label trim + max 80;
- URL-подібні фрагменти (`https://...`) не парсяться як команда.

Результат парсингу:
- `cleanedText` (команди прибрані),
- `detectedChange`,
- `allCommandsFound`.

## 4. Керування інтервалами стану

Репозиторій:
- `app/src/main/java/com/romankozak/forwardappmobile/data/repository/UserAwarenessRepository.kt`

Гарантії:
- lazy/default `NORMAL` при першій взаємодії;
- при зміні стану закривається попередній активний інтервал і відкривається новий;
- idempotency: повтор того самого стану не створює новий інтервал;
- нормалізація `CRISIS` (`level` 1..3, label trim).

Наявні API:
- `observeActiveState()`
- `getActiveState()`
- `setStateManual(...)`
- `applyStateChangeFromActivity(...)`
- `getStateTimeline(from, to)`
- `getStateAt(atMillis)`
- `getNudgePolicy(...)`
- `getQuotaPolicy(...)`
- `getWeeklyReviewFlags(...)`

## 5. Інтеграція з Activity Tracker

Репозиторій:
- `app/src/main/java/com/romankozak/forwardappmobile/data/repository/ActivityRepository.kt`

Ключова поведінка:
- парсинг відбувається на створенні запису;
- запис і зміна стану виконуються транзакційно;
- якщо введено тільки slash-команду без тексту:
  - стан перемикається,
  - activity-запис не створюється (`STATE_CHANGED_ONLY`);
- UI показує snackbar-реакцію на зміну стану.

ViewModel:
- `app/src/main/java/com/romankozak/forwardappmobile/features/activitytracker/ActivityTrackerViewModel.kt`

## 6. Аналітика стану справ

### 6.1 По контекстах і станах

Метод:
- `getTrackedMinutesByContextAndState(from, to)` у `ActivityRepository`

Що робить:
- рахує хвилини виконаних context-активностей;
- кладе тривалість у bucket стану (`NORMAL/CRISIS/EXHAUSTION/UNPRODUCTIVE`) по стану на момент `startTime`.

Поточна модель:
- v1-апроксимація: весь запис відноситься до стану на старті, без точного розбиття по overlap інтервалів.

### 6.2 Тижнева турбулентність

`DayAnalyticsViewModel` запитує `getWeeklyReviewFlags`.
Якщо є `CRISIS/EXHAUSTION` у періоді, UI позначає тиждень як турбулентний і показує м’яку інтерпретацію квот.

Файли:
- `app/src/main/java/com/romankozak/forwardappmobile/features/daymanagement/ui/dayanalitics/DayAnalyticsViewModel.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/daymanagement/ui/dayanalitics/DayAnalyticsScreen.kt`

## 7. Реакції системи (UI + AI Insights)

### 7.1 Глобальний badge стану

- Badge у хедерах головних вкладок (окрім `NORMAL`).
- Tap відкриває quick-switch діалог.

Файли:
- `app/src/main/java/com/romankozak/forwardappmobile/features/userawareness/UserAwarenessBadge.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/MainScreenLayout.kt`

### 7.2 Focus contexts

Окрема підсистема інтервалів фокусу:
- Entity/DAO/Repo:
  - `FocusContextIntervalEntity.kt`
  - `FocusContextIntervalDao.kt`
  - `FocusContextRepository.kt`
- Використання:
  - Dashboard tab (секція фокус-контекстів з діями open/start tracking/defocus),
  - контекстні меню (hierarchy/context screen).

### 7.3 AI Insights як реактивний шар

Файли:
- ViewModel/UI: `app/src/main/java/com/romankozak/forwardappmobile/features/ai/insights/AiInsightsScreen.kt`
- Policy engine: `app/src/main/java/com/romankozak/forwardappmobile/features/ai/insights/InsightPolicyEngine.kt`

Тригери перерахунку:
- зміни activity log;
- зміни active awareness state;
- зміни active focus contexts;
- періодичний ticker (кожні 30 хв).

Поточні правила реакції:
- немає активностей сьогодні;
- немає плану дня;
- недостатньо `#day_focus` у плані дня;
- активний `CRISIS` і немає focus contexts;
- трекер використовується надто мало за 7 днів;
- фокус за останні 5h/24h;
- низький рух вчора.

Денні правила:
- якщо причина повторилась у новий день, інсайт з тим самим `id` знову стає `unread`.

Dashboard (quick unread):
- показуються непрочитані;
- swipe позначає як прочитане і одразу прибирає картку локально.

## 8. Тести

Parser unit tests:
- `app/src/test/java/com/romankozak/forwardappmobile/domain/userawareness/StateSlashCommandParserTest.kt`

Integration tests:
- `UserAwarenessRepositoryTest.kt`
- `ActivityRepositoryUserAwarenessIntegrationTest.kt`

Покривають:
- default `NORMAL`;
- закриття/відкриття інтервалів;
- idempotency;
- зміну рівня кризи;
- парсинг/очищення тексту;
- застосування стану через activity.

## 9. Поточні обмеження

- Немає жорсткого DB-constraint на “рівно один активний інтервал стану” (гарантується транзакційною логікою).
- `getTrackedMinutesByContextAndState` використовує апроксимацію по `startTime`, без точного overlap по інтервалах стану.
- `NudgePolicy` і `QuotaPolicy` вже реалізовані в домені, але застосовані не у всіх UX-потоках (частково через AI Insights / weekly analytics).
- `SUGGESTED` source передбачений моделлю, але основний сценарій зараз manual/slash-driven.

## 10. Висновок

Підсистема обізнаності стану і реактивної аналітики вже покриває повний базовий цикл:
- зміна стану з вільного тексту;
- історія інтервалів стану;
- аудит у трекері;
- аналітичні зрізи;
- UX-реакції (badge, quick switch, AI insights, turbulent-week).

Для next step найцінніше:
- перейти з v1-апроксимації на точний overlap-розподіл часу по інтервалах стану;
- ширше підключити `NudgePolicy/QuotaPolicy` у планування та нагадування.
