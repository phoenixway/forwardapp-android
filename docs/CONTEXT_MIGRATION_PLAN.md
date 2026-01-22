# План міграції сутності Context

Цей документ описує план переходу від монолітної сутності `features.contexts.data.models.Context` до нової, гнучкої, поліморфної моделі, заснованої на ролях та можливостях (`core.context.Context`).

## Крок 1: Таблиця відповідності полів

Нижче наведено детальне зіставлення кожного поля старої сутності з його пропонованим аналогом у новій архітектурі.

| Старе поле (`Context`)         | Тип даних             | Призначення                                       | Нова модель (пропозиція)                                                              | Нотатки                                                                                                                              |
| ------------------------------ | --------------------- | ------------------------------------------------- | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **Основні**                    |                       |                                                   |                                                                                       |                                                                                                                                      |
| `id`                           | `String`              | Унікальний ідентифікатор                          | `id: ContextId` (ключове поле `core.context.Context`)                                 | Використовувати value class `ContextId` для типізації.                                                                               |
| `name`                         | `String`              | Назва сутності                                    | Властивість у `IdentityCapability`                                                    | Створити `IdentityCapability` для базової інформації (назва, опис).                                                                  |
| `description`                  | `String?`             | Детальний опис                                    | Властивість у `IdentityCapability`                                                    |                                                                                                                                      |
| `roleCode`                     | `String?`             | Код ролі (прямий аналог)                          | `role: ContextRole` (ключове поле `core.context.Context`)                             | Це поле є прямим містком між двома моделями.                                                                                         |
| `projectType`                  | `ContextType`         | Тип (`DEFAULT`, `RESERVED`, `SYSTEM`)             | `role: ContextRole`                                                                   | `ContextType.SYSTEM` та `ContextType.RESERVED` стануть окремими `ContextRole` з відповідними можливостями.                           |
| **Ієрархія та порядок**        |                       |                                                   |                                                                                       |                                                                                                                                      |
| `parentId`                     | `String?`             | ID батьківської сутності                          | Властивість у `HierarchyCapability`                                                   | Створити `HierarchyCapability` для управління вкладеністю.                                                                           |
| `order`                        | `Long`                | Порядок сортування                                | Властивість у `HierarchyCapability`                                                   |                                                                                                                                      |
| **Метадані та аудит**          |                       |                                                   |                                                                                       |                                                                                                                                      |
| `createdAt`, `updatedAt`       | `Long` / `Long?`      | Час створення та оновлення                        | Властивості у `AuditingCapability`                                                    | Створити `AuditingCapability` для відстеження життєвого циклу.                                                                       |
| `syncedAt`, `version`          | `Long?` / `Long`      | Час синхронізації та версія                       | Властивості у `SyncCapability`                                                        | Можна об'єднати з `AuditingCapability` або винести в окрему `SyncCapability`.                                                          |
| `isDeleted`                    | `Boolean`             | Прапорець м'якого видалення                       | Не є можливістю. Це має оброблятися на рівні репозиторію.                             | Логіка видалення не повинна бути частиною бізнес-моделі контексту.                                                                    |
| **Зв'язки та теги**            |                       |                                                   |                                                                                       |                                                                                                                                      |
| `tags`                         | `List<String>?`       | Список тегів                                      | Властивість у `TaggingCapability`                                                     | Створити `TaggingCapability`.                                                                                                        |
| `relatedLinks`                 | `List<RelatedLink>?`  | Пов'язані посилання                               | Властивість у `LinkingCapability`                                                     | Створити `LinkingCapability`.                                                                                                        |
| **Стан та статус**             |                       |                                                   |                                                                                       |                                                                                                                                      |
| `isCompleted`                  | `Boolean`             | Чи завершено контекст (завдання)                  | Властивість у `CompletableCapability`                                                 | Створити `CompletableCapability` для сутностей, які можна "завершити".                                                               |
| `projectStatus`                | `String?`             | Статус (`NO_PLAN`, `IN_PROGRESS`...)              | Властивість у `ProjectManagementCapability`                                           |                                                                                                                                      |
| `projectStatusText`            | `String?`             | Текстовий опис статусу                            | Властивість у `ProjectManagementCapability`                                           |                                                                                                                                      |
| **Проектний менеджмент**       |                       |                                                   |                                                                                       |                                                                                                                                      |
| `isProjectManagementEnabled`   | `Boolean?`            | Чи ввімкнено функції ПМ                           | `config.activeCapabilities.contains(ProjectManagementCapability.ID)`                | Активація `ProjectManagementCapability` в конфігурації контексту.                                                                    |
| `totalTimeSpentMinutes`        | `Long?`               | Загальний витрачений час                          | Властивість у `TimeTrackingCapability`                                                | Можна винести в окрему `TimeTrackingCapability`.                                                                                     |
| **Оцінка та скоринг**          |                       |                                                   |                                                                                       |                                                                                                                                      |
| `valueImportance`, `effort`... | `Float`               | Поля для скорингу (ICE, RICE)                     | Властивості у `ScoringCapability`                                                     | Усі 12 полів, пов'язаних з оцінкою, переїжджають до `ScoringCapability`.                                                              |
| `scoringStatus`                | `String`              | Статус оцінки                                     | Властивість у `ScoringCapability`                                                     |                                                                                                                                      |
| **Стан UI (не переносити)**    |                       |                                                   |                                                                                       |                                                                                                                                      |
| `isExpanded`                   | `Boolean`             | Розгорнутий вузол у ієрархії                      | **Видалити з моделі.** Має керуватися ViewModel або UI-стейтом.                       | Ця логіка не є частиною даних, а лише їх відображення.                                                                                |
| `isAttachmentsExpanded`        | `Boolean`             | Розгорнутий список вкладень                       | **Видалити з моделі.**                                                               | Аналогічно до `isExpanded`.                                                                                                          |
| `showCheckboxes`               | `Boolean`             | Показувати чекбокси для підзадач                  | **Видалити з моделі.** Це стан UI.                                                    |                                                                                                                                      |
| `defaultViewModeName`          | `String?`             | Вигляд за замовчуванням                           | `role.startView` або `config.currentView`                                             | Нова модель вже має цю концепцію в `ContextRole` та `ContextConfiguration`.                                                            |

## Крок 2: Пропонована структура нових сутностей

### Базові `ContextRole`

Потрібно буде визначити як мінімум такі ролі:

-   `generic_context`: Базова роль для звичайних проектів/контекстів.
-   `system_inbox`: Роль для системного проекту "Inbox".
-   `system_today`: Роль для системного проекту "Today".
-   `task`: Роль для простого завдання (якщо потрібно).
-   `note_collection`: Роль для сутності, що є просто колекцією нотаток.

### Базові `Capability`

На основі таблиці, потрібно створити такі "можливості":

-   `IdentityCapability`: `name`, `description`.
-   `HierarchyCapability`: `parentId`, `order`.
-   `AuditingCapability`: `createdAt`, `updatedAt`.
-   `SyncCapability`: `version`, `syncedAt`.
-   `TaggingCapability`: `tags`.
-   `LinkingCapability`: `relatedLinks`.
-   `CompletableCapability`: `isCompleted`, `completedAt`.
-
-   `ProjectManagementCapability`: `projectStatus`, `projectStatusText`.
-   `ScoringCapability`: Усі поля, пов'язані з оцінкою.
-   `TimeTrackingCapability`: `totalTimeSpentMinutes`.

---

Цей документ є відправною точкою. Тепер, маючи цю структуру, наступний крок — реалізувати базові класи для `Capability` та `ContextRole` і написати адаптер для перетворення старих об'єктів на нові.
