# Аналіз реалізації контекстної обізнаності щодо життя та діяльності користувача

*Дата створення: 2025-03-15*

## Огляд архітектури

Контекстна обізнаність щодо життя та поточної діяльності користувача реалізована через **багаторівневу архітектуру** в AI-шарі додатка:

1. **Збір фактів** (події про активності, завдання, навігацію)
2. **Агрегація** (обчислення стану системи життя)
3. **Аналіз** (політики та інсайти)
4. **Дія** (актуатори для адаптації UI/поведінки)
5. **Відображення** (UI для користувача)

## Місця реалізації контекстної обізнаності

### 1. **Event Intelligence Layer (Шар подій)**
**Файл:** `app/src/main/java/com/romankozak/forwardappmobile/domain/ai/events/AiEvent.kt`

Цей шар фіксує незворотні факти про діяльність користувача. Кожна подія містить timestamp та специфічні дані:

- **ActivityLoggedEvent** - початок активності (тривалість, XP, anti-XP)
- **ActivityFinishedEvent** - завершення активності
- **ActivityOngoingTickEvent** - оновлення поточної активності
- **TaskCompletedEvent** - завершення завдання
- **TaskCreatedEvent** - створення завдання
- **TaskDeferredEvent** - відкладання завдання
- **ScreenVisitedEvent** - перехід між екранами
- **IdleDetectedEvent** - виявлення простою
- **FocusResumedEvent** - відновлення фокусу
- **SystemNoteUpdatedEvent** - оновлення системних нотаток
- **LifeStateUpdatedEvent** - оновлення стану життя

### 2. **State Inference (Обчислення стану)**
**Файл:** `app/src/main/java/com/romankozak/forwardappmobile/domain/ai/inference/LifeStateInferencer.kt`

Інференсер аналізує потік подій та обчислює агрегований стан системи життя:

- **LoadLevel** (LOW, NORMAL, HIGH, CRITICAL) - навантаження користувача
- **ExecutionMode** (FOCUSED, SCATTERED, STUCK) - режим виконання
- **StabilityLevel** (STABLE, UNSTABLE, FRAGMENTED) - стабільність
- **EntropyLevel** (LOW, MEDIUM, HIGH) - ентропія/хаотичність

Алгоритм використовує ковзні вікна для аналізу:
- Сума XP/anti-XP за останні години
- Хвилини простою
- Кількість подій
- Гістерезис для плавних переходів

### 3. **AI Control Engine (Координаційний двигун)**
**Файл:** `app/src/main/java/com/romankozak/forwardappmobile/domain/ai/AiControlEngine.kt`

Центральний компонент, який:
1. Отримує нові події з репозиторію
2. Викликає інференсер для обчислення нового стану
3. Зберігає стан у репозиторії
4. Застосовує політики (AiPolicy) до стану
5. Виконує рішення через актуатори (AiActuator)

### 4. **Модель стану системи життя**
**Файл:** `app/src/main/java/com/romankozak/forwardappmobile/domain/ai/state/LifeSystemState.kt`

Модель даних, що представляє агрегований контекст життя користувача на основі всіх подій.

### 5. **Репозиторії даних**
- **AiEventRepository.kt** (`app/src/main/java/com/romankozak/forwardappmobile/data/repository/AiEventRepository.kt`) - зберігання та отримання подій
- **LifeSystemStateRepository.kt** (`app/src/main/java/com/romankozak/forwardappmobile/data/repository/LifeSystemStateRepository.kt`) - зберігання та отримання стану
- **AiInsightRepository.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/ai/data/repository/AiInsightRepository.kt`) - зберігання AI-інсайтів

### 6. **Life Context Intake (Активне введення контексту)**
**Документація:** `docs/ai/Life-Context-Intake.md`

Концептуальний канал для активного введення користувачем інформації про зміни в житті без необхідності структурувати, пояснювати чи приймати рішення.

### 7. **UI-компоненти для відображення контексту**
- **LifeStateScreen.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/lifestate/LifeStateScreen.kt`) - екран стану життя
- **LifeStateViewModel.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/lifestate/LifeStateViewModel.kt`) - ViewModel для стану життя
- **LifeStateChatViewModel.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/lifestate/LifeStateChatViewModel.kt`) - чат з AI про стан життя
- **AiInsightsScreen.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/ai/insights/AiInsightsScreen.kt`) - екран AI-інсайтів

### 8. **Воркери та фонові процеси**
- **LifeStateAnalysisWorker.kt** (`app/src/main/java/com/romankozak/forwardappmobile/domain/lifestate/LifeStateAnalysisWorker.kt`) - фонове оновлення аналізу стану життя

### 9. **Моделі даних (Core Data Models)**
- **AiEventEntity.kt** (`core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/entities/ai/AiEventEntity.kt`) - сутність події для бази даних
- **LifeSystemStateEntity.kt** (`core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/entities/LifeSystemStateEntity.kt`) - сутність стану системи життя
- **AiInsightEntity.kt** (`core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/entities/ai/AiInsightEntity.kt`) - сутність AI-інсайту

### 10. **Синхронізація контексту**
- **AiEventSnapshot.kt** (`core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshots/ai/AiEventSnapshot.kt`) - снапшот події для синхронізації
- **AiInsightSnapshot.kt** (`core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshots/ai/AiInsightSnapshot.kt`) - снапшот інсайту для синхронізації
- **LifeSystemStateSnapshot.kt** (`core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshots/misc/LifeSystemStateSnapshot.kt`) - снапшот стану системи життя

### 11. **DAO (Data Access Objects)**
- **AiEventDao.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/ai/data/dao/AiEventDao.kt`) - доступ до даних подій
- **AiInsightDao.kt** (`app/src/main/java/com/romankozak/forwardappmobile/features/ai/data/dao/AiInsightDao.kt`) - доступ до даних інсайтів
- **LifeSystemStateDao.kt** (`app/src/main/java/com/romankozak/forwardappmobile/data/dao/LifeSystemStateDao.kt`) - доступ до даних стану системи життя

## Висновок

Контекстна обізнаність щодо життя та поточної діяльності користувача реалізована через **комплексну систему**, яка:

1. **Пасивно спостерігає** за діяльністю користувача через події
2. **Активно приймає** контекст через Life Context Intake
3. **Аналізує** контекст через інференсер стану
4. **Генерує інсайти** через політики AI-шару
5. **Адаптує поведінку** додатка через актуатори
6. **Відображає** контекст користувачеві через спеціальні UI-екрани

Ця система дозволяє додатку розуміти контекст користувача, адаптуватися до його стану та надавати релевантні рекомендації, що робить ForwardApp справжньою системою управління життям.

## Додаткові ресурси

- **AI Layer Documentation:** `docs/ai/ai-layer.md`
- **Life Context Intake:** `docs/ai/Life-Context-Intake.md`
- **Chat History:** `docs/ai/chat_history.md`
- **Functionality Overview:** `docs/achitecture/functionality_overview.md`

---
*Цей документ створено автоматично на основі аналізу коду та документації проекту ForwardApp.*