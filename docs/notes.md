 codex resume 019add97-00ee-7470-a66e-5d10290cd78c

 codex resume 019adab2-2e46-7cc1-9be5-b7214f2f9e50

 проблема - синхронізація через мережу і препопуляція системних сутностей некоректно взаємодіють. помойому синхронізація системні
  сутності взагалі оминає на цей момент хоча можу помилятися. твої завдання
  - розібратися в тому як це все працює і взаємодіє
  - зробити щоб системні сутності теж нормально синхронізувалися
  - зробити щоб препопуляція не створювала нових системних сутностей при наявності старих з таким же системним ключем. щоб функціонал її не
  конфліктував з синхронізацією, імпортами, експортами

  у мене своєрідна система вкладень. задіяні таблиці нотаток, чеклістів, власне вкладень, посилань на них і ще бог знає чого. поточний код не
  синхронізує вкладення так як потрібно. в бібіотеці вкладень в анроїд версії було 106 вкладень. після синхронізації - 12. на анроїді роблю в
  проекті нову нотатку - не синхронізується на десктоп і пропадає з аедроїда. на десктопі при створенні нотатки вона існує локально а на андроїді
  її нема. з цим треба розібратися. ймовірно треба весь процес від створення вкладення будьякого типу до кінця синхронізації покрити контрольними
  логами щоб дізнатися де глюки. зараз створюю на андроїді нотатку для проекта. при синхронізації вона пропадає

amp threads continue T-2a06fdc3-5ca6-4a64-adfc-6605c0accf82

192.168.0.175:8080

відкриваю вкладення проекту на андр. 6 з 8 тих які є на десктопі. проходить кілька секунд - 0 вкладень. ще час проходить - 3 вкладення. ще час прозодить - 0. створюю 
нотатку - при синку пропадає з усіма іншими. наступний синк - 3 вкл але не та яку я стврив. на десктопі 9 включаючи новостворену 
  
Що зробити зараз:

  1. Перезібрати/поставити новий expDebug APK
     і повторити синк.
  2. Після синку знову зняти БД та перевірити:

  adb exec-out run-as
  com.romankozak.forwardappmobile.debug
  cat /data/user/0/
  com.romankozak.forwardappmobile.debug/
  databases/forward_app_database > /tmp/
  forward_app.db
  sqlite3 /tmp/forward_app.db "SELECT COUNT(*)
  FROM attachments; SELECT COUNT(*) FROM
  project_attachment_cross_ref;"
  sqlite3 /tmp/forward_app.db "SELECT
  a.id FROM attachments a LEFT JOIN
  project_attachment_cross_ref c ON
  a.id=c.attachment_id WHERE c.attachment_id
  IS NULL;"

  Очікування: кількість crossRef ≥ attachments, список “без crossRef” — порожній. Якщо ні — скинь результати, будемо копати далі.

відкриваю вкладення проекту на андр. 6 з 8 тих які є на десктопі. проходить кілька секунд - 0 вкладень. ще час проходить - 3 вкладення. ще час прозодить - 0. створюю 
нотатку - при синку пропадає з усіма іншими. наступний синк - 3 вкл але не та яку я стврив. на десктопі 9 включаючи новостворену 

Пропоную по всьому шляху проблемного функціоналу зробити наступне:
- наскільки це можливо винести важливий код в чисті функції
- максимально покрити їх юніт тестами з логами. Включаючи логи того що на вході і що на виході. Якщо великий об'єм логів можна в файли їх писати. 
- те що залежить від БД і мережевих повідомлень з десктопа просто дуже детально логувати також. 
- так ми будемо бачити чи наші тестовані функції дають не те, чи не те дається на їх вході чи з мережі. І можна буде дуже чітко бачити на якому етапі йде збій. 
- можна навіть зробити посилену валідацію входів і виходів функцій. Щоб зразу в логах було видно що йде не так

Тоді буде так 
- запускаємо юніт тести. 
- Якщо не ок - правимо чисті функції в зв'язаний функціонал. 
- якщо ок - запускаємо додаток, проганяємо проблемну ситуацію і дивимося логи

Можливо навіть написати інтеграційні тести щоб бачити автоматично чи конвеєр працює нормально, але це складно і об'ємно
Можна проміжний варіант. Тестова БД в пам'яті. Перевіряємо як наш конвеєр обробляє її тестові дані . Чи генерує нормальний експорт. Чи нормально обробляє імпорт

adb shell run-as com.romankozak.forwardappmobile.debug ls -l /data/user/0/com.romankozak.forwardappmobile.debug/files/sync-dumps

adb exec-out run-as com.romankozak.forwardappmobile.debug sh -c 'rm -f /data/user/0/com.romankozak.forwardappmobile.debug/files/sync-dumps/*'

❯   adb exec-out 'run-as com.romankozak.forwardappmobile.debug tar -cf - -C /data/user/0/com.romankozak.forwardappmobile.debug/files sync-dumps' > /tmp/android-sync-dumps.tar

forwardapp-android on  dev [$!⇡] via 🅶 v8.13 via ☕ v17.0.11 via 🅺 via  v22.20.0 via 🐍 v3.14.0
❯   mkdir -p /tmp/android-sync-dumps && tar -xf /tmp/android-sync-dumps.tar -C /tmp/android-sync-dumps



---

  sudo -u gdm DBUS_SESSION_BUS_ADDRESS=unix:path=/run/gdm/bus \
    gsettings set org.gnome.settings-daemon.plugins.power sleep-inactive-ac-type 'suspend'

  sudo -u gdm DBUS_SESSION_BUS_ADDRESS=unix:path=/run/gdm/bus \
    gsettings set org.gnome.settings-daemon.plugins.power sleep-inactive-ac-timeout 120

      у мене своєрідна система вкладень. задіяні таблиці нотаток, чеклістів, власне вкладень, посилань на них і ще бог знає чого. поточний код не
  синхронізує вкладення так як потрібно. в бібіотеці вкладень в анроїд версії було 106 вкладень. після синхронізації - 12. на анроїді роблю в
  проекті нову нотатку - не синхронізується на десктоп і пропадає з аедроїда. на десктопі при створенні нотатки вона існує локально а на андроїді
  її нема. з цим всім треба розібратися. ймовірно треба весь процес від створення вкладення будьякого типу до кінця синхронізації покрити контрольними
  логами щоб дізнатися де глюки. на андроїді створюю нотатку. при синхронізації вона пропадає.

відкриваю вкладення проекту на андр. 6 з 8 тих які є на десктопі. проходить кілька секунд - 0 вкладень. ще час проходить - 3 вкладення. ще час прозодить - 0. створюю 
нотатку - при синку пропадає з усіма іншими. наступний синк - 3 вкл але не та яку я стврив. на десктопі 9 включаючи новостворену 

Пропоную по всьому шляху проблемного функціоналу зробити наступне:
- наскільки це можливо винести важливий код в чисті функції
- максимально покрити їх юніт тестами з логами. Включаючи логи того що на вході і що на виході. Якщо великий об'єм логів можна в файли їх писати. 
- те що залежить від БД і мережевих повідомлень з десктопа просто дуже детально логувати також. 
- так ми будемо бачити чи наші тестовані функції дають не те, чи не те дається на їх вході чи з мережі. І можна буде дуже чітко бачити на якому етапі йде збій. 
- можна навіть зробити посилену валідацію входів і виходів функцій. Щоб зразу в логах було видно що йде не так

Тоді буде так 
- запускаємо юніт тести. 
- Якщо не ок - правимо чисті функції в зв'язаний функціонал. 
- якщо ок - запускаємо додаток, проганяємо проблемну ситуацію і дивимося логи

Можливо навіть написати інтеграційні тести щоб бачити автоматично чи конвеєр працює нормально, але це складно і об'ємно
Можна проміжний варіант. Тестова БД в пам'яті. Перевіряємо як наш конвеєр обробляє її тестові дані . Чи генерує нормальний експорт. Чи нормально обробляє імпорт



codex resume 019ae933-e395-7442-806c-d7a46b031a5b




app/src/main/java/com/romankozak/forwardappmobile/features/contexts . я хочу щоб сутності проекти були перетворені на контексти. структура проекта - конфігурація контекста. пресет структури - профайл ролі контекста. контекст має були поліморфною сутністю яка залежно від ролі може отримувати набір активних фіч, дефолтний стартовий огляд, ще щось
ролі через профайли можна робити кастомні.

Рекомендації для перетворення проектів на контексти: 
1. Поліморфна архітектура контекстів
   Базова сутність Context (замість Project):
- roleCode - визначає поведінку через профайл
- configurationId - посилання на конфігурацію контексту
- Поліморфні методи для фіч залежно від ролі

2. Профайли ролей (ContextRoleProfile) замість структур проектів
Додати до існуючих:
- activeFeatures - набір активних фіч - ключова штука. мають бути набори доступних фіч і активованих. 
- defaultView - стартовий огляд. має бути список доступних views залежно від активних фіч. з нього вибирати стартовий огляд
- availableViews - view modes які може мати контекст в даному профайлу ролі
- uiPreferences - налаштування інтерфейсу
- permissions - права доступу
- roleName - назва ролі контексту в тексті. Наприклад, проект, напрямок, орієнтир, аспект.

3. Конфігурація контексту (ContextConfiguration) Замість пресету проекту:
- бере за основу поточний профайл ролі. його можна змінити і це змінить властивості що успадковуються від профайлу ролі
- baseProfileCode - базовий профайл ролі
- baseRoleName
- activeFeatures - при зміні ролі робляться такимим як в профайлі ролі. але користувач для поточного контексту має мати можливість через ui змінити в конфігурації контексту активовані фічі
- availableViews - доступні режими перегляду. аналогічно activeFeatures
- тобто базовий профайл ролі задає стартову основу особливостей контексту а конфігурація контексту - це поточні налаштування які користувач може робити над конкретним контекстом 

4. Factory для контекстів
class ContextFactory {
    fun create(profile: ContextRoleProfile, config: ContextConfiguration): Context
}

план пропоную такий 
- перейменувати сутності і онови імпорти там де вони використовуються. включаючи dao і репозиторії. міграція бд
- впровадити добре діючу модель щодо активних фіч і доступних та стартових видів. 
- решта функціоналу в моделі даних будується на основі неї
- Реалізувати динамічну логіку: В бізнес-логіці (ViewModel, UseCases) зчитувати роль контексту та динамічно вмикати/вимикати елементи UI та функціональність.
- Оновлення UI/UX
    1. Оновити тексти: Замінити всі згадки "Проект" на "Контекст" в інтерфейсі.
    2. Адаптувати UI: Модифікувати екрани для підтримки диначної зміни функціоналу залежно від ролі.
    3. UI для керування ролями: Створити новий екран, де користувачі зможуть створювати та налаштовувати профайли ролей.

робити зміни не масові а точкові. і відразу перевіряти компіляцією. шоб потім не робити відкат годинної роботи щоб знайти баги 

питання. якщо я хочу використати потомок контексту більш специфічний. проект наприклад. 
чи є сенс його якось робити потомком чи просто створити фічі, види для проекту, профайл ролі проекту і все?

твої рекомендації?

***

Є екран в app/src/main/java/com/romankozak/forwardappmobile/features/contexts/ui/context_hierarchy_screen/ProjectHierarchyScreen.kt . це стабільний функціонал який я хочу оновити. Проекти зокрема мають стати контекстами. Гнучкою поліморфною сутністю. Є купа зввязаних з ними сутностей, коду. Все це треба адаптувати. 

Є екран app/src/main/java/com/romankozak/forwardappmobile/features/context_lab/ContextLabScreen.kt . це полігон для розробки і  тестування нових версії сутностей. останні практично готові. Я хочу знати які сутності моделей даних використовують обидва екрани. мені треба знати кожну сутність яку треба змінити і її альтернативну еспериментальну сутність. стратегія яку я бачу в тому щоб поступово, точечно, з перевіркою компіляцією кожної зміни обєднати стабільні і експериментальні суттності і звязаний весь код адаптувати під це. Експериментальні сутності недороблені але мають фічі які неодмінно слід перенести на стабільну існуючу модель даних. 
першою дією я бачу створити таблицю вичерпну сутностей даних - один рядок на дві сутності стабільна і альтернатива її експериментальна. коли це буде готово можна буде робити план уніфікації її

Ключові ідеї:
  * Створення адаптера: Це ключовий елемент, який дозволить системі функціонувати під час перехідного періоду.
  * Поступова міграція даних: Оновлення бази даних без видалення старих полів дозволить уникнути "великого вибуху" і відкотитися в разі проблем.
  * Поетапна рефакторинг фіч: Це правильний підхід, щоб контролювати процес та тестувати кожну зміну окремо.

розроби конткретний план щодо цього?

Тепер я маю дві ключові сутності для порівняння:

   * Стабільна: com.romankozak.forwardappmobile.features.contexts.data.models.Context
   * Експериментальна: com.romankozak.forwardappmobile.core.context.Context

    core.context.ContextConfiguration.kt
    core.context.ContextRole.kt



 План імплементації

  Фаза 1: Створення базових компонентів нової моделі

   1. Оновлення `core.context.Context`:
       * Додати поля val label: String та val description: String? до класу core.context.Context.
   2. Перевірка `core.context.ContextId`:
       * Переконатися, що value class ContextId коректно визначений.
   3. Перевірка `core.context.ContextRole`:
       * Переконатися, що ContextRole визначений і готовий для мапінгу projectType зі старої моделі.
   4. Визначення базових інтерфейсів/класів `Capability`:
       * Створити інтерфейс Capability та, можливо, базові реалізації для ActiveCapability (для JSON-зберігання) та PersistentCapability (для полів БД).
   5. Перевірка `core.context.ContextConfiguration`:
       * Переконатися, що ContextConfiguration визначений і може містити activeCapabilities та activeViews.

  Фаза 2: Імплементація "можливостей" (Capabilities)

   1. Реалізація `AuditingCapability` та `LinkingCapability` (JSON-зберігання):
       * Визначити data-класи для AuditingCapability та LinkingCapability з відповідними CapabilityId.
       * Реалізувати логіку серіалізації/десеріалізації цих можливостей до/з JSON-поля properties.
   2. Реалізація `HierarchyCapability` (Окремі колонки БД):
       * Визначити data-клас HierarchyCapability, який інкапсулює parentId та order.
   3. Реалізація `ScoringCapability` (Окремі колонки БД):
       * Визначити data-клас ScoringCapability, який інкапсулює всі поля скорингу.
   4. Реалізація `ProjectManagementCapability` (Окремі колонки БД):
       * Визначити data-клас ProjectManagementCapability, який інкапсулює isCompleted, projectStatus, projectStatusText та isProjectManagementEnabled.
   5. Налаштування тегів (Окрема таблиця зв'язку):
       * Створити сутності Tag та ContextTag (join-таблиця).
       * Визначити DAOs та репозиторії для ефективної роботи з тегами.

  Фаза 3: Адаптація та міграція

   1. Створення адаптера `features.contexts.data.models.Context` -> `core.context.Context`:
       * Написати функцію/клас, що конвертує стару сутність у нову. Адаптер повинен мапити всі поля, заповнювати label, description, ContextRole, ContextConfiguration та екстрагувати дані для всіх "можливостей".
   2. Рефакторинг `ProjectRepository`:
       * Модифікувати ProjectRepository для повернення об'єктів core.context.Context.
       * Використовувати адаптер всередині репозиторію для конвертації даних, отриманих зі старої схеми БД.
   3. Оновлення `ProjectHierarchyScreenViewModel`:
       * Модифікувати ProjectHierarchyScreenViewModel для безпосереднього використання об'єктів core.context.Context.
       * Скоригувати логіку UI, яка отримує властивості контексту, використовуючи нові label, description, role та config.activeCapabilities.
   4. Міграція схеми бази даних (Room):
       * Додати нову колонку properties: String? (для JSON) до таблиці projects.
       * Додати нові колонки для label, description, roleCode, parentId, order, всіх полів ScoringCapability, isCompleted, projectStatus, projectStatusText.
       * Створити таблиці tags та context_tags.
       * Реалізувати Room-міграції для заповнення цих нових колонок існуючими даними.

  Фаза 4: Тестування та ітерація

   1. Unit- та інтеграційні тести:
       * Написати тести для адаптера та нових компонентів моделі.
       * Забезпечити повне покриття нової логіки тестами.
   2. Поступова інтеграція:
       * Впроваджувати зміни поступово, перевіряючи функціональність після кожного значного кроку.

  Я пропоную почати з Фази 1, кроку 1: оновити `core.context.Context`. Що скажете?

  ***

    Ось зведений список:

  Існуючі стабільні сутності (для довідки та адаптера):

   * app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/Context.kt
   * app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/RelatedLink.kt
   * app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/ContextTypeConverter.kt
   * app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/ReservedContextKeys.kt
   * app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/ReservedGroup.kt

  Існуючі експериментальні сутності (ядро нової моделі):

   * app/src/main/java/com/romankozak/forwardappmobile/core/context/Context.kt
   * app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextId.kt
   * app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextRole.kt
   * app/src/main/java/com/romankozak/forwardappmobile/core/context/ContextConfiguration.kt
   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/CapabilityId.kt
   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/CapabilityDescriptor.kt
   * app/src/main/java/com/romankozak/forwardappmobile/core/context/ViewId.kt

  Нові сутності, які будуть створені (або існуючі, які будуть розширені/адаптовані):

   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/Capability.kt (Новий інтерфейс для можливостей)
   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/AuditingCapability.kt (Data-клас для можливості аудиту)
   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/LinkingCapability.kt (Data-клас для можливості посилань)
   * app/src/main/java/com.romankozak/forwardappmobile/core/capability/HierarchyCapability.kt (Data-клас для можливості ієрархії)
   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/ScoringCapability.kt (Data-клас для можливості скорингу)
   * app/src/main/java/com/romankozak/forwardappmobile/core/capability/ProjectManagementCapability.kt (Data-клас для можливості управління проектами)
   * app/src/main/java/com/romankozak/forwardappmobile/data/models/Tag.kt (Нова сутність для тегів)
   * app/src/main/java/com/romankozak/forwardappmobile/data/models/ContextTag.kt (Нова join-сутність для зв'язку Context-Tag)
   * app/src/main/java/com/romankozak/forwardappmobile/data/daos/TagDao.kt (Новий DAO для тегів)
   * app/src/main/java/com/romankozak/forwardappmobile/data/daos/ContextTagDao.kt (Новий DAO для ContextTag)
   * app/src/main/java/com/romankozak/forwardappmobile/data/repository/TagRepository.kt (Новий репозиторій для тегів)
   * app/src/main/java/com/romankozak/forwardappmobile/data/converters/CapabilityConverter.kt (Новий Room TypeConverter для JSON-можливостей)

  Це буде наша дорожня карта по файлах.

  я в процесі рефакторинга. мета його - перейменувати сутність проекти на сутність контексти. по всьому коду. і зробити щоб це компілювалося. зроблено я сказав би на 80%. 
  запусти ./gradlew compileExpDebugKotlin. визнач помилки і виправ їх. заверш рефакторинг

AttachmentRepository.kt
AttachmentsViewModel.kt

app/src/main/java/com/romankozak/forwardappmobile/features/attachments/ui/context/AttachmentsViewModel
app/src/main/java/com/romankozak/forwardappmobile/features/attachments/data/AttachmentRepository
app/src/main/java/com/romankozak/forwardappmobile/features/attachments/ui/library/AttachmentsLibraryScreen.kt
/app/src/main/java/com/romankozak/forwardappmobile/features/attachments/ui/library/AttachmentsLibraryViewModel.kt

Ось оновлений, максимально деталізований промпт для агента. Він побудований на принципі **"Test-Driven Refactoring"** — ми не переходимо до наступного кроку, поки не переконаємося, що поточний працює на реальному пристрої.

***

пошукай в @app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/DatabaseInitializer.kt і інших місцях які ще системні і зарезервовані проекти використовувалися. забув сказати - я в процесі рефакторинга. мета його - перейменувати сутність проекти на сутність контексти. по всьому коду. і зробити щоб це компілювалося. зроблено я сказав би на 80%. тобто проекти - це стара назва, контексти - нова 

***

я замінюю логіку системних проектів через ReservedGroup на нову через SystemAppEntity.kt. треба завершити рефакторинг.
запит такий був. 

Перехід на SystemContexts та очищення застарілої логіки
Контекст: Ми замінюємо стару логіку спеціальних проектів (яка базувалася на ContextType, systemKey та reservedGroup) на нову систему SystemContexts, де роль проекту визначається виключно його ContextId.

Важливо: Кожен етап має завершуватися успішною компіляцією та ручною перевіркою на телефоні.

0. Еталонний код для впровадження
   Використовуй ці сніпети як основу:

Kotlin

// core/context/SystemContexts.kt
object SystemContexts {
val INBOX = ContextId("sys_inbox")
val DAY_REVIEW = ContextId("sys_day_review")
val STRATEGIC_REVIEW = ContextId("sys_strategic")

    private val RESERVED = setOf(INBOX, DAY_REVIEW, STRATEGIC_REVIEW)

    fun isSystem(id: ContextId): Boolean = RESERVED.contains(id)
}

// Приклад використання в логіці видалення або UI
if (SystemContexts.isSystem(context.id)) {
// Блокувати видалення або приховати кнопку
}
Етап 1: Функціональна заміна логіки
Впровадження: Створи файл SystemContexts.kt у пакеті core.context.

Пошук та заміна: Знайди всі згадки projectType == ContextType.SYSTEM, systemKey != null або reservedGroup. Заміни їх на перевірку через SystemContexts.isSystem(id).

Захист від видалення: Знайди код, що відповідає за видалення контекстів/проектів, і додай перевірку, щоб системні ID неможливо було видалити.

ЗУПИНКА ТА ТЕСТ:

Чи компілюється проект?

Запусти додаток. Чи працюють існуючі "Інбокс" та "Спеціальні проекти" як раніше?

Чи зникла можливість видалити їх (якщо ти вже оновив UI)?

Етап 2: Міграція даних (SQL)
Створення міграції: Напиши Migration для Room, яка:

Для проектів із reserved == true або наявним system_key змінює їхній id на відповідний системний (sys_inbox, sys_day_review тощо).

Оновлює Foreign Keys: Обов'язково виконай UPDATE для таблиць notes та tasks, щоб вони посилалися на нові ID.

Database Initializer: Онови RoomDatabase.Callback, щоб при чистій інсталяції ці системні ID створювалися автоматично.

ЗУПИНКА ТА ТЕСТ:

Запусти додаток (бажано на базі з існуючими даними).

Перевір через Database Inspector або в самому додатку: чи змінилися ID у системних проектів?

Чи залишилися нотатки та завдання всередині цих проектів?

Етап 3: Видалення застарілих сутностей
Очищення ContextEntity: Тільки тепер видали поля projectType, systemKey, reservedGroup та Enum ContextType.

Оновлення БД: Напиши міграцію для видалення відповідних колонок (через ALTER TABLE DROP COLUMN або перестворення таблиці, залежить від версії SQLite).

Остаточне очищення: Видали всі застарілі мапери чи константи, що стосувалися старої логіки.

ЗУПИНКА ТА ТЕСТ:

Чи компілюється проект?

Протестуй повний цикл: створення звичайного контексту, робота з системним контекстом, спроба видалення.

Суворі обмеження:
Жодного fallbackToDestructiveMigration().

Кожна зміна ID має супроводжуватися оновленням зв'язків у notes та tasks.

Якщо ти не впевнений, де використовується стара логіка — використовуй глобальний пошук за назвами полів.

***
Поточний план

 ПОТОЧНА СИТУАЦІЯ:

   * `SystemContexts.kt`: Містить коректний набір системних ContextId.
   * `ReservedContextKeys.kt`: Визначає рядкові константи для старих системних ключів.
   * `Migrations.kt`: Містить існуючі міграції бази даних Room до версії 94.
   * `MigrationUtils.kt`: Містить допоміжні функції для міграцій; hasColumn зроблено internal.
   * Статус компіляції: Проект компілюється успішно після виправлення попередніх помилок. Залишилися міграція бази даних та очищення застарілої логіки.

  НАСТУПНІ КРОКИ (Етап 2: Міграція даних (SQL) та Етап 3: Видалення застарілих сутностей):

  Фаза 1: Реалізація міграції бази даних (MIGRATION_94_95)

   1. Визначити `MIGRATION_94_95` у `app/src/main/java/com/romankozak/forwardappmobile/data/database/Migrations.kt`
       * Дія: Додати новий об'єкт MIGRATION_94_95 до файлу Migrations.kt. Ця міграція оброблятиме зміни ID та очищення схеми.
       * Ключові файли:
           * app/src/main/java/com/romankozak/forwardappmobile/data/database/Migrations.kt
           * app/src/main/java/com/romankozak/forwardappmobile/data/database/MigrationUtils.kt (для помічника hasColumn)
           * app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/ReservedContextKeys.kt (для зіставлення старих системних ключів з новими ID)
       * Логіка всередині `MIGRATION_94_95.migrate(db: SupportSQLiteDatabase)`:
           * Зіставити системні ключі: Створити Kotlin map зі старих ReservedContextKeys (наприклад, "personal-management") на нові ContextId з префіксом sys_ (наприклад, "sys_personal_management").
           * Визначити таблиці з зовнішніми ключами: Визначені таблиці: contexts, backlog_orders, context_execution_logs, inbox_records, list_items, notes, note_documents, checklists, scripts, context_artifacts,
             context_attachment_cross_ref, tactical_missions, tactical_mission_attachment_cross_ref.
               * Самокорекція: Підтверджено, що AiEventEntity, LifeSystemStateEntity, AiInsightEntity не мають полів contextId, тому їх буде виключено з оновлень.
           * Зібрати мапу Old-to-New ID: Проітерувати таблицю contexts. Для кожного Context з system_key записати його поточний id та цільовий новий sys_ prefixed ID. Зберігати це в мапі oldId -> newId.
           * Оновити таблицю `contexts`: Для кожного запису в мапі oldId -> newId виконати оператор UPDATE на таблиці contexts для зміни id на newId та system_key на newId.
           * Оновити пов'язані таблиці: Для кожної таблиці, визначеної як така, що має зовнішній ключ до contexts.id, проітерувати мапу oldId -> newId та оновити відповідний contextId (або псевдонім стовпця, такий як list_id) у цих
             таблицях. Використовувати hasColumn для безпечної перевірки існування стовпця.
           * Видалити старі стовпці: Використати ALTER TABLE contexts DROP COLUMN project_type та ALTER TABLE contexts DROP COLUMN reserved_group. Огорнути це в перевірки hasColumn.
           * Видалити старі індекси: Використати DROP INDEX IF EXISTS index_contexts_project_type та DROP INDEX IF EXISTS index_contexts_reserved_group.

   2. Оновити `DatabaseModule.kt` для включення `MIGRATION_94_95`
       * Дія: Додати MIGRATION_94_95 до списку міграцій у app/src/main/java/com/romankozak/forwardappmobile/core/di/DatabaseModule.kt.
       * Ключовий файл: app/src/main/java/com/romankozak/forwardappmobile/core/di/DatabaseModule.kt

   3. Оновити `RoomDatabase.Callback` для початкових даних (якщо застосовно)
       * Дія: Переглянути DatabaseModule.kt на предмет заповнення початкових даних бази даних (наприклад, addCallback). Якщо системні контексти створюються там, переконатися, що вони використовують нові ContextId з префіксом sys_.
       * Ключовий файл: app/src/main/java/com/romankozak/forwardappmobile/core/di/DatabaseModule.kt

  Фаза 2: Очищення старих сутностей (Етап 3: Видалення застарілих сутностей)

   1. Видалити `system_key` з сутності `Context`
       * Дія: Модифікувати клас даних Context у app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/Context.kt, щоб видалити поле system_key. Це робиться після виконання міграції, тому база даних більше не
         очікує цього стовпця.
       * Ключовий файл: app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/Context.kt

   3. вирішити що робити з migrateSpecialProjects. спитати в користувача

  Перевірка після кожної фази:

   * Компіляція: Переконатися, що проект компілюється успішно.
   * Ручне тестування: Запустити програму та вручну перевірити:
       * Існуючі "Вхідні" та інші системні проекти функціонують як очікувалося.
       * Системні проекти неможливо видалити.
       * Нотатки та завдання в системних проектах коректно пов'язані.
       * (Після Фази 2) Схема бази даних більше не містить стовпців project_type або reserved_group (можна перевірити за допомогою Database Inspector).

  Це детальний план. Я почну з Фази 1, Кроку 1 (визначення MIGRATION_94_95).

***

  Май на увазі що я перейменував проекти в контексти в процесі минулого рефакторингу. можуть залишатися залишки старої назви

***

✦ Користувач вважає, що видалення runPostBackupMigration() було помилкою, оскільки вона, можливо, забезпечувала коректну обробку системних проєктів після відновлення з резервної копії.

  Я визнав свою помилку: migrateSpecialProjects (яку викликала runPostBackupMigration) була "лікувальним" механізмом для системних проєктів після імпорту резервної копії. Моя MIGRATION_94_95 та DatabaseInitializer не покривають цей
  сценарій.

  Необхідно створити нову функцію (наприклад, reconcileSystemContextsAfterImport), яка, використовуючи новий підхід SystemContexts, буде:
   1. Перевіряти наявність усіх зарезервованих системних ContextId.
   2. Створювати їх, якщо відсутні (наприклад, через стару резервну копію).
   3. Оновлювати некоректні імена (наприклад, "Вхідні" на "Inbox").

  Переглянутий план:

   1. Повернути виклик `runPostBackupMigration()` у SyncFileService.kt.
   2. Створити нову функцію/клас `SystemContextReconciler` для примирення системних контекстів.
   3. Викликати цей новий примиритель замість старої міграції:
       * У SyncFileService.kt (після імпорту резервної копії).
       * У DatabaseModule.kt (у RoomDatabase.Callback для чистої установки).
   4. Видалити `ReservedContextKeys.kt` (після реалізації нової логіки).
   5. Видалити `migrateSpecialProjects` та її помічників (після реалізації нової логіки та перевірки).

