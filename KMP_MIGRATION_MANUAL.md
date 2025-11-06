# Мануал з міграції фічі на Kotlin Multiplatform та SQLDelight

Документ описує перевірений порядок винесення фіч Android застосунку у KMP‑шар із підключенням SQLDelight. Дотримання цих кроків допомагає уникати збоїв компіляції та тримати синхронізованими Android і JS таргети.

## Передумови

- Створений модуль `shared` із таргетами `android` та `js`, підключений до `app`.
- У `shared/build.gradle.kts` додані плагіни `org.jetbrains.kotlin.multiplatform`, `org.jetbrains.kotlin.plugin.serialization`, `com.android.library`, а також залежності SQLDelight (`runtime`, `coroutines`, `android-driver`, `sqljs-driver`).
- Базова структура `shared/src/{commonMain,androidMain,jsMain}` існує; у `commonMain` немає Android-специфічних анотацій.
- `MASTER_PLAN.md` оновлений, щоб координувати чергу міграцій.

## Загальний порядок

1. **Аналіз фічі**
   - Визначити моделі, DAO, репозиторії, допоміжні сервіси та UI, що залежать від Android API.
   - Зафіксувати залежності в `MASTER_PLAN.md`, описати необхідні expect/actual.

2. **Підготовка спільних моделей**
   - Винести data-класи у `shared/src/commonMain` (наприклад, `shared/features/<feature>/data/model`).
   - Позбутися Room- або Android-специфічних анотацій. Якщо Room ще потрібен на Android, створити окремі `FooRoomEntity` з маперами `toShared()/toRoom()`.
   - За потреби додати утиліти (`Clock`, генератори UUID, логування) у `shared`.

3. **Налаштування SQLDelight**
   - Додати схему у `shared/src/commonMain/sqldelight/<package>/<Feature>Queries.sq`.
   - Переписати запити з DAO у `.sq` файл; дотримуватися імен згенерованих колонок.
   - Запустити `./gradlew :shared:generateSqlDelightInterface` (або `make check-compile`, якщо огортає Gradle).
   - У `shared` додати фабрику `DatabaseDriverFactory` з expect/actual для Android (AndroidSqliteDriver) і JS (sql.js).

4. **Кросплатформні інтерфейси**
   - Описати інтерфейси для платформних залежностей (наприклад, `LinkItemDataSource`), що залишаються на Room/Jetpack.
   - Реалізувати Android-адаптери в `app` модулі (використати наявні DAO, перетворити entity ↔ record).

5. **Перенесення репозиторію**
   - Створити новий репозиторій у `shared/commonMain` і замінити Room DAO на виклики SQLDelight (`AttachmentQueries`, тощо).
   - Інжектити кросплатформні інтерфейси для платформних сутностей.
   - Для потоків використовувати `mapToList` + `CoroutineContext` з `Dispatchers.IO` (передається під час DI).
   - Якщо Android-проєкт покладався на `@Inject`/Room-репозиторій, залиште у `app/data/repository` тонку обгортку, яка делегує до KMP-реалізації. Це зберігає існуючі зв'язки Hilt та дозволяє поступово переводити споживачів.

6. **Оновлення Android DI**
   - У `DatabaseModule` ініціалізувати `ForwardAppDatabase` через `createForwardAppDatabase(DatabaseDriverFactory(context))`.
   - Провайдити `AttachmentQueries` (або інші згенеровані DAO) та Android-адаптери інтерфейсів.
   - Замінити конструкторні залежності репозиторію на нові KMP-компоненти.

7. **Адаптація споживачів**
   - Оновити ViewModel/UseCase імпорти на KMP-моделі.
   - В UI перевірити, що типи (`LinkItemRecord`, `AttachmentEntity` тощо) співпадають.
   - Якщо ViewModel планується виносити в `shared`, підготуйте expect/actual або обгортку AndroidX.

8. **Тестування та документація**
   - Прогнати `./gradlew :app:compileDebugKotlin` + `./gradlew :shared:compileKotlinJs` (за можливості поза sandbox).
   - Оновити `TESTING_MANUAL.md` з новими кроками.
   - Задокументувати прогрес у `PROGRESS_LOG.md`.
    - Якщо Gradle блокують sandbox-права (`gradle-*-bin.zip.lck`), встановіть тимчасовий `GRADLE_USER_HOME` у робочу директорію або запустіть збірку поза sandbox.

## Рекомендовані команди

```bash
./gradlew :shared:generateSqlDelightInterface
./gradlew :app:compileDebugKotlin
make check-compile           # швидка перевірка Kotlin-коду
make debug-cycle             # складання, установка, запуск APK (при наявному пристрої)
```

> **Примітка:** У sandbox середовищах Gradle може падати через `gradle-*-bin.zip.lck`. У такому разі перенесіть збірку на локальну машину без обмежень або попросіть дозвіл на виконання.

## Типові помилки і поради

- **Room + SQLDelight одночасно.** Якщо треба тимчасово залишити Room таблиці, не використовуйте ті ж entity в KMP. Краще створити `RoomEntity` + мапери.
- **UUID/Clock.** На KMP використовуйте `com.benasher44:uuid` та `kotlinx-datetime`.
- **Платформні логери.** Виносьте логування через expect/actual (див. `shared/logging/Logger`).
- **Порядок міграції.** Завжди завершуйте поточну фазу (`MASTER_PLAN.md`) перед переходом до нової, щоб команда могла синхронно працювати.
- **Оновлення DI.** Не забудьте прибрати старі провайдери (Room DAO, репозиторії), інакше Hilt/Koin продовжать інжектити застарілі залежності.

### Практичні нюанси (оновлено 2025‑11)

- **Спільний доступ до бази.** На Android SQLDelight має працювати поверх того самого `SupportSQLiteOpenHelper`, що й Room. Створення окремого `AndroidSqliteDriver(context, name = "forward_app_database")` призводить до `Can't downgrade database` та `database is locked`. Використовуйте `AndroidSqliteDriver(appDatabase.openHelper)`.
- **Назви колонок.** SQLDelight схема має повторювати Room-колонки 1‑в‑1 (`scoring_status`, `show_checkboxes`, …). CamelCase у `.sq` згенерує інші імена й викличе `no such column`.
- **JSON у рядках.** Room зберігає `relatedLinks` як рядок і може писати `"null"`. Перш ніж викликати `Json.decodeFromString`, перевіряйте на порожній/`"null"` та обгорніть у `runCatching`.
- **Залежності Android модуля.** Після переходу на SQLDelight не забудьте підключити `implementation(libs.sqldelight.android.driver)` у `app/build.gradle.kts`, інакше `AndroidSqliteDriver` не знайдеться.
- **Усунення блокувань PRAGMA.** Не виконуйте `db.execSQL("PRAGMA …")` всередині `onConfigure` для Room/SQLDelight спільного драйвера — пару `SELECT PRAGMA ...` замінює exec.
- **Перевірка спеціальних проєктів.** Після імпорту бекапу переконайтеся, що резервні групи (`special`, `strategic`, …) мають очікуваних нащадків: Room міграції створюють їх, але SQLDelight не повторює `onOpen` колбек, тому відсутність prePopulate помітна вартою перевірки.

## Чекліст перед завершенням міграції фічі

- [ ] Спільні моделі знаходяться у `shared/commonMain`, без Android-специфічних анотацій.
- [ ] SQLDelight `.sq` файли покривають усі потрібні таблиці/запити.
- [ ] Репозиторій реалізований у `shared`, повертає KMP-моделі, використовує SQLDelight.
- [ ] Android адаптери (`LinkItemDataSource`, драйвери, DI) під’єднані.
- [ ] UI/бізнес логіка компілюються з новими моделями.
- [ ] Оновлені `MASTER_PLAN.md`, `PROGRESS_LOG.md`, `TESTING_MANUAL.md`.
- [ ] Запущені основні build/test команди; результати задокументовані в PR/логах.

Дотримання цього мануалу дозволяє крок за кроком переносити решту фіч у KMP, зберігаючи контроль над Android-залежностями та даючи змогу згодом під’єднати інші платформи (JS/Electron). Якщо виникають нові винятки або змінюється структура, оновлюйте документ разом зі змінами у `shared`.
