# Контексти, головні орієнтири і стратегічна ієрархія

Цей документ фіксує поточний механізм і рекомендації для семантичної
інтеграції контекстів з головними життєвими орієнтирами.

## Поточна модель

Контекст зберігається як `Context` у таблиці `contexts`.

Ключові поля:

- `id` - стабільний ідентифікатор контексту;
- `name`, `description` - назва і опис;
- `parentId` - єдиний канонічний батьківський контекст;
- `goal_order` / `order` - порядок серед siblings;
- `is_expanded` - історичний стан розгортання;
- `role_code` - семантична роль або структурний preset;
- `tags` - додаткові маркери.

Канонічна ієрархія зараз будується через `parentId`.
Root-контексти - це контексти без `parentId`.

Основні місця реалізації:

- `core-data-models/.../entities/Context.kt`
- `core-data-models/.../entities/ContextHierarchyData.kt`
- `app/.../features/contexts/data/dao/ContextDao.kt`
- `app/.../data/repository/ContextRepository.kt`
- `app/.../features/contexts/ui/context_hierarchy_screen/usecases/HierarchyUseCase.kt`
- `app/.../features/contexts/ui/context_hierarchy_screen/ContextHierarchyScreenViewModel.kt`
- `app/.../features/contexts/ui/context_hierarchy_screen/hierarchy/*`

Головні орієнтири вже мають окрему доменну модель `MainBeacon`, тому їх не
треба перетворювати на спеціальний тип контексту.

Основні місця реалізації `MainBeacon`:

- `core-data-models/.../entities/MainBeaconModels.kt`
- `app/.../features/mainscreen/core/MainBeaconDao.kt`
- `app/.../features/mainscreen/core/MainBeaconRepository.kt`
- `app/.../features/mainscreen/CoreLevelViewModel.kt`
- `app/.../features/mainscreen/CoreLevelScreen.kt`
- `core-data-models/.../sync/SnapshotBundle.kt`
- `core-data-models/.../sync/snapshots/misc/MainBeaconSnapshots.kt`

Схема вже містить:

- `main_beacons`;
- `main_beacon_context_cross_ref`;
- `main_beacon_attachment_cross_ref`;
- `main_beacon_level_statuses`.
- `context_parent_links` - додаткові структурні появи контексту під іншими
  parent-контекстами без зміни canonical `Context.parentId`.

## Важлива деталь про вкладення

Контексти можуть також з'являтися у вмісті іншого контексту через
`list_items` як `SUBLIST` / context link. Це не те саме, що канонічний
`parentId`, хоча `ContextRepository.moveContext()` зараз синхронізує обидва
рівні: змінює `parentId` і додає/видаляє відповідний link у `list_items`.

Практичний висновок: `parentId` варто лишити як основне дерево, а додаткові
появи контексту в інших місцях моделювати окремими зв'язками/лінками.

## Поточний екран ієрархії

`ContextHierarchyScreen` уже має сильну базу для стратегічної роботи:

- показ ієрархії контекстів;
- фокусування на піддереві;
- breadcrumbs;
- пошук;
- створення child-контекстів;
- drag/drop reorder;
- move через chooser;
- copy/cut/paste контекстів;
- selection mode;
- доступ до меню контексту.

Обмеження:

- актуальний список у новішому `ProjectHierarchyView` фактично плоский:
  `buildVisibleHierarchy()` зараз повертає `flattenedHierarchy` без фільтрації
  по `isExpanded`;
- `flattenHierarchyWithLevels()` зараз не рекурсивний;
- `is_expanded` є полем доменної сутності і потрапляє у sync snapshot, тому
  це не ідеальне місце для локального UI-стану strategic tree;
- reorder через drag/drop зараз працює як переміщення відносно target item,
  але не як повноцінне drag-to-nest створення нового parent-child зв'язку.

## Рекомендована доменна модель

### 1. `MainBeacon` лишається головним орієнтиром

Оскільки окрема сутність головних орієнтирів уже існує, не варто дублювати її
через `Context.roleCode = main-beacon` або новий тип контексту.

Рекомендовано:

- `MainBeacon` = головний життєвий орієнтир;
- `Context` = універсальний контейнер реалізації: напрямок, проект,
  управління, проміжний container;
- зв'язок між ними = `main_beacon_context_cross_ref`;
- вкладення/документи, пов'язані з орієнтиром, = `main_beacon_attachment_cross_ref`.

Поточна роль `ContextRoleRegistry.ROLE_MAIN_BEACON = "main-beacon"` виглядає
як потенційне дублювання. Якщо немає живого сценарію, де саме контекст має
імітувати головний орієнтир, її краще депрекейтнути:

- прибрати з UI створення нових контекстів з роллю `main-beacon`;
- лишити читання/відображення для legacy даних;
- не використовувати її для нової strategic hierarchy логіки;
- нові головні орієнтири створювати тільки як `MainBeacon`.

### 2. Головний орієнтир контексту

Потреба: будь-який контекст може існувати для реалізації певного головного
орієнтира.

Мінімально інвазивний варіант уже існує:

- використовувати `main_beacon_context_cross_ref`;
- додати зручні repository/usecase методи поверх нього:
  - `observeBeaconsWithContextTree()`;
  - `observeBeaconContextIds(beaconId)`;
  - `linkContextToBeacon(beaconId, contextId)`;
  - `unlinkContextFromBeacon(beaconId, contextId)`;
  - `setPrimaryBeaconForContext(contextId, beaconId)` тільки якщо потрібна
    рівно одна основна прив'язка.

Якщо один контекст може реалізовувати кілька головних орієнтирів,
поточний cross-ref уже підходить краще, ніж nullable поле у `contexts`.

Якщо все-таки потрібна семантика "головний з кількох", її краще додати в
cross-ref, а не в `Context`:

- `relation_type` (`primary`, `secondary`, `supporting`);
- `item_order`;
- `created_at`, `updated_at`, `is_deleted`, `version`.

Це збереже `MainBeacon` як джерело правди і не змішає різні домени.

### 3. Кілька parentів

Не рекомендується робити `parentIds: List<String>` у `contexts`.
Це зламає просту деревоподібну модель, ускладнить deletion, move, sync,
cycles, breadcrumbs і порядок siblings.

Краще:

- `parentId` лишити як канонічний parent;
- додаткові появи реалізувати окремою таблицею зв'язків:
  `context_parent_links`;
- фактичні поля першої реалізації:
  - `parent_context_id`
  - `child_context_id`
  - `link_order`
  - `createdAt`, `updatedAt`, `synced_at`
  - `is_deleted`, `version`
- таблиця додана міграцією `126 -> 127`;
- backup/snapshot має поле `contextParentLinks`;
- UI builder використовує `parentId` + активні `context_parent_links` як
  combined graph, але `Move` і drag/drop і далі змінюють тільки canonical
  `parentId`;
- меню контексту має окрему дію `Додати появу тут`, яка бере контексти з
  clipboard і створює structural link без дублювання контексту.

Поле `relation_type` наразі не введене. Його варто додавати тільки тоді, коли
з'явиться реальна потреба розрізняти `alias`, `supporting`, `related` тощо.

## Рекомендований UX напрям

Основний strategic hierarchy screen краще будувати як єдине дерево, де
`MainBeacon` є root-вузлами, а під ними показується поточна канонічна
ієрархія контекстів. Тоді UX лишається майже таким самим, як у поточному
`ContextHierarchyScreen`: користувач бачить дерево, розгортає/згортає вузли,
фокусується на піддереві і переміщує контексти.

Базова форма:

- root level = `MainBeacon` nodes + virtual node `No beacon`;
- під кожним `MainBeacon` = контексти, прив'язані через
  `main_beacon_context_cross_ref`, збережені у своїй canonical context
  hierarchy;
- під `No beacon` = root-контексти без `parentId`, які не мають жодного
  зв'язку з `MainBeacon`;
- діти таких root-контекстів лишаються під ними за `parentId`.

Це краще, ніж окремий режим "орієнтири", бо стратегічна картина і звична
ієрархія стають одним і тим самим екраном.

### Beacon-rooted tree rules

Рекомендовані правила побудови:

1. Побудувати canonical `contextChildMap` через `Context.parentId`.
2. Побудувати `beaconId -> contextIds` через `main_beacon_context_cross_ref`.
3. Для кожного `MainBeacon` знайти top-level context entry points:
   - контексти, прямо прив'язані до beacon;
   - якщо прив'язаний контекст має ancestor, який теж показується під цим же
     beacon, не дублювати його як top-level, а показати у canonical місці.
4. Для `No beacon` взяти тільки root-контексти:
   - `parentId == null`;
   - немає direct beacon link;
   - або немає ancestor/path, який входить у beacon-rooted tree.
5. Розгортання всередині beacon використовує ту саму логіку, що і поточне
   context subtree.

Якщо контекст прив'язаний до кількох `MainBeacon`, є два варіанти:

- простий: показувати його під кожним beacon як secondary appearance;
- строгий: додати `relation_type` у cross-ref і показувати canonical appearance
  під `primary`, а під іншими beacon показувати як linked/alias node.

На першому етапі достатньо простого варіанту з візуальним маркером "linked in
multiple beacons".

Потрібні режими:

- `Усе дерево` - beacon-rooted tree: `MainBeacon` + `No beacon`;
- `Піддерево beacon` - focus на одному `MainBeacon`;
- `Піддерево контексту` - поточний фокус на вибраному контексті;
- `Canonical context tree` - опційний технічний режим без beacon roots, якщо
  треба дебажити/редагувати сирий `parentId`.

Потрібні дії:

- collapse all;
- expand one level;
- focus subtree;
- create container here;
- move into;
- convert to container / set role;
- link/unlink context to `MainBeacon`;
- set primary beacon тільки якщо cross-ref отримає `relation_type`;
- show linked secondary parents.

Візуально достатньо мінімальних позначок:

- role chip або маленький label;
- іконка/emoji з context marker;
- child count;
- secondary parent/link indicator.

Для `No beacon` краще використати нейтральну назву на кшталт:

- `No beacon`;
- `Unassigned`;
- `Без головного орієнтира`.

В UI це має бути virtual node, а не реальний `MainBeacon` у базі, щоб не
змішувати службову групу з користувацькими орієнтирами.

## Персистентний стан розгортання

Є два різні сценарії:

1. Локальний стан екрану: "що я розгорнув на цьому пристрої зараз".
2. Синхронізований стратегічний стан: "яку стратегічну картину я хочу бачити
   всюди".

`Context.isExpanded` уже синхронізується у snapshot, але він прив'язаний тільки
до контекстів. Для дерева, де root може бути `MainBeacon`, цього недостатньо.

Рекомендовано:

- ввести окрему модель стану дерева, а не використовувати тільки
  `Context.isExpanded`;
- мінімум:
  - `expandedContextIds`;
  - `expandedBeaconIds`;
  - `focusedBeaconId`;
  - `focusedContextId`;
- якщо стан має синхронізуватися, зробити таблицю на кшталт
  `strategic_hierarchy_view_state`;
- якщо стан має бути тільки локальним, тримати його у `SettingsRepository`
  DataStore;
- `buildVisibleHierarchy()` має фільтрувати descendants за `expandedIds`;
- мати команди `collapseAll`, `expandLevel(1)`, `expandPathTo(contextId)`,
  `focusBeacon(beaconId)`, `focusContext(contextId)`.

Поточний `CoreLevelScreen` має `expandedBeaconIds` як `remember` state, тобто
стан розкриття beacon-карт не переживає пересоздання екрана. Для стратегічної
роботи цього замало.

## Мобільна глибина дерева

На малому екрані не варто намагатися показувати всю глибоку ієрархію
звичайними відступами. Після 4-5 рівнів дерево стане нечитабельним.

Краще використовувати zoom/focus модель:

- перший екран: тільки `MainBeacon` або верхній рівень контекстів;
- tap по beacon -> focused view цього beacon;
- tap по контексту з дітьми -> focused subtree цього контексту;
- breadcrumbs показують шлях;
- indentation обмежити максимумом;
- глибокі рівні показувати як "поточний рівень + діти", а не як нескінченний
  full tree;
- для дуже глибоких структур використовувати path chips / parent header
  замість великих left padding.

Практично: `ContextHierarchyScreen` уже має focus mode і breadcrumbs, тому
його треба розширювати, а не замінювати.

## Послідовність реалізації

1. Документально зафіксувати поточну модель і терміни.
2. Вирішити долю `roleCode = main-beacon`: deprecated/legacy або чіткий
   окремий сенс, але не дублювання `MainBeacon`.
3. Додати/винести usecase, який збирає `MainBeacon` + linked contexts +
   canonical context subtree + virtual `No beacon`.
4. Розширити `ContextHierarchyScreen`, щоб root items могли бути не тільки
   `Context`, а й `MainBeacon` / virtual group.
5. Додати focus mode для `MainBeacon`, аналогічний існуючому focus mode для
   контексту.
6. Додати persisted `expandedBeaconIds` + `expandedContextIds`.
7. Додати швидке link/unlink context <-> `MainBeacon` з ієрархічного екрану.
8. Додати швидке створення проміжного container-контексту.
9. Лише після цього вирішувати, чи потрібна окрема таблиця для кількох
   parentів, чи достатньо secondary links.

## Головна рекомендація

Не починати з повного DAG і нового екрану.

Найкращий перший крок - еволюціонувати поточний `ContextHierarchyScreen` у
strategic orientation tree:

- контекст = універсальна одиниця;
- головний орієнтир = `MainBeacon`;
- зв'язок головного орієнтира з контекстами =
  `main_beacon_context_cross_ref`;
- root екрану = `MainBeacon` nodes + virtual `No beacon`;
- `parentId` = канонічна ієрархія;
- додаткові parentи = links/secondary placements;
- expanded/collapsed = окремий view-state для beacon/context tree;
- grouping containers = звичайні контексти з роллю `container`.

Це дає стратегічну картину без дублювання в Obsidian і без різкого зламу
поточної архітектури.
