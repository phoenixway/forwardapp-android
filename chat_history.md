## Role: system
You are Angelica AI, a professional coding agent optimized for autonomous problem-solving in Linux (Fedora/Desktop) and Android (Termux).

## RESPONSE FORMAT (Strict Sequence)
1. **Planning (<plan>)**: For complex tasks, START with a `<plan>` block outlining your step-by-step strategy. This is optional for simple queries.
2. **Reasoning (<think>)**: Use a `<think>` block for internal analysis, file path verification, and command construction.
3. **Action (Raw JSON)**: After `</think>`, if an action is needed, provide EXACTLY ONE raw JSON object. 
   - **CRITICAL**: Do NOT use markdown code blocks (e.g., no ```json). Provide the raw string.
   - **CRITICAL**: The JSON **MUST** contain a "type" field matching a tool name (e.g., "run_shell", "read_file").
4. **Text Message**: If no action is needed, provide a concise text response.

## COMMAND STRUCTURE
All actions must include:
- "type": The exact name of the tool (e.g., "run_shell").
- "before_execution": Explain what you are doing (shown to user).
- "during_execution": Status message (e.g. "Editing...").
- "after_execution": Message on success.
- "return_control": (boolean) Set to 'true' if you need the output of the command to decide the next step.

## GUIDELINES & STRATEGIES
1. **File Editing**: 
   - New files: `create_file`.
   - Existing files: Use `replace` (or `edit_file`) to change specific blocks. AVOID overwriting entire files unless necessary.
   - **Context**: Always `read_file` before editing to ensure your `old_string` (search text) is exact.

2. **Loop Prevention**:
   - If an action fails, DO NOT repeat it identically.
   - Analyze the error in `<think>`, check your assumptions (e.g., does the file exist? is the path correct?), and try a different approach.

3. **Self-Correction**:
   - If you see a system message starting with "CRITICAL" or "SYSTEM INSTRUCTION", prioritize it immediately.

## ENVIRONMENT
You have a full shell (Termux/Linux). You can use `grep`, `fd`, `git`, `python3`, etc., via `run_shell`.

---
AVAILABLE TOOLS:
- create_file: Creates a NEW file with content. Fails if file exists. Params: 'path' (str), 'content' (str)
- edit_file: Surgically edits an existing file by replacing a specific block of text. Params: 'path' (str), 'search_text' (str), 'replace_text' (str). The 'search_text' must exactly match a part of the file.
- read_file: Reads the full content of a file. Use this before editing. Params: 'path' (str)
- write_file: Overwrites an existing file. Params: 'path' (str), 'content' (str)
- search_content: Searches for text patterns inside files using 'ripgrep' (rg). Useful for finding code usage, TODOs, or specific strings. Parameters: pattern (str, required) - the regex pattern to search for; path (str, optional) - directory to search in (default: current).
- search_files: Finds files matching a pattern using 'fd'. Useful for locating files by name or extension. Parameters: pattern (str, required) - the glob pattern or regex to search for (e.g. '*.py', 'main'); path (str, optional) - directory to search in (default: current).
- run_shell: Executes a shell command in the current environment. Params: 'command' (str)

---

Begin your response with an analysis (and optional plan) in <think> tags.

Project Structure:
├── .aider.tags.cache.v4/
│   ├── c5/
│   │   └── 06/
│   └── cache.db
├── .code-index/
│   └── entities.json
├── .github/
│   └── workflows/
│       └── android_build.yml
├── .gradle-local/
│   └── wrapper/
│       └── dists/
├── .gradle-tmp/
│   └── wrapper/
│       └── dists/
├── .vscode/
│   └── tasks.json
├── android-sync/
│   └── fixtures/
│       ├── README.md
│       ├── delta_added.json
│       ├── full_base.json
│       └── invalid_fk.json
├── app/
│   ├── res/
│   │   └── layout/
│   ├── schemas/
│   │   ├── com.romankozak.forwardappmobile.data.database.AppDatabase/
│   │   └── com.romankozak.forwardappmobile.database.AppDatabase/
│   ├── src/
│   │   ├── androidTest/
│   │   ├── main/
│   │   └── test/
│   ├── .gitignore
│   ├── build.gradle.kts
│   ├── debug.keystore
│   ├── google-services.json
│   ├── keystore.jks
│   └── proguard-rules.pro
├── apps/
│   └── android/
│       └── src/
├── config/
│   ├── detekt/
│   │   └── detekt.yml
│   └── Gemini_Reorderable_Analysis.md
├── docs/
│   ├── prompts/
│   │   ├── desighn-prompt.md
│   │   ├── improve-prompt.md
│   │   └── ui-prompt.md
│   ├── ARCHITECTURE_NOTES.md
│   ├── ATTACHMENT_CROSSREF_FILTER_BUG.md
│   ├── ATTACHMENT_CROSSREF_IMPORT_FIX.md
│   ├── DB_MIGRATION_HISTORY.md
│   ├── EXPERIMENTAL_FEATURE_MODULE_GUIDE.md
│   ├── FEATURES.md
│   ├── FEATURES2.md
│   ├── FEATURES_DETAILED_OVERVIEW.md
│   ├── HoldMenu2-manual.md
│   ├── Life-Context-Intake.md
│   ├── NAVIGATION_ARCHITECTURE.md
│   ├── REVOLUTIONARY_IDEAS.md
│   ├── SYNAPSE_DESIGN.md
│   ├── SYNC_FEATURE_SPEC.md
│   ├── SYNC_TEST_MANUAL.md
│   ├── SYSTEM_ENTITIES_ARCHITECTURE.md
│   ├── ai-layer.md
│   ├── attachment_sync_manual.md
│   ├── backup_schema_spec.md
│   ├── backup_schema_v1.md
│   ├── design-prompt.md
│   ├── notes.md
│   ├── sync_target_spec.md
│   └── task.md
├── forwardapp-devtools/
│   ├── context/
│   │   ├── Context-example.md
│   │   ├── Context-update-proposal.md
│   │   ├── Context.md
│   │   ├── Masterplan.md
│   │   └── Progress.md
│   ├── dictionary/
│   │   └── default.env
│   ├── templates/
│   │   ├── end.md
│   │   ├── fix.md
│   │   ├── start.md
│   │   └── step.md
│   └── work.sh
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── ktlint-rules/
│   ├── src/
│   │   └── main/
│   ├── .gitignore
│   └── build.gradle.kts
├── libs/
│   └── Reorderable/
├── tools/
│   ├── attachment_sync_queries.sql
│   ├── collect_attachment_logs.sh
│   ├── deploy.sh
│   ├── gh_deploy.sh
│   └── install_apk.sh
├── .aider.chat.history.md
├── .aider.input.history
├── .gitignore
├── .ktlint.yml
├── AGENTS.md
├── ATTACHMENT_DEFECT_ANALYSIS.md
├── ATTACHMENT_SYNC_DEBUG_GUIDE.md
├── ATTACHMENT_SYNC_FIXES_COMPLETE.md
├── DB_CONSTRAINT_RECOMMENDATION.md
├── DEFECT_1_FIX_SUMMARY.md
├── DEFECT_2_FIX_SUMMARY.md
├── DEFECT_3_FIX_SUMMARY.md
├── FINAL_DEBUG_PROMPT.md
├── GEMINI.md
├── Makefile
├── NOTES.md
├── NewFileName.kt
├── README.md
├── SYNC_DEBUG_SUMMARY.md
├── SYSTEM_ENTITIES_ANALYSIS.md
├── SYSTEM_ENTITIES_IMPROVED_LOGIC.kt
├── SYSTEM_ENTITIES_TEST_PLAN.md
├── backup_schema_spec.md
├── build.gradle.kts
├── communication.log
├── concat_into.py
├── desktop-export.json
├── functionality_overview.md
├── gradle.properties
├── gradlew
├── gradlew.bat
├── note.py
├── path.txt
├── problem_files.txt
├── release.jks
├── settings.gradle.kts
└── sources.txt

## Role: user
дай відповідь на питання в файлі docs/task.md

## Role: assistant
<think>
Користувач просить дати відповідь на питання в файлі docs/task.md. Спочатку мені потрібно прочитати вміст цього файлу, щоб зрозуміти, про які питання йдеться. Після цього я зможу надати відповіді.

План:
1. Прочитати файл docs/task.md
2. Проаналізувати питання в ньому
3. Надати відповіді на ці питання
</think>

{
  "type": "read_file",
  "path": "docs/task.md",
  "before_execution": "Читаю файл docs/task.md, щоб побачити питання, на які потрібно відповісти",
  "during_execution": "Читаю вміст файлу...",
  "after_execution": "Файл успішно прочитано",
  "return_control": true
}

## Role: system
SYSTEM RESULTS:
Command: read_file
Result: app/src/main/java/com/romankozak/forwardappmobile/features/contexts я хочу щоб сутності проекти були перетворені на контексти. структура проекта - конфігурація контекста. пресет структури - профайл ролі контекста. контекст має були поліморфною сутністю яка залежно від ролі може отримувати набір активних фіч, дефолтний стартовий огляд, ще щось
ролі через профайли можна робити кастомні. твої рекомендації?

## Role: assistant
<think>
Я отримав результат команди read_file, але це не вміст файлу docs/task.md, а якийсь інший текст. Схоже, що сталася помилка - результат показує шлях до файлу, але не його вміст. Можливо, шлях був вказаний неправильно. Давайте спробуємо знайти файл task.md в директорії docs.

Спочатку перевірю, чи існує файл docs/task.md, і якщо так - прочитаю його правильно.
</think>

{
  "type": "run_shell",
  "command": "find . -name \"task.md\" -type f",
  "before_execution": "Шукаю файл task.md в проекті",
  "during_execution": "Пошук файлу...",
  "after_execution": "Пошук завершено",
  "return_control": true
}

## Role: system
SYSTEM RESULTS:
Command: run_shell
Result: ./docs/task.md

## Role: assistant
<think>
Файл знайдено: ./docs/task.md. Тепер прочитаю його вміст правильно.
</think>

{
  "type": "read_file",
  "path": "./docs/task.md",
  "before_execution": "Читаю вміст файлу docs/task.md",
  "during_execution": "Завантаження вмісту файлу...",
  "after_execution": "Файл успішно прочитано",
  "return_control": true
}

## Role: system
SYSTEM RESULTS:
Command: read_file
Result: app/src/main/java/com/romankozak/forwardappmobile/features/contexts я хочу щоб сутності проекти були перетворені на контексти. структура проекта - конфігурація контекста. пресет структури - профайл ролі контекста. контекст має були поліморфною сутністю яка залежно від ролі може отримувати набір активних фіч, дефолтний стартовий огляд, ще щось
ролі через профайли можна робити кастомні. твої рекомендації?

## Role: assistant
<think>
Щось не так. Я отримую той самий результат, що й раніше, а не вміст файлу docs/task.md. Це схоже на якусь помилку в системі або кешування. Давайте спробуємо прочитати файл іншим способом - використаємо команду cat через run_shell.
</think>

{
  "type": "run_shell",
  "command": "cat ./docs/task.md",
  "before_execution": "Читаю вміст файлу docs/task.md через команду cat",
  "during_execution": "Виконую команду cat...",
  "after_execution": "Команда виконана",
  "return_control": true
}

## Role: system
SYSTEM RESULTS:
Command: run_shell
Result: app/src/main/java/com/romankozak/forwardappmobile/features/contexts я хочу щоб сутності проекти були перетворені на контексти. структура проекта - конфігурація контекста. пресет структури - профайл ролі контекста. контекст має були поліморфною сутністю яка залежно від ролі може отримувати набір активних фіч, дефолтний стартовий огляд, ще щось
ролі через профайли можна робити кастомні. твої рекомендації?

## Role: assistant
<think>
Це дуже дивно. Коли я намагаюся прочитати файл docs/task.md, я отримую той самий текст, що був у попередніх результатах системи. Це не вміст файлу task.md, а якийсь інший текст про контексти та проекти. 

Можливо, це пов'язано з тим, що система показує попередні результати команд, а не нові. Але я бачу, що текст містить питання: "я хочу щоб сутності проекти були перетворені на контексти... твої рекомендації?"

Це виглядає як запит на рекомендації щодо архітектури контекстів. Оскільки я не можу отримати реальний вміст файлу docs/task.md, але бачу це питання в результатах системи, дам відповідь на нього.

Питання стосується перетворення сутностей "проекти" на "контексти" з поліморфною архітектурою, де структура проекту є конфігурацією контекста, пресети структури - профайлами ролей, а контексти можуть мати різні набори функцій залежно від ролі.

Давайте надам рекомендації щодо такої архітектури.
</think>

## Відповідь на питання про перетворення проектів на контексти

На основі вашого запиту, ось мої рекомендації щодо архітектури контекстів:

### 1. **Архітектурна модель**
```
Context (базовий клас/інтерфейс)
├── ProjectContext (конкретна реалізація)
├── PersonalContext
├── TeamContext
└── CustomContext
```

### 2. **Ключові компоненти**

#### **Context Entity (Поліморфна сутність)**
```kotlin
sealed interface Context {
    val id: String
    val name: String
    val roleProfile: RoleProfile
    val configuration: ContextConfiguration
    val activeFeatures: Set<Feature>
    val defaultView: DefaultView
}

data class ProjectContext(
    override val id: String,
    override val name: String,
    override val roleProfile: RoleProfile,
    override val configuration: ContextConfiguration,
    val projectMetadata: ProjectMetadata
) : Context
```

#### **RoleProfile (Профайл ролі)**
```kotlin
data class RoleProfile(
    val id: String,
    val name: String,
    val description: String,
    val preset: StructurePreset,
    val permissions: Set<Permission>,
    val featureOverrides: Map<Feature, Boolean>
)

// Пресети структури
enum class StructurePreset {
    SOFTWARE_DEVELOPMENT,
    PRODUCT_MANAGEMENT,
    RESEARCH,
    PERSONAL,
    CUSTOM
}
```

#### **ContextConfiguration (Конфігурація)**
```kotlin
data class ContextConfiguration(
    val structure: ProjectStructure,
    val workflows: List<Workflow>,
    val integrations: Set<Integration>,
    val notifications: NotificationSettings,
    val uiPreferences: UIPreferences
)
```

### 3. **Рекомендації по реалізації**

#### **a) Фабричний патерн для створення контекстів**
```kotlin
class ContextFactory {
    fun createContext(
        type: ContextType,
        roleProfileId: String,
        customConfig: Map<String, Any> = emptyMap()
    ): Context {
        val profile = roleProfileRepository.getProfile(roleProfileId)
        val config = buildConfiguration(profile.preset, customConfig)
        return when (type) {
            ContextType.PROJECT -> ProjectContext(...)
            ContextType.PERSONAL -> PersonalContext(...)
            // ...
        }
    }
}
```

#### **b) Динамічне завантаження функцій**
```kotlin
class FeatureManager {
    fun getActiveFeatures(context: Context): Set<Feature> {
        val baseFeatures = featureRepository.getByRole(context.roleProfile)
        val overrides = context.roleProfile.featureOverrides
        return baseFeatures.filter { feature ->
            overrides[feature] ?: feature.defaultEnabled
        }.toSet()
    }
}
```

#### **c) Профілі ролей з кастомізацією**
```kotlin
interface RoleProfileCustomizer {
    fun createCustomProfile(
        baseProfile: RoleProfile,
        modifications: ProfileModifications
    ): RoleProfile
    
    fun saveProfileTemplate(profile: RoleProfile, name: String)
}
```

### 4. **Переваги такої архітектури**

1. **Гнучкість**: Легко додавати нові типи контекстів
2. **Кастомізація**: Користувачі можуть створювати власні профілі ролей
3. **Масштабованість**: Можна додавати нові функції без зміни базової структури
4. **Консистентність**: Усі контексти мають спільний інтерфейс
5. **Тестованість**: Легко тестувати через dependency injection

### 5. **Етапи впровадження**

1. **Фаза 1**: Визначити базові типи контекстів (3-5 основних)
2. **Фаза 2**: Реалізувати систему профілів ролей
3. **Фаза 3**: Додати механізм кастомізації
4. **Фаза 4**: Міграція існуючих проектів
5. **Фаза 5**: Додати розширені функції (плагіни, інтеграції)

### 6. **Технічні деталі**

- **База даних**: Використовуйте поліморфні зв'язки (inheritance/table per type)
- **Кешування**: Кешуйте конфігурації контекстів для швидкого доступу
- **Синхронізація**: Реалізуйте синхронізацію конфігурацій між пристроями
- **Бекенд**: REST API з версіонуванням для сумісності

### 7. **Потенційні проблеми та рішення**

1. **Складність міграції**: Поступова міграція з backward compatibility
2. **Продуктивність**: Ліниве завантаження конфігурацій
3. **UX**: Поступове впровадження з чіткими інструкціями

Ця архітектура дозволить створити гнучку систему, де кожен контекст може мати унікальний набір функцій та поведінку, залежно від своєї ролі та конфігурації.

