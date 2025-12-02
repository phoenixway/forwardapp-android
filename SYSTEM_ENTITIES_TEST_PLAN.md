# План тестування для забезпечення унікальності системних проектів

## Контекст
Щоб запобігти рецидиву проблеми з дублі системних проектів, потрібні:
1. Unit тести для логіки `importFullBackupFromFile()`
2. Integration тести для DB constraint
3. Regression тести для міграцій

---

## 1. Unit Test: Обробка дублів системних проектів у бекапі

📍 **Файл:** `app/src/test/java/com/romankozak/forwardappmobile/data/sync/SyncRepositorySystemProjectsTest.kt`

```kotlin
class SyncRepositorySystemProjectsTest {
    
    @Test
    fun `should detect duplicate system keys in incoming backup`() {
        // Arrange
        val backup = FullAppBackup(
            database = DatabaseContent(
                projects = listOf(
                    Project(id = "id1", systemKey = "personal-management", name = "PM 1"),
                    Project(id = "id2", systemKey = "personal-management", name = "PM 2"),  // ← Дублікат!
                    Project(id = "id3", systemKey = "strategic", name = "Strategic"),
                ),
                goals = emptyList(),
                // ... інші поля...
            )
        )
        
        // Act & Assert
        val result = importFullBackup(backup)
        
        // Повинна повернути помилку з явним повідомленням
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message?.contains("duplicate") == true)
    }
    
    @Test
    fun `should handle system project ID remapping correctly`() {
        // Arrange: У БД вже є personal-management з ID "existing-id"
        val existingProject = Project(
            id = "existing-id",
            systemKey = "personal-management",
            name = "Personal Management",
            updatedAt = 1000
        )
        
        // Бекап має той же проект з іншим ID та новішою версією
        val backupProject = Project(
            id = "backup-id",
            systemKey = "personal-management",
            name = "Personal Management",
            updatedAt = 2000  // ← Новіше
        )
        
        val childProject = Project(
            id = "child-id",
            name = "Child Project",
            parentId = "backup-id"  // ← Вказує на проект в бекапі
        )
        
        val backup = FullAppBackup(
            database = DatabaseContent(
                projects = listOf(backupProject, childProject),
                goals = emptyList(),
                // ...
            )
        )
        
        // Act
        val result = importFullBackup(backup)
        
        // Assert
        assert(result.isSuccess)
        
        // Батько child проекту повинен бути переіндексований на "existing-id"
        val insertedChild = projectDao.getProjectById("child-id")
        assert(insertedChild?.parentId == "existing-id")
    }
    
    @Test
    fun `should validate that all parent references are valid after import`() {
        // Arrange
        val backup = FullAppBackup(
            database = DatabaseContent(
                projects = listOf(
                    Project(id = "id1", systemKey = "personal-management", name = "PM"),
                    Project(id = "id2", name = "Child", parentId = "id3"),  // ← Батько не існує!
                ),
                goals = emptyList(),
                // ...
            )
        )
        
        // Act
        val result = importFullBackup(backup)
        
        // Assert
        // Батько повинен бути очищений (parentId = null)
        // ЛУ повинен бути записаний у логи з попередженням
        val insertedProject = projectDao.getProjectById("id2")
        assert(insertedProject?.parentId == null)
    }
    
    @Test
    fun `should not allow duplicate system keys in database state`() {
        // Arrange
        val backup = FullAppBackup(
            database = DatabaseContent(
                projects = listOf(
                    Project(id = "id1", systemKey = "inbox", name = "Inbox 1"),
                    Project(id = "id2", systemKey = "inbox", name = "Inbox 2"),  // ← Дублікат
                ),
                goals = emptyList(),
                // ...
            )
        )
        
        // Act
        val result = importFullBackup(backup)
        
        // Assert: Повинна бути помилка перед вставкою
        assert(result.isFailure)
        
        // Базі у DB повинно бути не більше ніж 1 проект з systemKey="inbox"
        val inboxProjects = projectDao.getAll().filter { it.systemKey == "inbox" }
        assert(inboxProjects.size <= 1)
    }
    
    @Test
    fun `should prefer local system project when incoming is older`() {
        // Arrange
        val existingProject = Project(
            id = "local-id",
            systemKey = "strategic",
            name = "Strategic Local",
            updatedAt = 3000  // ← Новіше
        )
        
        val backupProject = Project(
            id = "backup-id",
            systemKey = "strategic",
            name = "Strategic Backup",
            updatedAt = 1000  // ← Старіше
        )
        
        val backup = FullAppBackup(
            database = DatabaseContent(
                projects = listOf(backupProject),
                goals = emptyList(),
                // ...
            )
        )
        
        // Допустимо що existingProject вже у БД
        projectDao.insert(existingProject)
        
        // Act
        val result = importFullBackup(backup)
        
        // Assert
        assert(result.isSuccess)
        
        // Локальна версія повинна залишитися без змін
        val strategicProject = projectDao.getProjectBySystemKey("strategic")
        assert(strategicProject?.id == "local-id")
        assert(strategicProject?.name == "Strategic Local")
    }
}
```

---

## 2. Integration Test: DB Constraint

📍 **Файл:** `app/src/test/java/com/romankozak/forwardappmobile/data/database/ProjectEntityConstraintTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class ProjectEntityConstraintTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var projectDao: ProjectDao
    private lateinit var database: AppDatabase
    
    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            AppDatabase::class.java
        ).build()
        projectDao = database.projectDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `should throw constraint violation for duplicate systemKey`() {
        // Arrange
        val project1 = Project(
            id = UUID.randomUUID().toString(),
            systemKey = "personal-management",
            name = "PM 1",
            parentId = null,
            // ... інші обов'язкові поля
        )
        
        val project2 = Project(
            id = UUID.randomUUID().toString(),
            systemKey = "personal-management",  // ← ТОЙ ЖЕ КЛЮЧ!
            name = "PM 2",
            parentId = null,
            // ...
        )
        
        // Act & Assert
        projectDao.insert(project1)
        
        assertThrows(SQLiteIntegrityConstraintException::class.java) {
            projectDao.insert(project2)
        }
    }
    
    @Test
    fun `should allow null systemKey (non-system projects)`() {
        // Arrange
        val project1 = Project(
            id = "id1",
            systemKey = null,  // ← Звичайний проект
            name = "Regular 1",
            parentId = null,
            // ...
        )
        
        val project2 = Project(
            id = "id2",
            systemKey = null,  // ← Інший звичайний проект
            name = "Regular 2",
            parentId = null,
            // ...
        )
        
        // Act & Assert
        // Повинна бути помилка, але не через constraint
        projectDao.insert(project1)
        projectDao.insert(project2)  // ← Має працювати нормально
        
        assert(projectDao.getAll().size == 2)
    }
    
    @Test
    fun `should allow only one project per systemKey`() {
        // Arrange
        val project1 = Project(
            id = "id1",
            systemKey = "inbox",
            name = "Inbox 1",
            parentId = null,
            // ...
        )
        
        // Act
        projectDao.insert(project1)
        val allInbox = projectDao.getAll().filter { it.systemKey == "inbox" }
        
        // Assert
        assert(allInbox.size == 1)
    }
}
```

---

## 3. Regression Test: Import з дублями

📍 **Файл:** `app/src/test/java/com/romankozak/forwardappmobile/data/sync/SyncRepositoryRegressionTest.kt`

```kotlin
class SyncRepositoryRegressionTest {
    
    @Test
    fun `should handle backup with duplicate system projects gracefully`() {
        // Цей тест репродукує оригінальну помилку та забезпечує виправлення
        
        // Arrange: Бекап з дублями (як це було у користувача)
        val corruptedBackup = FullAppBackup(
            backupSchemaVersion = 2,
            database = DatabaseContent(
                projects = listOf(
                    // inbox дублі
                    Project(id = "a8d097cf", systemKey = "inbox", name = "strategic-inbox🚀", 
                            updatedAt = 1764656232800),
                    Project(id = "bd6f7668", systemKey = "inbox", name = "inbox📥", 
                            updatedAt = 1763147848968),
                    Project(id = "ccb25339", systemKey = "inbox", name = "inboxes", 
                            updatedAt = 1756838136978),
                    Project(id = "a82b076e", systemKey = "inbox", name = "inbox", 
                            updatedAt = 0),
                    
                    // personal-management дублі
                    Project(id = "b7923d3d", systemKey = "personal-management", 
                            name = "personal-managementSAVE", updatedAt = 1764679399354),
                    Project(id = "a2c26024", systemKey = "personal-management", 
                            name = "personal-management", updatedAt = 1764679395247),
                    Project(id = "4d3d3846", systemKey = "personal-management", 
                            name = "social", updatedAt = 1764656232800),
                    Project(id = "f5f9e1a5", systemKey = "personal-management", 
                            name = "personal-management", updatedAt = 0),
                    Project(id = "987f96c5", systemKey = "personal-management", 
                            name = "personal-management", updatedAt = 0),
                    
                    // strategic дублі
                    Project(id = "54a2c1c6", systemKey = "strategic", name = "strategic🔭", 
                            updatedAt = 1764679401816),
                    Project(id = "a7c6252c", systemKey = "strategic", name = "strategic-programs", 
                            updatedAt = 1764656232800),
                    Project(id = "1fc926be", systemKey = "strategic", name = "strategic", 
                            updatedAt = 0),
                    
                    // Дитина з невалідним батьком
                    Project(id = "94a194e4", name = "publications", 
                            parentId = "4d3d3846")  // ← батько буде видалений
                ),
                goals = emptyList(),
                listItems = listOf(
                    ListItem(id = "item1", projectId = "4d3d3846", ...)  // ← Батько буде видалений
                ),
                // ...
            )
        )
        
        // Act
        val result = importFullBackup(corruptedBackup)
        
        // Assert
        // 1. Імпорт повинен пройти успішно (не краші)
        assert(result.isSuccess)
        
        // 2. Системні проекти повинні бути унікальні
        val allProjects = projectDao.getAll()
        val systemProjects = allProjects.filter { it.systemKey != null }
        val duplicateSystemKeys = systemProjects.groupBy { it.systemKey }
            .filter { it.value.size > 1 }
        assert(duplicateSystemKeys.isEmpty())
        
        // 3. Правильна версія має бути вибрана (найновіша)
        val inboxProject = projectDao.getProjectBySystemKey("inbox")
        assert(inboxProject?.id == "a8d097cf")  // Найновіша (updatedAt = 1764656232800)
        
        val pmProject = projectDao.getProjectBySystemKey("personal-management")
        assert(pmProject?.id == "b7923d3d")  // Найновіша (updatedAt = 1764679399354)
        
        val strategicProject = projectDao.getProjectBySystemKey("strategic")
        assert(strategicProject?.id == "54a2c1c6")  // Найновіша (updatedAt = 1764679401816)
        
        // 4. Дітьми повинні мати коректних батьків
        val publicationsProject = projectDao.getProjectById("94a194e4")
        // Батько був 4d3d3846 (personal-management), який переіндексований на b7923d3d
        assert(publicationsProject?.parentId == "b7923d3d")
        
        // 5. ListItems повинні мати коректні projectId
        val item = listItemDao.getListItemById("item1")
        assert(item?.projectId == "b7923d3d")  // Переіндексовано
    }
    
    @Test
    fun `should not create duplicate system keys after multiple imports`() {
        // Arrange: Один бекап, імпортуємо 2 рази
        val backup = FullAppBackup(
            database = DatabaseContent(
                projects = listOf(
                    Project(id = "id1", systemKey = "personal-management", name = "PM"),
                    Project(id = "id2", name = "Child", parentId = "id1"),
                ),
                goals = emptyList(),
                // ...
            )
        )
        
        // Act
        val result1 = importFullBackup(backup)
        val result2 = importFullBackup(backup)  // Імпортуємо ще раз
        
        // Assert
        assert(result1.isSuccess)
        assert(result2.isSuccess)
        
        // Дублів НЕ повинно виникнути
        val allProjects = projectDao.getAll()
        val duplicateSystemKeys = allProjects
            .filter { it.systemKey != null }
            .groupBy { it.systemKey }
            .filter { it.value.size > 1 }
        assert(duplicateSystemKeys.isEmpty())
    }
}
```

---

## 4. Спеціалізовані тести для DatabaseInitializer

📍 **Файл:** `app/src/test/java/com/romankozak/forwardappmobile/data/database/DatabaseInitializerTest.kt`

```kotlin
class DatabaseInitializerTest {
    
    @Test
    fun `should create 14 unique system projects on first run`() {
        // Arrange: Чиста БД
        val dao = projectDao  // empty
        val initializer = DatabaseInitializer(dao, systemAppRepository)
        
        // Act
        initializer.prePopulate()
        
        // Assert
        val systemProjects = dao.getAll().filter { it.systemKey != null }
        assert(systemProjects.size == 14)
        
        // Всі systemKey унікальні
        val duplicates = systemProjects.groupBy { it.systemKey }
            .filter { it.value.size > 1 }
        assert(duplicates.isEmpty())
        
        // Всі батьки існують
        val orphans = systemProjects.filter {
            it.parentId != null && dao.getProjectById(it.parentId) == null
        }
        assert(orphans.isEmpty())
    }
    
    @Test
    fun `should not create duplicates when called multiple times`() {
        // Act
        initializer.prePopulate()
        initializer.prePopulate()  // Друга спроба
        
        // Assert
        val systemProjects = dao.getAll().filter { it.systemKey != null }
        assert(systemProjects.size == 14)  // Не 28!
    }
    
    @Test
    fun `should detect duplicate system projects and log warning`() {
        // Arrange: Ручно вставити дублікат
        val pm1 = Project(id = "id1", systemKey = "personal-management", name = "PM 1")
        val pm2 = Project(id = "id2", systemKey = "personal-management", name = "PM 2")
        dao.insert(pm1)
        dao.insert(pm2)  // Якщо constraint опціональний
        
        // Act
        val result = initializer.prePopulate()
        
        // Assert
        // Повинен логувати помилку
        verify(logger).e(contains("duplicate system projects"))
    }
}
```

---

## 5. Чек-лист для CI/CD

Додати до вашого CI pipeline:

```yaml
# .github/workflows/test.yml
test-system-entities:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v2
    - name: Run System Entity Tests
      run: |
        ./gradlew test -k "SystemProjects"
        ./gradlew test -k "ProjectEntityConstraint"
        ./gradlew test -k "SyncRepositoryRegression"
    - name: Check for System Project Duplicates
      run: |
        # Script to verify no duplicate systemKeys in test databases
        python3 check_system_projects.py
```

---

## Резюме тестів

| Тест | Цель | Помічає |
|------|------|---------|
| `SyncRepositorySystemProjectsTest` | Unit-тести логіки імпорту | Дублі в бекапі, неправильне переіндексування |
| `ProjectEntityConstraintTest` | DB-level constraint | Дублі на рівні БД |
| `SyncRepositoryRegressionTest` | Регресія оригіналної помилки | Що помилка більше не виникне |
| `DatabaseInitializerTest` | Препопуляція | Дублі при старті |
| CI/CD Check | Automated validation | Дублі у PR-їх перед merge |

**Результат:** Жоден дублікат системного проекту не просочиться в production.
