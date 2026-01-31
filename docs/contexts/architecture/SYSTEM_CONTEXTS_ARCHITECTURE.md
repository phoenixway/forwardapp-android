# Архітектура Системних Контекстів

Дата: 31 січня 2026

Цей документ описує архітектуру, принципи роботи та процес модифікації системних контекстів (System Contexts) у додатку.

## 1. Огляд

Системні контексти — це набір спеціальних, попередньо визначених контекстів (раніше "проектів"), які є невід'ємною частиною базової функціональності програми. Прикладами є `Inbox`, `Today`, `Strategic Review` тощо.

На відміну від звичайних контекстів, які створюються користувачем, системні контексти мають бути завжди присутніми в базі даних для коректної роботи багатьох механізмів.

## 2. Основні принципи

1.  **Ідентифікація за ID**: Кожен системний контекст має унікальний, жорстко закодований `ID` (напр., `inbox_context_id`). Цей ID ніколи не змінюється.
2.  **Неможливість видалення**: Користувач не може видалити системний контекст. Спроба видалення блокується на рівні репозиторію.
3.  **Гарантоване існування**: Система автоматично відновлює відсутні системні контексти при запуску програми, після відновлення з бекапу або після синхронізації.

## 3. Ключові компоненти (Файл за файлом)

Нижче наведено опис ключових файлів, що реалізують механізм системних контекстів.

### 3.1. `app/src/main/java/com/romankozak/forwardappmobile/core/context/SystemContexts.kt`

-   **Роль**: **Центральне джерело правди**. Тут визначаються всі системні контексти.
-   **Структура**:
    -   `enum class ContextId`: Перелік, що містить `ID` всіх системних контекстів.
    -   `object SystemContexts`: Містить константи для кожного системного контексту (напр., `INBOX`) та список `ALL`, що об'єднує їх.
    -   `fun isSystem(id: String)`: Функція для перевірки, чи є переданий `ID` системним.

### 3.2. `core-data-interfaces/src/main/java/com/romankozak/forwardappmobile/core/data/interfaces/SystemContextEnsurer.kt`

-   **Роль**: Абстракція (інтерфейс) для механізму гарантування існування системних контекстів.
-   **Призначення**: Створений для розриву циклічних залежностей між модулями `app` та `sync` згідно з принципом інверсії залежностей (DIP).
-   **Структура**:
    -   `interface SystemContextEnsurer`: Містить єдиний метод `ensureAllSystemContextsExist()`.

### 3.3. `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/DatabaseInitializer.kt`

-   **Роль**: Конкретна реалізація `SystemContextEnsurer`. Відповідає за початкове створення системних контекстів у базі даних.
-   **Механізм**:
    -   Реалізує `SystemContextEnsurer.ensureAllSystemContextsExist()`.
    -   Усередині цього методу викликається `prePopulateProjects`, який послідовно створює кожен системний контекст за допомогою `ensureProjectExists`.
    -   `ensureProjectExists` перевіряє наявність контексту за `ID` і, якщо він відсутній, створює новий запис у базі даних.

### 3.4. `app/src/main/java/com/romankozak/forwardappmobile/data/repository/ContextRepository.kt`

-   **Роль**: Захист системних контекстів від видалення.
-   **Механізм**:
    -   У методі `deleteContextsAndSubContexts` перед виконанням м'якого видалення (soft delete) відбувається фільтрація списку контекстів.
    -   Усі контексти, для яких `SystemContexts.isSystem()` повертає `true`, виключаються зі списку на видалення.

### 3.5. `app/src/main/java/com/romankozak/forwardappmobile/data/sync/FullBackupLocalDataSourceImpl.kt`

-   **Роль**: Гарантування наявності системних контекстів після відновлення з повного бекапу.
-   **Механізм**:
    -   Клас інжектує інтерфейс `SystemContextEnsurer`.
    -   У методі `restoreDatabaseFromBackup`, після повного очищення таблиць (`clearAllTables()`) та вставки даних з бекапу, **в кінці транзакції** викликається `systemContextEnsurer.ensureAllSystemContextsExist()`.
    -   Це гарантує, що навіть якщо бекап був пошкоджений або не містив системних контекстів, вони будуть створені заново.

### 3.6. `sync/src/syncOn/java/com/romankozak/forwardappmobile/sync/SyncRepository.kt`

-   **Роль**: Гарантування наявності системних контекстів після операцій синхронізації.
-   **Механізм**:
    -   Репозиторій також інжектує `SystemContextEnsurer`.
    -   У методах `applyServerChanges` та `importSelectedData` після застосування змін з сервера/файлу викликається `systemContextEnsurer.ensureAllSystemContextsExist()`.

## 4. Як додати або змінити системний контекст

Процес модифікації вимагає уважності та послідовності.

### 4.1. Крок 1: Додавання ID

-   **Файл**: `app/src/main/java/com/romankozak/forwardappmobile/core/context/SystemContexts.kt`
-   **Дія**: Додайте новий унікальний ID у `enum class ContextId`. Наприклад:
    ```kotlin
    enum class ContextId(val raw: String) {
        // ... існуючі ID
        MY_NEW_CONTEXT("my_new_context_id");
    }
    ```

### 4.2. Крок 2: Створення константи

-   **Файл**: `app/src/main/java/com/romankozak/forwardappmobile/core/context/SystemContexts.kt`
-   **Дія**: Усередині `object SystemContexts` створіть нову константу та додайте її до списку `ALL`.
    ```kotlin
    object SystemContexts {
        // ... існуючі константи
        val MY_NEW_CONTEXT = SystemContext(ContextId.MY_NEW_CONTEXT, "My New Context")

        val ALL = listOf(
            // ... існуючі
            MY_NEW_CONTEXT
        )
    }
    ```

### 4.3. Крок 3: Додавання логіки створення

-   **Файл**: `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/DatabaseInitializer.kt`
-   **Дія**: У методі `prePopulateProjects` додайте виклик `ensureProjectExists` для вашого нового контексту. Вкажіть його `ID`, ім'я за замовчуванням та `ID` батьківського контексту (якщо потрібно).
    ```kotlin
    private suspend fun prePopulateProjects(contextDao: ContextDao) {
        // ... існуючі виклики

        ensureProjectExists(
            contextDao,
            SystemContexts.MY_NEW_CONTEXT.raw,
            "my-new-context-name", // Ім'я, яке буде в базі при створенні
            parentId = someOtherContextId // напр., personalManagementProjectId
        )
    }
    ```

Після виконання цих трьох кроків новий системний контекст буде повністю інтегрований у систему. Він буде автоматично створюватися, захищатися від видалення та відновлюватися за потреби.
