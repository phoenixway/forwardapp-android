# AI Control Layer — архітектура та онбординг для подій/станів

## Що це

AI Control Layer — локальний рівень керування поведінкою додатка без залежності від LLM. Складається з:

- **Event Intelligence Layer** — поток незворотних фактів (подій)
- **State Inference** — обчислення `LifeSystemState` з подій
- **Policy Engine** — правила, що перетворюють стан на рішення
- **Actuators** — виконання рішень (адаптація UI, планування воркерів тощо)
- **AiInsights** — мінімальні повідомлення/рекомендації на основі даних
- **LLM Advisor** (опційно) — пояснення/текст, не впливає на стан

## Поточний стан реалізації

- Room таблиці: `ai_events`, `life_system_state`, `ai_insights`
- Репозиторії:
  - `AiEventRepository` — запис/читання подій
  - `LifeSystemStateRepository` — збереження останнього стану
  - `AiInsightRepository` — збереження/читання інсайтів
- Інференсер: `DeterministicLifeStateInferencer` (rolling windows + гистерезис)
- Політики: Overload, Stuck, Entropy → `AiDecision`
- Актюатори: UI/Recommendation/Worker (поки логують/ставлять заглушку worker)
- Контролер: `AiControlEngine.tick()` — events → state → decisions → actuators
- AiInsightsScreen: показує прості рекомендації з трекера (немає активностей сьогодні, низький xp учора, або мотивація). Інсайти зберігаються в БД, можна помітити прочитаним/видалити/очистити.
- Навігація: AI Insights доступний з Command Deck → More.

## Як додати нову подію

1. **Модель**: додай payload у `domain/ai/events/AiEvent.kt`. Використовуй числа/enums/короткі строки, timestamp — `Instant`.
2. **Джерело**: тригер *лише* в репозиторії/сервісі, що фіксує факт (НЕ ViewModel/Compose).
   - Activity: `ActivityRepository` після вставки/оновлення
   - Tasks: `DayManagementRepository` при створенні/завершенні/відкладанні
   - Projects: `ProjectRepository` при створенні/активації
   - Notes: `NoteDocumentRepository` при збереженні system note
   - Navigation/Idle: у відповідних трекерах (додати listener/observer)
3. **Запис**: інжект `AiEventRepository` у репозиторій і викликай `emit(event)`.
4. **Міграції**: якщо додаєш нову таблицю — збільшити версію БД, додати `MIGRATION_X_Y`, підключити в `DatabaseModule`.
5. **Budget/Discipline**:
- Подія — рідкісний незворотний факт, не UI-шум.
- 1 факт → 1 подія.
- Не текстові wall-of-text payloads; лише сухі цифри/enum.
- Не стріляти часто: агрегуй перед emit.

### Де саме тригерити (коротка шпаргалка)
- **Activity**: `ActivityRepository` після запису/оновлення → `ActivityLoggedEvent/ActivityFinishedEvent/ActivityOngoingTickEvent`
- **Tasks/Projects**: відповідні репозиторії (створення/завершення/перенесення/активація) → `TaskCreated/TaskCompleted/TaskDeferred/ProjectActivated`
- **System notes**: `NoteDocumentRepository.save()` для `my-life-current-state` → `SystemNoteUpdatedEvent`
- **Navigation/Idle**: NavController listener / presence tracker → `ScreenVisitedEvent`, `IdleDetectedEvent`, `FocusResumedEvent`
- **Life state/Policy**: після оновлення стану/застосування політики → `LifeStateUpdatedEvent`, `RecommendationAccepted/Ignored`
- **Background**: WorkManager tick → `BackgroundAnalysisTickEvent`

❌ Не тригерити з UI/ViewModel/Compose, не з LLM-відповідей.

## Як додати нове правило/політику

1. Створи клас, що реалізує `AiPolicy`.
2. Оціни `LifeSystemState` → поверни список `AiDecision`.
3. Додай @Binds @IntoSet у `AiBindingModule`.
4. За потреби додай актюатор, який виконує новий тип рішення.

## Як підключити актюацію

- `AiActuator.apply(decision)` — виконує рішення.
- Додай власний актюатор у `AiBindingModule` через @IntoSet.
- Для UI-адаптацій поки лише логування; можна перевести на shared settings/feature flags.

## Як використовувати AiControlEngine

Викликай `aiControlEngine.tick()`:
- після запису важливої події (наприклад, завершення активності/задачі)
- у періодичному воркері

tick: читає події з останнього оновлення стану → обчислює стан → політики → актюатори.

## Мінімальний обов’язковий набір подій

- ActivityLoggedEvent / ActivityFinishedEvent
- TaskCompletedEvent / TaskDeferredEvent
- SystemNoteUpdatedEvent (my-life-current-state)
- IdleDetectedEvent / FocusResumedEvent
- LifeStateUpdatedEvent

Цього достатньо, щоб порахувати load/entropy/stuck і генерувати прості інтервенції.

## Як додати UI-реакцію через інсайти

- Сформуй список `AiInsightEntity` (id, text, type, timestamp, isRead) у своєму генераторі та запиши через `AiInsightRepository.upsertInsights`.
- AiInsightsScreen автоматично підхопить зміни (Flow з БД).
- Типи карток: MOTIVATION/INFO/WARNING/ERROR, показує час, читано/не читано, кнопки “прочитано”, “видалити”, “очистити всі”.

## Де не тригерити події

- НЕ в UI/Compose
- НЕ на кожен tap/scroll/символ
- НЕ з LLM-відповідей (LLM — advisor, не факт)

## Швидка пам’ятка (перед новою подією)

| Питання | Так/Ні |
| --- | --- |
| Це незворотний факт? |  |
| Змінився реальний стан/намір? |  |
| Впливає на LifeSystemState? |  |
| Не UI-дірібниця? |  |
| Payload сухий (цифри/enum)? |  |
| Не шлеся частіше, ніж треба? |  |

Якщо не всі “так” — подію не додаємо.
