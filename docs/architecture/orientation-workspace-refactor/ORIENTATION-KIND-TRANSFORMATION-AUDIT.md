# Аудит маршрутів зміни типу Orientation

Дата: 2026-11-12. Статус: REFERENCE, read-only аудит і пропозиції.
Це не прийняте розширення контракту та не дозвіл змінювати наявні дані.

## Обсяг і висновок

Перевірено всі 42 спрямовані переходи між QUEST, DIRECTION, PROJECT,
INTENT, MILESTONE, ONGOING_STANDARD та OPPORTUNITY: поточний допуск,
оцінки, історію, семантичні залежності, спеціалізованих споживачів і transport.
Взаємні переходи не вважаються автоматично однаково безпечними.
Додатково перевірено межу Aspect-конверсії та проведено попередній огляд
решти типів; для них повного маршрутного аудиту не заявлено.

20 переходів між п'ятьма вже дозволеними типами підтримуються зараз.
Решта 22 — кандидати для окремого розширення лише за перевірених умов.
Поточний застосунок блокує всі ці 22 переходи.

Для незалежних QUEST/DIRECTION, створених через загальне canonical authoring,
не знайдено принципової несумісності з п'ятьма дозволеними типами.
Це висновок зі статичного аналізу, а не виконана перевірка нових команд.
Для квестів з MANUAL ArcQuest mapping і напрямків із Workspace Direction entries
є конкретні несумісні інваріанти. Загальне розширення enum-списку небезпечне.

## Матриця: джерело в рядку, новий тип у стовпці

Матриця нижче стосується живих незалежних canonical Orientations без жодного
legacy mapping і без спеціалізованих залежностей, включно з історичними.

| Звідки / куди | QUEST | DIRECTION | PROJECT | INTENT | MILESTONE | ONGOING_STANDARD | OPPORTUNITY |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QUEST | — | N | N | N | N | N* | N |
| DIRECTION | N | — | N | N | N | N* | N |
| PROJECT | N | N | — | A | A | A* | A |
| INTENT | N | N | A | — | A | A* | A |
| MILESTONE | N | N | A | A | — | A* | A |
| ONGOING_STANDARD | N* | N* | A* | A* | A* | — | A* |
| OPPORTUNITY | N | N | A | A | A | A* | — |

- A: наявна підтримка, з поточним preflight і транзакційною перевіркою.
- N: кандидат після розширення command/history/transport policy та тестів;
  зараз заборонений, безпечність на пристрої не підтверджена.
- `*`: змінюється застосовність Expected span / Target window. При вході в
  ONGOING_STANDARD — DERIVED ONGOING / NOT_APPLICABLE; при виході — UNSET.
  Старі immutable revisions мають зберігатися зі своїм історичним типом.
- Діагональ — відсутність зміни, а не команда трансформації.

QUEST і DIRECTION мають однакову застосовність усіх осей із PROJECT, INTENT,
MILESTONE, OPPORTUNITY. Їхні взаємні переходи самі собою не потребують скидання
оцінок. Це випливає з `axisApplicability`, а не з назв чи ролі сутностей.

## Перевірка маршрутів із QUEST

1. QUEST → DIRECTION. Для незалежної сутності — кандидат N. Для MANUAL ArcQuest
   — блокер: `projectCanonicalArcQuests` і `QuestOrientationBridge.writeCommon`
   вимагають QUEST; Strategic mapping для MANUAL і Tactical resolver квеста
   теж вимагають QUEST. Заміна kind порушує ці перевірки.
2. QUEST → PROJECT/INTENT/MILESTONE/OPPORTUNITY. Той самий блокер джерела;
   додаткової відмінності в оцінках не знайдено.
3. QUEST → ONGOING_STANDARD. Той самий блокер плюс зміна часових осей.
4. PROJECT/INTENT/MILESTONE/OPPORTUNITY → QUEST. Для незалежної сутності — N;
   зміна семантичного типу не повинна автоматично створювати ArcQuest чи
   Strategic participation. Native QUEST уже можна створити без арки.
5. ONGOING_STANDARD → QUEST. Те саме, з поверненням часових осей до UNSET.

Участь у Strategic Arc сама по собі не означає блокер: гілка
ArcQuestSourceType.ORIENTATION перевіряє typed Orientation, але не вимагає QUEST.
Потрібно відрізняти пряме ORIENTATION-посилання від MANUAL carrier/mapping.

Для mapped QUEST потрібно окремо спроєктувати збереження semantic ID,
mapping provenance, історії оцінок, ArcQuest carriers, незмінних sourceType/sourceId,
Strategic/Tactical references та Restore/Merge. Ordinary update ArcQuest не
дозволяє змінювати sourceType/sourceId. Видалити mapping або підмінити carrier
звичайним редагуванням не є безпечним розв'язанням.

## Перевірка маршрутів із DIRECTION

1. DIRECTION → QUEST. Незалежна сутність — N. Якщо є WorkspaceDirectionEntry,
   редактор напрямку вимагає DIRECTION, а transport validator відхиляє інший kind.
2. DIRECTION → PROJECT/INTENT/MILESTONE/OPPORTUNITY. Той самий блокер source
   entries; оцінки сумісні.
3. DIRECTION → ONGOING_STANDARD. Той самий блокер плюс зміна часових осей.
4. PROJECT/INTENT/MILESTONE/OPPORTUNITY → DIRECTION. Незалежна сутність — N;
   семантична зміна не створює Workspace, DIRECTION capability або запис у ній.
   Якщо потрібне таке розміщення, це окрема явна операція.
5. ONGOING_STANDARD → DIRECTION. Те саме, з поверненням часових осей до UNSET.

Особливо важливо: `CanonicalWorkspaceDirectionEntrySyncStore` перевіряє kind
і для tombstoned entries. Просте прибирання активного запису не усуває блокер
Restore/Merge. Відсутність legacy mapping теж не доводить незалежність:
`createSemanticDirection` створює canonical Direction разом із capability entry.
Потрібен пошук усіх entries для semantic ID, а не лише видимих активних.

## Спільні залежності та приховані межі розширення

- `SUBJECT_CONVERSION_TARGET_KINDS` використовується одночасно командою зміни
  kind, Aspect-конверсією, UI, перерахунком оцінок і валідацією immutable facts.
  Додавання QUEST/DIRECTION до цього набору також розширить Aspect → Orientation.
  Рекомендовано розділити політики допустимих операцій перед розширенням.
- Історія зараз забороняє будь-який legacy mapping для converted subject:
  `CanonicalSubjectConversionStore.validate` і спільний reference validator.
  Навіть корекції Quest bridge недостатньо, щоб дозволити mapped QUEST.
- Snapshot generation `semantic-subject-conversion/2` захищає історію kind
  changes. Старі читачі generation 2 мають старий перелік допустимих типів.
  Прийом нових facts та відмова старих клієнтів потребують явної перевірки
  сумісності; сам новий код enum або меню цього не вирішує. Номер майбутньої
  generation цим аудитом не визначається.
- Common Orientation relations, Aspect refs і WorkspaceBindings прив'язані до
  subject ID/type; у перевірених загальних контрактах немає заборони саме
  QUEST/DIRECTION. Їх потрібно зберігати, а не перевигадувати за новим kind.
- Planning participation і references, recurrence SOURCE з typed ORIENTATION
  допускають збереження того самого ID. Винятки — спеціалізовані resolver-и,
  перелічені вище. Не переносити участь між Day/Tactical/Strategic через зміну kind.
- Generic semantic deletion дозволяє лише ті самі п'ять звичайних типів.
  PROJECT → QUEST/DIRECTION змінить доступність видалення в редакторі. Перед
  реалізацією потрібно явно визначити очікуваний lifecycle для незалежних
  QUEST/DIRECTION; довільно розширювати delete policy не слід.
- H1 appearances не повинні змінюватися. Це чинна V2 структура після cutover,
  незалежна від семантичної класифікації. Conversion facts, assessments, relations,
  mappings/provenance, Planning і capability entries — SEMANTIC_NOT_HIERARCHY;
  історичний Context parent не стає джерелом topology. Жодної зміни persisted
  authority або migration lane у цьому аудиті немає.

## Інші класи та типи: попередній огляд

| Маршрут | Поточний стан / межа |
| --- | --- |
| Aspect → п'ять звичайних типів | Є bounded native-команда: taxonomy/tag/reference conversion, власний preflight |
| Aspect → QUEST/DIRECTION | Заборонений тим самим набором; потенційний наступний окремий slice, не автоматичний наслідок kind expansion |
| Orientation → Aspect | Не підтримується; інша subjectType-конверсія, потрібні власні правила для assessments/Planning/taxonomy |
| THEME ↔ інші | Specialized Theme bridge вимагає THEME; змінюються також застосовність часових осей і attention tier; потрібен окремий аудит Day-залежностей |
| GOAL ↔ інші | Goal bridge вимагає GOAL; потрібен окремий аудит backlog/execution/compatibility |
| MAIN_BEACON / MAIN_BEACON_GROUP ↔ інші | Specialized membership/lifecycle; не входять у перевірені 42 маршрути |
| DAY_THEME / ARC_QUEST | Historical compatibility vocabulary, не звичайні цілі authoring або трансформації |
| Orientation ↔ Workspace | Різні identities/owners; EMBODIES/REALIZES — зв'язок, а не зміна підтипу |

## Рекомендований порядок, не прийнятий план

1. Незалежні QUEST ↔ DIRECTION: окремі command eligibility та history rules,
   повний dependency preflight, lifecycle-рішення, транспортна сумісність.
2. Ті самі незалежні типи ↔ PROJECT/INTENT/MILESTONE/OPPORTUNITY, потім
   ONGOING_STANDARD із перевіркою історичних часових оцінок. Спільний механізм
   розробляти один раз, а приймати пари окремо.
3. Напрямки з Workspace entries: окреме рішення щодо збереження/завершення
   спеціалізованих appearances та історичних references у transport.
4. Mapped квести з арок: окремий authority/provenance контракт для
   identity-preserving conversion і збереження історичної участі.

Орієнтовна вартість першого bounded native slice з тестами — medium (2–8 год),
зі залежністю від потрібної transport/lifecycle політики. Повноцінна підтримка
mapped/capability-backed випадків — large (від робочого дня), не правка меню.

Потрібні тести перед дозволом нової пари: збереження ID/оцінок/історії/зв'язків,
відмова при mapping/entry (включно з tombstone), повтор і конфлікт команди,
гонка додавання залежності, rollback, restart, exact Restore, Merge в обох
напрямках, delta/ACK і поведінка старого клієнта. Для Ongoing — explicit/unset
часові значення та кілька послідовних змін kind.

## Докази та перевірки

Основні вихідні файли (шляхи від кореня репозиторію):

- [SubjectConversionEvidence.kt](../../../shared-core-data-models/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/models/orientation/SubjectConversionEvidence.kt): allowlist і shape форматів 1/2.
- [OrientationContract.kt](../../../shared-core-domain/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/domain/orientation/OrientationContract.kt): `axisApplicability`.
- [OrientationConversionHistory.kt](../../../shared-core-domain/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/domain/orientation/OrientationConversionHistory.kt): assessment change, історичний kind chain.
- [CanonicalOrientationReferenceValidation.kt](../../../shared-core-domain/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/core/domain/orientation/CanonicalOrientationReferenceValidation.kt): native/history/reference closure.
- [CanonicalOrientationKindCoordinator.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/orientation/CanonicalOrientationKindCoordinator.kt): command eligibility, transaction, version revalidation.
- [CanonicalSubjectConversionStore.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/orientation/CanonicalSubjectConversionStore.kt): no-mapping invariant та historical closure.
- [QuestOrientationBridge.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/orientation/QuestOrientationBridge.kt): mapped QUEST kind requirements.
- [CanonicalStrategicPlanningMapping.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/planning/strategic/CanonicalStrategicPlanningMapping.kt): MANUAL vs direct ORIENTATION resolution.
- [CanonicalStrategicPlanningAdapter.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/planning/strategic/CanonicalStrategicPlanningAdapter.kt): immutable ArcQuest source.
- [TacticalMissionOrientationResolver.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/planning/tactical/TacticalMissionOrientationResolver.kt): mapped QUEST resolution.
- [CanonicalDirectionEntryEditor.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/workspace/capability/CanonicalDirectionEntryEditor.kt): Direction kind requirement.
- [CanonicalWorkspaceDirectionEntrySyncStore.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/workspace/CanonicalWorkspaceDirectionEntrySyncStore.kt): historical/live entry validation.
- [CanonicalSemanticEntityLifecycleCoordinator.kt](../../../app/src/main/java/com/romankozak/forwardappmobile/data/orientation/CanonicalSemanticEntityLifecycleCoordinator.kt): generic deletion policy.
- [SubjectConversionSnapshotAdapter.kt](../../../core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/SubjectConversionSnapshotAdapter.kt): conversion wire generations.

Виконано 2026-11-12:

- `:app:testProdDebugUnitTest --tests '*CanonicalOrientationKindRoomTest'`: 9/9 PASS.
- `:shared-core-domain:jvmTest --tests '*OrientationKindChangeContractTest'`: 4/4 PASS.
- Нуль failures/errors/skips; `git diff --check` PASS.

Shared тест перебирає всі 20 чинних спрямованих пар; Room перевіряє native
eligibility, відмову specialized/mapped sources, identity/dependencies,
Ongoing assessments, послідовну історію, Restore/Merge, race/replay/rollback
і recurrence SOURCE. Для нових 22 маршрутів це лише статичний висновок:
їх не відкривали й не виконували як успішні трансформації.

Нових маршрутів не реалізовано; production-код і дані в рамках аудиту не
змінювалися. Зміни коду з попередніх запитів збережено. Physical smoke не проводився.
