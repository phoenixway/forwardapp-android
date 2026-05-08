# Context Capability Manual

Актуальний гайд по додаванню нового `capability` у цій кодовій базі.

Цей документ базується на фактичній реалізації в:

- `app/src/main/java/com/romankozak/forwardappmobile/core/gate/ContextRoleRegistry.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextCapabilitiesResolver.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextSessionStore.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextViewPolicy.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_screen/ContextScreenContent.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_properties/ContextSettingsViewModel.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_properties/ContextSettingsScreen.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/navigation/capability/settings/CapabilitySettingsRegistry.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/core/navigation/capability/actions/CapabilityViewActionRegistry.kt`
- `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/di/capabilities/*`

Старі capability-гайди були видалені, бо вони описували неповну і частково неправильну модель. Зокрема, вони стверджували, що достатньо створити `Capability` + Hilt module, після чого нова можливість автоматично з'явиться в UI. У поточному коді це не так.

## 1. Що таке capability в цій кодовій базі насправді

Тут `capability` не є одним механізмом. Це набір окремих шарів:

1. Ідентичність capability:
   - `CapabilityId`
   - інколи `CapabilityDescriptor`
   - інколи `Capability`-object + Hilt multibinding

2. Увімкнення capability для конкретного контексту:
   - role defaults через `ContextRoleRegistry`
   - legacy boolean-прапорці в `ContextConfiguration`
   - `experimentalCapabilityIds`
   - резолвінг через `ContextCapabilitiesResolver`

3. Поява окремого view на екрані контексту:
   - окремий `ContextViewMode`
   - мапінг у `ContextViewPolicy`
   - окремий `when`-branch у `ContextScreenContent`
   - супутні label/icon/input-mode мапінги

4. Setup tab у налаштуваннях контексту:
   - окремий `CapabilitySettingsEntry`
   - окремий Hilt module з `@IntoSet`

5. Дії, специфічні для конкретного view:
   - `CapabilityViewActionEntry`
   - окремий Hilt module з `@IntoSet`

Через це питання "як додати capability" завжди треба розбивати на уточнення:

- воно має просто вмикатись як фіча?
- має з'явитися як окрема вкладка/view на context screen?
- має мати setup tab у context settings?
- має мати додаткові actions у нижній панелі?

## 2. Які патерни реально існують зараз

У коді є кілька різних типів capability.

### 2.1. Повноцінний descriptor + Hilt module + окремий settings tab

Приклад: `direction`

- `DirectionCapability` у `features/contexts/data/models/capabilities/direction/DirectionCapability.kt`
- `DirectionCapabilityModule.kt`
- `DirectionCapabilitySettingsModule.kt`
- `DirectionCapabilityViewActionsModule.kt`

Але навіть тут `register(runtime)` зараз `No-op`, а сам view рендериться не через runtime-screen registry, а напряму в `ContextScreenContent`.

### 2.2. Capability, який дає тільки settings UI

Приклад: `inbox_sorting`

- `InboxSortingCapabilityModule.kt`
- `InboxSortingCapabilitySettingsModule.kt`

У нього `supportedViews = emptySet()`, а `register(runtime)` нічого не робить. Тобто capability може існувати без окремого context-screen view.

### 2.3. View на context screen без окремого runtime-screen

Приклад: `key_problems`

- є `KeyProblemsCapability`
- є `ContextViewMode.KEY_PROBLEMS`
- view рендериться в `ContextScreenContent`
- в коментарі прямо вказано, що окрема runtime-реєстрація не потрібна

### 2.4. Legacy/embedded capability без окремого `Capability` object

Так працюють поточні `backlog`, `inbox`, `log`, `artifact`, `dashboard`, `connections`.

Вони:

- існують як `CapabilityId`
- резолвляться через `ContextCapabilitiesResolver`
- впливають на доступні `ContextViewMode`
- мають UI-гілки у `ContextScreenContent`

Але для більшості з них немає окремого `Capability` object у `features/contexts/di/capabilities`.

Висновок: у проєкті одночасно живуть новіший capability-підхід і legacy view-driven підхід.

## 3. Найважливіший висновок для нового `journal_log`

Якщо ти хочеш, щоб `journal_log` був окремим елементом у списку capability і окремим view на екрані контексту, то одного `CapabilityDescriptor` недостатньо.

Потрібно окремо підключити:

1. сам capability ID і місця, де він вважається "відомим";
2. логіку активації для контексту;
3. окремий `ContextViewMode`;
4. рендеринг у `ContextScreenContent`;
5. label/icon/input-mode мапінги;
6. setup tab, якщо він потрібен.

Якщо цього не зробити, capability може формально існувати в реєстрі, але не з'явиться як view у UI.

## 4. Рекомендований спосіб додати новий `journal_log`

Нижче описаний практичний шлях саме для нового capability, який:

- видно у `Features` на екрані налаштувань контексту;
- можна увімкнути для контексту;
- має окремий view на context screen;
- може мати окремий settings tab.

### Крок 1. Вибрати системний ID

Для прикладу:

- `CapabilityId("journal_log")`

Тримай один і той самий raw-id скрізь:

- descriptor
- role registry
- feature toggle mapping
- settings ownerCapability
- view/action ownership

Не використовуй кілька варіантів типу `journal`, `journal_log`, `log_journal`.

### Крок 2. Зробити capability "відомим" системі

Мінімум:

- додати `CapabilityId("journal_log")` у `ContextRoleRegistry.getAllKnownCapabilities()`

За потреби:

- додати його в один або кілька role presets у `ContextRoleRegistry`

Чому це потрібно:

- `ContextSettingsViewModel.loadExistingProject()` будує список `features` через `ContextRoleRegistry.getAllKnownCapabilities()`
- якщо capability там немає, він не потрапить у список feature toggles на settings screen

### Крок 3. Додати formal capability descriptor

Для нового capability краще робити окремий `Capability` object і Hilt module, навіть якщо view буде рендеритись напряму в `ContextScreenContent`.

Причини:

- capability стає видимим для `CapabilityRegistry`
- `CapabilityGate` перевіряє реєстр для non-legacy capability
- це консистентно з `direction`, `key_problems`, `inbox_sorting`

Структура:

- `features/contexts/data/models/capabilities/journallog/JournalLogCapability.kt`
- `features/contexts/di/capabilities/JournalLogCapabilityModule.kt`

У descriptor зазвичай потрібні:

- `id = CapabilityId("journal_log")`
- `label = "Journal Log"`
- `navRoute = "journal_log"` або інший стабільний route-id
- `supportedViews = setOf(ViewId("journal_log"))`, якщо це окремий view

Важливо:

- у поточній кодовій базі `supportedViews` є радше метаданими;
- сам показ view на context screen відбувається не автоматично через runtime/view registry.

### Крок 4. Якщо потрібен окремий view на context screen, додати новий `ContextViewMode`

Це обов'язково.

Додай нове значення в:

- `core-data-models/.../ContextAdditionalModels.kt`

Наприклад:

- `JOURNAL_LOG`

Після цього доведеться пройтися по всіх `when (ContextViewMode)` і доповнити їх. Це не опціонально: enum уже зараз використовується в багатьох exhaustive `when`.

Мінімально перевірити:

- `ContextViewPolicy.kt`
- `ContextScreenContent.kt`
- `ContextViewActions.kt`
- `ViewModeToggle.kt`
- dashboard label/icon helpers у `ContextScreenContent.kt`
- інші місця, де `ContextViewMode` мапиться на іконку, label або input mode

### Крок 5. Підключити view до політики доступних режимів

У `ContextViewPolicy` треба:

- додати priority для `JOURNAL_LOG`
- додати мапінг у `orderPriority()`

Чому:

- саме `ContextViewPolicy.availableViews(enabled)` визначає, які режими реально доступні користувачу
- `ContextSessionStore` будує `availableViews` через цей policy

Критично:

- `ContextViewPolicy.toCapabilityId()` мапить `ContextViewMode` у `CapabilityId` просто через `name.lowercase()`
- тому назва enum має збігатись із raw capability id після lowercase/underscore-нормалізації

Для `JOURNAL_LOG` це зручно, бо вийде `journal_log`.

### Крок 6. Реально намалювати UI view

Окремий view має бути явно підключений у `ContextScreenContent`.

Тобто треба:

1. створити composable, наприклад:
   - `features/contexts/ui/context_screen/capabilities/journallog/JournalLogView.kt`
2. додати branch у `when (currentViewMode)` у `ContextScreenContent.kt`

Приблизна схема:

- `ContextViewMode.JOURNAL_LOG -> JournalLogView(...)`

Саме тут відбувається фактичне підключення view до UI.

Без цього:

- capability може бути enabled;
- view mode може бути в enum;
- але користувач нічого не побачить.

### Крок 7. Підключити labels, icons і поведінку перемикання view

Після додавання нового `ContextViewMode` потрібно оновити helper-и, які формують UI навігації між режимами.

Перевірити щонайменше:

- `ContextScreenContent.kt`
  - `dashboardLabel()`
  - `dashboardIcon()`
- `ViewModeToggle.kt`
  - `displayName()`
  - `toIcon()`
  - `getDefaultInputMode()`
- `ContextViewActions.kt`
  - `defaultInputMode()`

Якщо цього не зробити:

- у меню перемикання view будуть неправильні іконки/назви;
- або код не скомпілюється через неповний `when`.

### Крок 8. Підключити capability у settings screen як feature toggle

Сам по собі descriptor не додає toggle в `Features`.

У поточній реалізації `Features` на settings screen будується з `ContextSettingsViewModel.loadExistingProject()`, де:

- береться `ContextRoleRegistry.getAllKnownCapabilities()`
- для кожного capability будується текстовий label
- label стає key у `uiState.features`

Тому для нового capability треба перевірити:

1. чи він потрапляє у `getAllKnownCapabilities()`;
2. чи label коректно мапиться назад у `featureLabelToCapabilityId()`.

Для `journal_log` поточний fallback-механізм уже працюватиме, бо:

- label стане `Journal log`
- `featureLabelToCapabilityId()` для невідомих label робить `lowercase().replace(" ", "_")`
- це дасть `journal_log`

Але якщо потрібен інший label, не покладайся на fallback і додай явний branch у `featureLabelToCapabilityId()`.

### Крок 9. Визначити, як capability зберігається в конфігурації

Для нового capability рекомендований шлях:

- зберігати його в `experimentalCapabilityIds`

Не варто для нового capability одразу додавати новий legacy boolean-прапорець, якщо немає сильної причини.

Чому:

- у поточній архітектурі non-legacy capability нормально живуть через `experimentalCapabilityIds`
- саме так працюють новіші capability типу `key_problems` і `inbox_sorting`

Що вже робить існуючий код:

- `onToggleFeature()` додає/прибирає capability з `experimentalCapabilityIds`
- `persistFeatureFlags()` записує `experimentalCapabilityIds` назад у `ContextConfiguration`
- `ContextCapabilitiesResolver` читає їх і включає у фінальний `enabledCapabilities`

### Крок 10. Якщо потрібен setup tab, створити `CapabilitySettingsEntry`

Setup tab для capability підключається окремо, не через descriptor.

Потрібно:

1. створити composable вкладки, наприклад:
   - `features/contexts/ui/context_properties/capabilitysettings/journallog/JournalLogSettingsContent.kt`
2. створити Hilt module:
   - `features/contexts/di/capabilities/JournalLogCapabilitySettingsModule.kt`
3. дати `ownerCapability = CapabilityId("journal_log")`

Механіка така:

- `CapabilitySettingsRegistry.forCapabilities(enabledCapabilities)` відфільтровує вкладки по `ownerCapability`
- `ContextSettingsScreen` додає їх після базових табів

Отже setup tab з'явиться тільки якщо:

- capability enabled для цього контексту;
- `CapabilitySettingsEntry` зареєстрований через `@IntoSet`.

### Крок 11. Якщо потрібні специфічні кнопки дій для view, додати `CapabilityViewActionEntry`

Для нижньої панелі чи меню view-специфічні дії підключаються окремо:

- через `CapabilityViewActionEntry`
- через Hilt `@IntoSet`
- з прив'язкою до:
  - `ownerCapability`
  - `viewMode`

Приклади:

- `BacklogCapabilityViewActionsModule.kt`
- `DirectionCapabilityViewActionsModule.kt`

UI бере їх через:

- `ContextScreenViewModel.getAvailableCapabilityViewActions()`
- `CapabilityViewActionRegistry.forView(...)`

Тому для `journal_log` це окреме рішення, не частина settings tab і не частина view registration.

## 5. Що саме треба зробити для "Journal Log", якщо він має бути окремою вкладкою/view

Повний чекліст:

1. Додати `CapabilityId("journal_log")` у `ContextRoleRegistry.getAllKnownCapabilities()`.
2. Додати його в default role(s), якщо він має приходити з пресетів.
3. Створити `JournalLogCapability` + `JournalLogCapabilityModule`.
4. Додати `ContextViewMode.JOURNAL_LOG`.
5. Оновити `ContextViewPolicy`.
6. Створити `JournalLogView`.
7. Підключити `JournalLogView` у `ContextScreenContent`.
8. Оновити всі label/icon/input-mode мапінги для нового view mode.
9. Перевірити `featureLabelToCapabilityId()` для правильного round-trip.
10. Якщо потрібен setup tab: створити `JournalLogCapabilitySettingsModule`.
11. Якщо потрібні view actions: створити `JournalLogCapabilityViewActionsModule`.

## 6. Якщо "Journal Log" має бути не окремим capability, а розширенням існуючого `LOG`

Це другий, часто простіший варіант.

Якщо по суті це не нова capability, а просто новий підвид існуючого логування, тоді може бути достатньо:

- не створювати `journal_log` як новий `CapabilityId`
- залишити `ContextViewMode.LOG`
- розширити модель даних/фільтрацію/типи в `LogContent.kt`
- за потреби додати setup tab, прив'язаний до існуючого `log`, або окремий settings-only capability

Це краще, якщо:

- користувач не повинен бачити окремий view-перемикач;
- journal log є лише варіантом поточного `LOG`-екрана;
- не потрібна окрема рольова capability-семантика.

Це гірше, якщо:

- journal log має бути окремим елементом у списку capability;
- має окремий lifecycle;
- має окремий setup;
- має окремі дії та окрему UX-модель.

## 7. Важливе зауваження про runtime/view registry

У кодовій базі існують:

- `CapabilityRuntime`
- `CapabilityBootstrapper`
- `ViewRegistry`
- `ViewResolver`

Але станом на перевірений код:

- `ViewRegistryModule` створює `InMemoryViewRegistry(emptySet())`
- для поточних context-screen capability view не знайшлося реального runtime-screen registration
- `DirectionCapability`, `KeyProblemsCapability`, `InboxSortingCapability` не реєструють екрани через `register(runtime)`

Практичний висновок:

- для context screen орієнтуйся не на `runtime.registerScreen(...)`, а на `ContextViewMode` + `ContextScreenContent`
- старі інструкції, де capability view підключався через runtime-screen registration, не відповідають поточному робочому шляху

## 8. Мінімальний план реалізації без сюрпризів

Якщо зараз треба впевнено додати новий окремий capability, роби так:

1. Спочатку додай `CapabilityId` у `ContextRoleRegistry.getAllKnownCapabilities()`.
2. Потім створи descriptor/module.
3. Потім додай `ContextViewMode`.
4. Потім підключи UI у `ContextScreenContent`.
5. Потім додай settings tab через `CapabilitySettingsEntry`.
6. Потім додай optional actions.
7. Після цього пройдися по compile errors від `when (ContextViewMode)` і закрий усі місця.

Це найбільш надійний шлях для цієї конкретної кодової бази.

## 9. Коротка відповідь на головне питання

Як створити і правильно підключити новий capability з власним view і setup-вкладкою:

- descriptor/module: так, бажано створити;
- context-screen view: обов'язково окремо реєструється через `ContextViewMode` і `ContextScreenContent`, не автоматично;
- settings tab: обов'язково окремо через `CapabilitySettingsEntry` і Hilt `@IntoSet`;
- feature toggle: capability має бути "відомим" через `ContextRoleRegistry.getAllKnownCapabilities()`;
- збереження стану: для нового capability краще через `experimentalCapabilityIds`.

Саме така схема відповідає поточному фактичному коду.
