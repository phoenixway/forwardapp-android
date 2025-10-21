- **ЗАВЕРШЕНО:** Виправлено помилку, через яку екран редагування налаштувань цілі не закривався після збереження. Тепер екран закривається, як очікувалося.

- **РОЗПОЧАТО:** Реалізація функціоналу чекбоксів у редакторі.
  - **ЗРОБЛЕНО:** Додано рендеринг Markdown чекбоксів, кнопку на панель інструментів та обробку кліків.
  - **ПРОБЛЕМА:** Виникли помилки компіляції ("Unsupported escape sequence") у файлі `UniversalEditorScreen.kt` через некоректне екранування символів у регулярних виразах. Спроби виправити це за допомогою інструмента `replace` не мали успіху.
  - **НАСТУПНИЙ КРОК:** Потрібно виправити всі регулярні вирази у класі `ListVisualTransformation` та в обробнику `pointerInput` у файлі `UniversalEditorScreen.kt`, щоб вирішити помилки компіляції.

- **ЗАВЕРШЕНО:** Проведено рефакторинг UI головного екрану.
  - **ЗРОБЛЕНО:** Всі UI компоненти (`MainScreenTopAppBar`, `InProgressIndicator`, `ContextBottomSheet`, `SearchHistoryBottomSheet`, `SearchTextField`, `MainScreenScaffold`, `HandleDialogs`) були винесені в окремі файли.
  - **ЗРОБЛЕНО:** Виправлені помилки компіляції, що виникли в процесі рефакторингу.

- **РОЗПОЧАТО:** Рефакторинг `MainScreenViewModel`.
  - **ПЛАН:** Розбити `MainScreenViewModel` на менші, більш сфокусовані UseCase'и.
  - **ЗРОБЛЕНО:** Створено пусті класи для `SearchUseCase`, `HierarchyUseCase`, `DialogUseCase`, `PlanningUseCase`, `SyncUseCase`, `ProjectActionsUseCase`.
  - **ЗАВЕРШЕНО:** Міграція логіки пошуку та навігації з `MainScreenViewModel` до `SearchUseCase`.
  - **НАСТУПНИЙ КРОК:** Міграція логіки ієрархії до `HierarchyUseCase`.