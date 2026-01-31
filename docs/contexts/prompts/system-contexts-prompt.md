
> **Context:** Ми впроваджуємо "Snapshot Architecture" для системи синхронізації та бекапу.
> **Problem:** Зараз Room-сутності (Entities) напряму використовуються в JSON-бекапах та мережевій синхронізації. Це призводить до розривів (breaking changes) при кожній міграції БД.
> **Solution:** Створити проміжний шар Snapshot DTO.
> **Architectural Rules:**
> 1. **Package by Feature:** Кожна фіча повинна мати пакет `data.snapshot`, де лежать її `Snapshot` класи та методи розширення для мапінгу.
> 2. **Immutability:** Снапшоти — це стабільні контракти. Вони використовують лише примітивні типи (Long для дат, String для Enum-ів).
> 3. **Mapping:** Кожна сутність повинна мати `toSnapshot()` та `toEntity()`.
> 
> 
> **Task:**
> 1. Використовуючи надані класи снапшотів, розмісти їх у відповідних пакетах фіч.
> 2. Створи/онови центральний `SnapshotBundle`, який агрегує всі списки снапшотів.
> 3. Перепиши `SyncLocalService`, щоб він завантажував дані з БД, конвертував у Snapshots і повертав `SnapshotBundle`.
> 4. Онови `MergeRepository`, щоб логіка порівняння та конфліктів працювала виключно на рівні снапшотів.
> 5. Реалізуй підтримку старих бекапів через `LegacyMigrationMapper`.
> 
> 
> **Files provided below:** (Встав сюди снапшоти та файли нижче).

---

## 🛠 Повні версії ключових файлів

### 1. SnapshotBundle.kt

*Це новий "контракт" твого додатка. Він замінює `DatabaseContent`.*

```kotlin
package com.romankozak.forwardappmobile.sync.snapshot

import com.google.gson.annotations.SerializedName

/**
 * Єдиний об'єкт для експорту, імпорту та синхронізації.
 * Версія снапшота (version) дозволяє змінювати БД без зламу сумісності.
 */
data class SnapshotBundle(
    @SerializedName("snapshotVersion")
    val version: Int = 1,
    @SerializedName("exportedAt")
    val exportedAt: Long = System.currentTimeMillis(),
    val contexts: List<ContextSnapshot> = emptyList(),
    val goals: List<GoalSnapshot> = emptyList(),
    val backlogItems: List<BacklogItemSnapshot> = emptyList(),
    val backlogOrders: List<BacklogOrderSnapshot> = emptyList(),
    val notes: List<LegacyNoteSnapshot> = emptyList(),
    val documents: List<NoteDocumentSnapshot> = emptyList(),
    val documentItems: List<NoteDocumentItemSnapshot> = emptyList(),
    val checklists: List<ChecklistSnapshot> = emptyList(),
    val checklistItems: List<ChecklistItemSnapshot> = emptyList(),
    val artifacts: List<ContextArtifactSnapshot> = emptyList(),
    val scripts: List<ScriptSnapshot> = emptyList(),
    val attachments: List<AttachmentSnapshot> = emptyList(),
    val crossRefs: List<ContextAttachmentCrossRefSnapshot> = emptyList(),
    val inbox: List<InboxRecordSnapshot> = emptyList(),
    val logs: List<ContextLogSnapshot> = emptyList(),
    val systemApps: List<SystemAppSnapshot> = emptyList()
)

```

---

### 2. SnapshotMapping.kt (Приклад шаблону)

*Створи такі у кожній фічі. AI-агент заповнить їх за аналогією.*

```kotlin
package com.romankozak.forwardappmobile.features.contexts.data.snapshot

import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.sync.snapshot.GoalSnapshot

/**
 * Mapping extension для ізоляції Room від Sync.
 */
fun Goal.toSnapshot(): GoalSnapshot = GoalSnapshot(
    id = this.id,
    text = this.text,
    description = this.description,
    isCompleted = this.completed,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt ?: this.createdAt,
    version = this.version,
    isDeleted = this.isDeleted,
    tags = this.tags ?: emptyList(),
    scoringStatus = this.scoringStatus,
    valueImportance = this.valueImportance,
    valueImpact = this.valueImpact,
    effort = this.effort,
    cost = this.cost,
    risk = this.risk,
    parentValueImportance = this.parentValueImportance,
    impactOnParentGoal = this.impactOnParentGoal
)

fun GoalSnapshot.toEntity(): Goal = Goal(
    id = this.id,
    text = this.text,
    description = this.description,
    completed = this.isCompleted,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
    tags = this.tags,
    scoringStatus = this.scoringStatus,
    valueImportance = this.valueImportance,
    valueImpact = this.valueImpact,
    effort = this.effort,
    cost = this.cost,
    risk = this.risk,
    parentValueImportance = this.parentValueImportance,
    impactOnParentGoal = this.impactOnParentGoal
)

```

---

### 3. SyncLocalService.kt (Refactored)

*Тепер цей сервіс — це "пакувальник" снапшотів.*

```kotlin
package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.sync.snapshot.*
import com.romankozak.forwardappmobile.features.contexts.data.snapshot.*
import com.romankozak.forwardappmobile.features.attachments.data.snapshot.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLocalService @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun createFullSnapshotBundle(): SnapshotBundle {
        // Завантажуємо все з БД паралельно або послідовно
        return SnapshotBundle(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            contexts = db.contextDao().getAllRaw().map { it.toSnapshot() },
            goals = db.goalDao().getAllRaw().map { it.toSnapshot() },
            backlogItems = db.listItemDao().getAllRaw().map { it.toSnapshot() },
            backlogOrders = db.backlogOrderDao().getAllRaw().map { it.toSnapshot() },
            notes = db.legacyNoteDao().getAllRaw().map { it.toSnapshot() },
            documents = db.noteDocumentDao().getAllDocumentsRaw().map { it.toSnapshot() },
            // ... додай всі інші сутності за аналогією
        )
    }

    suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
        db.withTransaction {
            // Викликаємо відповідні DAO, конвертуючи снапшоти назад в Entity
            bundle.goals.map { it.toEntity() }.let { db.goalDao().insertAll(it) }
            bundle.contexts.map { it.toEntity() }.let { db.contextDao().insertAll(it) }
            // ...
        }
    }
}

```

---

### План впровадження (для Агента):

1. **Phase 1: DTO Setup** — Розмістити всі 15+ класів Snapshot у відповідні `data.snapshot` пакети.
2. **Phase 2: Mapping Layer** — Написати методи `toSnapshot()` / `toEntity()` для кожної Room-сутності. Це "найнудніша" частина, AI зробить її за 1 хвилину.
3. **Phase 3: Central Bundle** — Замінити `DatabaseContent` на `SnapshotBundle` у всіх інтерфейсах.
4. **Phase 4: Merge Logic Update** — В `MergeRepository` змінити типи аргументів. Замість `List<Goal>` тепер `List<GoalSnapshot>`. Логіка конфліктів залишається тією ж (LWW), але вона тепер працює зі стабільними полями.
5. **Phase 5: Cleanup** — Видалити старі `Deserializers` (як-от `GoalDeserializer`), оскільки Snapshot JSON буде стандартним і не потребуватиме складних адаптерів.

---

перечитай все в папці документації щодо синхронізації.  памятай що дещо там може невідповідати реальності
переконайся що для всіх сутностей які зараз в синхронізації,є відповідні дата класи снапшотів. синхронізація має працювати старим способом (як зараз) і новим. для зворотної сумісності поки що. можна навіть в ui зробити нові дії поряд зі старими імпортом-експортом в файл повного бекапу. wifi синхронізація мене зараз хвилює не сильно. головне робота з файловими повними бекапами

> май на увазі що в запиті модулі і пакети, можуть вказувати невірно бо модель яка його писала не знає про поточну архітектуру, зокрема di

  forwardapp-android on  dev [$✘?] via 🅶 v8.13 via ☕ v17.0.11 via 🅺 via 🐍 v3.14.2 on ☁️  phoenix.way.rising@gmail.com took 56s
  ❯ fd napshot
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/AttachmentSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/ChecklistItemSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/ChecklistSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/ContextAttachmentCrossRefSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/LegacyNoteSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/NoteDocumentItemSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/NoteDocumentSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/attachments/ScriptSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/BacklogItemSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/BacklogOrderSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextArtifactSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextConfigurationSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextLogSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextRoleProfileItemSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextRoleProfileSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/ContextStructureItemSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/GoalSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/InboxRecordSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/LinkItemSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/RelatedLinkSnapshot.kt
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/SystemAppSnapshot
  sync/src/main/java/com/romankozak/forwardappmobile/sync/snapshot/entities/context/SystemAppSnapshot.kt