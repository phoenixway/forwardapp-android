# План міграції сутності Context

Цей документ описує план переходу від монолітної сутності `features.contexts.data.models.Context` до нової, гнучкої, поліморфної моделі, заснованої на ролях та можливостях (`core.context.Context`).

## Крок 1: Таблиця відповідності полів

| Старе поле (`Context`) | Тип даних | Призначення | Нова модель (пропозиція) | Нотатки |
| --- | --- | --- | --- | --- |
| **Основні** | | | | |
| `id` | `String` | Унікальний ідентифікатор | `id: ContextId` (ключове поле `core.context.Context`) | Використовувати value class `ContextId` для типізації. |
| `name` | `String` | Назва сутності | `label: String` (ключове поле `core.context.Context`) | Поле `name` зі старої моделі перейменовується в `label` в новій. |
| `description` | `String?` | Детальний опис | `description: String?` (ключове поле `core.context.Context`) | |
| `roleCode` | `String?` | Код ролі (прямий аналог) | `role: ContextRole` (ключове поле `core.context.Context`) | Це поле є прямим містком між двома моделями. |
| `projectType` | `ContextType` | Тип (`DEFAULT`, `RESERVED`, `SYSTEM`) | `role: ContextRole` | `ContextType.SYSTEM` та `ContextType.RESERVED` стануть окремими `ContextRole` з відповідними можливостями. |
| **Ієрархія та порядок** | | | | |
| `parentId` | `String?` | ID батьківської сутності | Властивість у `HierarchyCapability` | Створити `HierarchyCapability` для управління вкладеністю. |
| `order` | `Long` | Порядок сортування | Властивість у `HierarchyCapability` | |
| **Метадані та аудит** | | | | |
| `createdAt`, `updatedAt` | `Long` / `Long?` | Час створення та оновлення | Властивості у `AuditingCapability` | Створити `AuditingCapability` для відстеження життєвого циклу. |
| `syncedAt`, `version` | `Long?` / `Long` | Час синхронізації та версія | Властивості у `SyncCapability` | Можна об'єднати з `AuditingCapability` або винести в окрему `SyncCapability`. |
| `isDeleted` | `Boolean` | Прапорець м'якого видалення | Не є можливістю. Це має оброблятися на рівні репозиторію. | Логіка видалення не повинна бути частиною бізнес-моделі контексту. |
| **Зв'язки та теги** | | | | |
| `tags` | `List<String>?` | Список тегів | Окрема таблиця зв'язку `context_tags` (Many-to-Many) | Для ефективної фільтрації та запитів теги будуть винесені в реляційну структуру. |
| `relatedLinks` | `List<RelatedLink>?` | Пов'язані посилання | Властивість у `LinkingCapability` | Створити `LinkingCapability`. |
| **Стан та статус** | | | | |
| `isCompleted` | `Boolean` | Чи завершено контекст (завдання) | Окрема колонка в таблиці Context | Важливо для фільтрації, тому буде окремою індексованою колонкою. |
| `projectStatus` | `String?` | Статус (`NO_PLAN`, `IN_PROGRESS`...) | Окрема колонка в таблиці Context | Важливо для фільтрації, тому буде окремою індексованою колонкою. |
| `projectStatusText` | `String?` | Текстовий опис статусу | Окрема колонка в таблиці Context | |
| **Проектний менеджмент** | | | | |
| `isProjectManagementEnabled`| `Boolean?` | Чи ввімкнено функції ПМ | `config.activeCapabilities.contains(ProjectManagementCapability.ID)` | Активація `ProjectManagementCapability` в конфігурації контексту. |
| `totalTimeSpentMinutes` | `Long?` | Загальний витрачений час | Властивість у `TimeTrackingCapability` | Можна винести в окрему `TimeTrackingCapability`. |
| **Оцінка та скоринг** | | | | |
| `valueImportance`, `effort`... | `Float` | Поля для скорингу (ICE, RICE) | Окремі колонки в таблиці Context | Ці поля є критичними для запитів і фільтрації, тому будуть окремими колонками БД. |
| `scoringStatus` | `String` | Статус оцінки | Окрема колонка в таблиці Context | Критично для запитів і фільтрації. |
| **Стан UI (не переносити)** | | | | |
| `isExpanded` | `Boolean` | Розгорнутий вузол у ієрархії | **Видалити з моделі.** Має керуватися ViewModel або UI-стейтом. | Ця логіка не є частиною даних, а лише їх відображення. |
| `isAttachmentsExpanded` | `Boolean` | Розгорнутий список вкладень | **Видалити з моделі.** | Аналогічно до `isExpanded`. |
| `showCheckboxes` | `Boolean` | Показувати чекбокси для підзадач | **Видалити з моделі.** Це стан UI. | |
| `defaultViewModeName` | `String?` | Вигляд за замовчуванням | `role.startView` або `config.currentView` | Нова модель вже має цю концепцію в `ContextRole` та `ContextConfiguration`. |

## Крок 2: Пропонована структура нових сутностей

**Примітка щодо зберігання даних (Гібридний підхід):**
Для реалізації нової архітектури буде застосовано гібридний підхід до зберігання даних. Поля, за якими потрібні часті запити, фільтрація або індексація (наприклад, `id`, `label`, `description`, `parentId`, поля `ScoringCapability`, поля `ProjectManagementCapability`, а також `tags` через окрему таблицю), будуть зберігатися як окремі, індексовані колонки або таблиці. Дані менш критичних для запитів, але більш гнучких "можливостей" (наприклад, `AuditingCapability`, `LinkingCapability`) будуть серіалізовані в єдине JSON-поле (`properties`) в тій же таблиці. Це забезпечить оптимальний баланс між продуктивністю та гнучкістю схеми.


### Перелік полів, що будуть окремими колонками БД:
-   `id` (`ContextId`)
-   `label` (для відображення назви)
-   `description`
-   `roleCode`
-   `parentId`
-   `order`
-   `tags` (реалізовано через окрему таблицю зв'язку `context_tags`)
-   `valueImportance`, `valueImpact`, `effort`, `cost`, `risk`
-   `weightEffort`, `weightCost`, `weightRisk`
-   `rawScore`, `displayScore`
-   `scoringStatus`
-   `isCompleted`
-   `projectStatus`
-   `projectStatusText`

### Базові `ContextRole`

- `generic_context`
- `system_inbox`
- `system_today`
- `task`
- `note_collection`

### Базові `Capability`

- `HierarchyCapability`
- `AuditingCapability`
- `SyncCapability`
- `TaggingCapability`
- `LinkingCapability`
- `ProjectManagementCapability`
- `ScoringCapability`
- `TimeTrackingCapability`
