# План відновлення ProjectScreen

**Коротке резюме:**
Цей план замінює попередній і фокусується на відновленні `ProjectScreen`. Ми перенесемо UI та ViewModel з `dev` гілки, адаптуємо їх до KMP/SQLDelight архітектури та налаштуємо DI за допомогою `kotlin.inject`.

**Припущення:**
*   Новий шар даних (KMP+SQLDelight) функціонує.
*   DI-компонент (`AppComponent`) налаштований для `kotlin.inject`.
*   UI-компоненти з `dev` гілки сумісні з поточною версією Compose.
*   Сутності `Project` та `RelatedLink` у новому шарі даних мають структуру, подібну до версії з `dev`.

**Покроковий план:**

1.  **Створення `ProjectScreenViewModel.kt`:**
    *   Створити файл `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/ProjectScreenViewModel.kt`.
    *   Скопіювати вміст з `dev` версії.
    *   Адаптувати `package` та базові імпорти.
    *   Замінити залежності від Room-репозиторіїв на інтерфейси нових KMP-репозиторіїв.
    *   Анотувати конструктор ViewModel для `kotlin.inject`.

2.  **Створення `ProjectScreen.kt`:**
    *   Створити файл `apps/android/src/main/java/com/romankozak/forwardappmobile/features/projectscreen/ProjectScreen.kt`.
    *   Скопіювати вміст Composable-функцій з `dev` версії.
    *   Адаптувати імпорти та виправити помилки компіляції, пов'язані з UI.

3.  **Налаштування DI через `kotlin.inject`:**
    *   Створити `features/projectscreen/di/ProjectScreenModule.kt`.
    *   Додати в нього `provideProjectScreenViewModel`.
    *   Підключити `ProjectScreenModule` до `AppComponent.kt`.

4.  **Адаптація ViewModel до нового Data Layer:**
    *   Проаналізувати логіку `ProjectScreenViewModel` та замінити всі виклики до старого репозиторію на нові.
    *   Реалізувати мапінг між SQLDelight-сутністями та UI-моделями, якщо вони відрізняються.
    *   Переконатися, що `Flow` з нового репозиторію коректно обробляється.

5.  **Інтеграція UI та ViewModel:**
    *   У `ProjectScreen.kt` отримувати ViewModel через `remember { LocalAppComponent.current.projectScreenViewModel }`.
    *   Підключити UI до `StateFlow` та івентів з адаптованої ViewModel.
    *   Виправити всі помилки, пов'язані з несумісністю даних.

6.  **Компіляція та тестування:**
    *   Запустити `make check-compile` для перевірки компіляції.
    *   Запустити додаток і перевірити, що `ProjectScreen` відкривається та коректно відображає дані.