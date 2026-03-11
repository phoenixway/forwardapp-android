# Огляд UI/UX головного екрану, вкладок і підекранів

Дата: 2026-03-11

## Мета

Цей звіт фіксує поточний стан головного екрану додатка, його вкладок, підекранів вкладок і ключових UX-проблем, знайдених під час огляду реальної Compose-реалізації.

Огляд побудовано по коду навігації та composable-екранах, без live-сесії застосунку.

## Карта основних екранів

### 1. Головний екран `Command Deck`

Верхній рівень містить 6 вкладок:

- `Dashboard`
- `Today`
- `Tactics`
- `Strategic Arc`
- `Strategy`
- `Core`

Реалізація:

- `app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/MainScreenLayout.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/CommandDeckComponents.kt`

### 2. Вкладка `Dashboard`

Поточний зміст:

- блок `Фокус-контексти`
- блок `AI Insights`

Екран:

- `app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/DashboardContent.kt`

### 3. Вкладка `Today`

Передбачені підекрани:

- `TRACK`
- `PLAN`
- `DASHBOARD`
- `ANALYTICS`

Екрани:

- `app/src/main/java/com/romankozak/forwardappmobile/features/daymanagement/ui/DayManagementScreen.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/daymanagement/ui/DayManagementTab.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/daymanagement/ui/dayplan/components/DayManagementBottomNav.kt`

### 4. Вкладка `Tactics`

Поточна структура:

- список місій
- multi-select режим
- bulk actions для статусів
- редагування місії
- action menu
- scope links

Екран:

- `app/src/main/java/com/romankozak/forwardappmobile/features/missions/presentation/TacticalManagementScreen.kt`

### 5. Вкладка `Strategic Arc`

Поточний зміст:

- список зв'язків через `ConnectionsPanel`
- reorder
- додавання контекстів, вкладень, URL, Obsidian
- scope links bottom sheet

Екран:

- `app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/StrategicArcScreen.kt`

### 6. Вкладка `Strategy`

Передбачені підекрани:

- `DASHBOARD`
- `AI_INSIGHTS`
- `AI_CHAT`

Екрани:

- `app/src/main/java/com/romankozak/forwardappmobile/features/strategicmanagement/StrategicManagementScreen.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/strategicmanagement/StrategicManagementBottomNav.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/strategicmanagement/StrategicManagementTab.kt`

### 7. Вкладка `Core`

Поточний зміст:

- список зв'язків через `ConnectionsPanel`
- reorder
- додавання контекстів, вкладень, URL, Obsidian
- scope links bottom sheet

Екран:

- `app/src/main/java/com/romankozak/forwardappmobile/features/mainscreen/CoreLevelScreen.kt`

### 8. Стек `Contexts`

Окремий великий стек із:

- `Context Hierarchy`
- `Context Detail`
- локальним пошуком
- recent lists
- focus mode
- діалогами створення
- capability-driven view modes

Основні файли:

- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_hierarchy_screen/ContextHierarchyScreen.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_hierarchy_screen/components/MainScreenScaffold.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreen.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenContent.kt`

У `Context Detail` зараз є такі режими:

- `BACKLOG`
- `INBOX`
- `ADVANCED`
- `CONNECTIONS`
- `DIRECTION`
- `DASHBOARD`
- `LOG`
- `ARTIFACT`
- `KEY_PROBLEMS`
- `NOTES`
- `VET_CASE`

### 9. `Settings`

Підекрани:

- `General`
- `Ui`
- `Reminders`
- `Management`
- `Integrations`
- `Ai`
- `Experiments`
- `Diagnostics`

Екран:

- `app/src/main/java/com/romankozak/forwardappmobile/features/settings/settings/SettingsScreen.kt`

## Основні UX findings

### 1. Підекрани `Today` фактично не відкриваються як повноцінна навігація

Проблема:

- `DayManagementScreen` стартує з `PLAN`
- `HorizontalPager` має `userScrollEnabled = false`
- `DayManagementBottomNav` існує окремо, але не підключений до екрана

Наслідок:

- `TRACK`, `DASHBOARD`, `ANALYTICS` існують технічно, але для користувача майже недоступні
- вкладка `Today` виглядає як один екран замість системи денного управління

### 2. Підекрани `Strategy` теж існують у коді, але не мають явного перемикача

Проблема:

- `StrategicManagementViewModel` тримає `currentTab`
- `StrategicManagementBottomNav` існує
- але сам `StrategicManagementScreen` не рендерить цей bottom nav

Наслідок:

- AI Insights і AI Chat фактично сховані
- користувач бачить лише dashboard-режим

### 3. Верхні вкладки `Command Deck` мають низьку discoverability

Проблема:

- у верхньому tab row рендеряться майже лише символи
- назви вкладок не показуються явно

Наслідок:

- новому користувачу важко зрозуміти різницю між `Core`, `Strategy`, `Strategic Arc`, `Tactics`
- інтерфейс виглядає стильним, але не самоочевидним

### 4. Нижній бар `Command Deck` змішує навігацію, швидкі дії і службові команди

Проблема:

- основні кнопки в барі не мають видимих текстових підписів
- багато важливих функцій винесено в `More`
- в `More` лежать різнорідні сутності: settings, about, import/export, attachments, scripts, AI, reminders

Наслідок:

- нижній бар не читається як чітка навігація
- частина важливих сценаріїв ховається на другий або третій клік
- користувачеві важко сформувати стабільну ментальну модель

### 5. `Context Detail` надто перевантажений нижнім action/input шаром

Проблема:

- нижній шар одночасно виконує роль:
- input panel
- перемикача режимів
- меню операцій
- quick actions
- навігації назад/вперед
- jump у hierarchy
- focus toggle
- export/import дій

Наслідок:

- високий cognitive load
- складно навчитися користуванню без попереднього знання системи
- низька discoverability рідкісних, але важливих дій

### 6. Частина режимів `Context Detail` лишається незавершеною

Проблема:

- `NOTES` і `VET_CASE` віддають placeholder `Coming Soon`

Наслідок:

- якщо ці режими видно у продуктовому UI, це створює відчуття незавершеності
- знижується довіра до системної цілісності екрана

### 7. Мова інтерфейсу змішана

Проблема:

- частина інтерфейсу англійською
- частина українською
- це стосується і головної навігації, і settings, і sheet-меню

Наслідок:

- продукт виглядає як внутрішній prototype build, а не як зібрана UX-система

### 8. `Core`, `Strategic Arc` і частково `Strategy` мають занадто схожу взаємодію

Проблема:

- кілька вкладок побудовано навколо того самого патерну `ConnectionsPanel`
- відмінності між стратегічними рівнями не підсилені окремою структурою, щільністю інформації або власними primary actions

Наслідок:

- користувач не відчуває, чим саме відрізняються рівні моделі
- різниця існує в термінах, але не в UX-сценарії

### 9. `Settings` перевантажений як продуктова, dev і experimental панель одночасно

Проблема:

- в одному екрані зібрано користувацькі налаштування, інтеграції, AI-конфіг, експерименти і debug actions

Наслідок:

- складно знайти базові налаштування
- екран виглядає як технічний control panel, а не як user-facing settings

## Що варто покращити

### Пріоритет 1. Навігаційна ясність

- Дати всім головним вкладкам `Command Deck` видимі текстові назви, не лише символи.
- У `Today` відновити або явно підключити навігацію між `Track / Plan / Dashboard / Analytics`.
- У `Strategy` відобразити реальний перемикач між `Dashboard / AI Insights / AI Chat`.
- Відділити навігаційні елементи від action-команд у нижніх панелях.

### Пріоритет 2. Discoverability

- Додати видимі підписи до нижніх кнопок `Command Deck`.
- Скоротити `More` до справді другорядних функцій.
- Винести найчастіші дії у прямий доступ першого рівня.

### Пріоритет 3. Структурна диференціація вкладок

- Для `Core`, `Strategic Arc`, `Strategy`, `Tactics` дати різні інформаційні моделі, а не лише різні заголовки.
- `Core` має бути більш стабільним і опорним.
- `Strategic Arc` має показувати довгу траєкторію або ланцюг стратегічних напрямків.
- `Strategy` має бути decision-oriented з аналітикою і AI.
- `Tactics` має бути execution-oriented з короткими діями та станами.

### Пріоритет 4. Спрощення `Context Detail`

- Розбити нижній control layer на:
- view switcher
- основний input
- secondary actions

- Меню рідкісних дій винести в окремий screen menu.
- Найчастіші операції лишити завжди видимими.

### Пріоритет 5. Прибрати або сховати незавершене

- Не показувати `Coming Soon` режими у production UX, якщо вони не готові.
- Сховати debug/diagnostic дії поза developer build.
- Experimental toggles або винести в окремий dev-розділ, або заховати за developer mode.

### Пріоритет 6. Мовна цілісність

- Обрати одну основну мову інтерфейсу.
- Уніфікувати назви вкладок, sheet-меню, settings, підказки та action labels.

## Рекомендований порядок UX-рефакторингу

1. Виправити недоступні підекрани `Today` і `Strategy`.
2. Переробити верхній і нижній navigation layer `Command Deck`.
3. Спрощувати `Context Detail` bottom panel.
4. Відділити user settings від dev/experimental settings.
5. Уніфікувати мову та назви сценаріїв.
6. Лише після цього робити візуальний polish.

## Висновок

Поточний стан додатка показує сильну функціональну глибину і багаторівневу модель роботи, але UX поки що більше схожий на power-user internal system, ніж на добре зібраний продукт.

Найбільша проблема зараз не нестача фіч, а слабка видимість уже наявних можливостей, перевантаження control-елементів і розрив між реальною архітектурою екранів та тим, як користувач може до них дістатися.
