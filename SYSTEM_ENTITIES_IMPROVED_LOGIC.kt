/**
 * ПОСИЛЕНА ЛОГІКА ОБРОБКИ СИСТЕМНИХ ПРОЕКТІВ ДЛЯ importFullBackupFromFile()
 * 
 * Це коду заміняє лінії 437-546 в SyncRepository.kt
 * та додає нову логіку переіндексації системних проектів.
 */

// ============================================================================
// КРОК 1: Перевірка цілісності БД ДО ІМПОРТУ
// ============================================================================

Log.d(IMPORT_TAG, "=== КРОК 1: Перевірка цілісності БД до імпорту ===")
val dbSystemProjects = projectDao.getAll().filter { it.systemKey != null }
val dbDuplicatesByKey = dbSystemProjects.groupBy { it.systemKey }
val dbDuplicateKeys = dbDuplicatesByKey.filter { it.value.size > 1 }.keys

if (dbDuplicateKeys.isNotEmpty()) {
    val message = "CRITICAL: Database already has duplicate system keys: $dbDuplicateKeys. " +
        "This violates system project invariants. Please reset database and reimport a clean backup."
    Log.e(IMPORT_TAG, message)
    return Result.failure(Exception(message))
}
Log.d(IMPORT_TAG, "✅ DB системні проекти унікальні (${dbSystemProjects.size} проектів)")

// ============================================================================
// КРОК 2: Будування маппінгу системних проектів
// ============================================================================

Log.d(IMPORT_TAG, "=== КРОК 2: Будування маппінгу системних проектів ===")

val existingSystemProjectsByKey = dbSystemProjects.associateBy { it.systemKey!! }
Log.d(IMPORT_TAG, "Знайдено ${existingSystemProjectsByKey.size} системних проектів у БД")

// Перевірити дублі в бекапі
val backupSystemProjects = backup.projects.filter { it.systemKey != null }
val backupDuplicatesByKey = backupSystemProjects.groupBy { it.systemKey }
val backupDuplicateKeys = backupDuplicatesByKey.filter { it.value.size > 1 }.keys

if (backupDuplicateKeys.isNotEmpty()) {
    Log.w(IMPORT_TAG, "WARNING: Backup has duplicate system keys (це обробимо): $backupDuplicateKeys")
    // Вибираємо "правильну" версію для кожного дублювального ключа
    val cleanedBackupSystem = mutableMapOf<String, Project>()
    backupDuplicatesByKey.forEach { (key, duplicates) ->
        val chosen = duplicates.maxByOrNull { it.updatedAt ?: 0 } ?: duplicates.first()
        cleanedBackupSystem[key] = chosen
        Log.d(IMPORT_TAG, "  Система проект '$key': Вибрано ${chosen.name} (${chosen.id}), видалено ${duplicates.size - 1} дублів")
    }
    // Замінити дублі в бекапі на вибрані версії
    backup.projects = backup.projects.filter { proj ->
        if (proj.systemKey != null && proj.systemKey in backupDuplicateKeys) {
            proj.id == cleanedBackupSystem[proj.systemKey]?.id
        } else {
            true
        }
    }
}

// ============================================================================
// КРОК 3: Розрахунок ID-маппінгу для системних проектів
// ============================================================================

Log.d(IMPORT_TAG, "=== КРОК 3: Розрахунок ID-маппінгу системних проектів ===")

// projectIdMap: backupId -> actualId
val projectIdMap = mutableMapOf<String, String>()

val cleanedProjects = backup.projects.map { projectFromBackup ->
    val normalizedIncoming = projectFromBackup.copy(
        projectType = projectFromBackup.projectType ?: ProjectType.DEFAULT,
        reservedGroup = ReservedGroup.fromString(projectFromBackup.reservedGroup?.groupName),
        defaultViewModeName = projectFromBackup.defaultViewModeName ?: ProjectViewMode.BACKLOG.name,
        isProjectManagementEnabled = projectFromBackup.isProjectManagementEnabled ?: false,
        projectStatus = projectFromBackup.projectStatus ?: ProjectStatusValues.NO_PLAN,
        projectStatusText = projectFromBackup.projectStatusText ?: "",
        projectLogLevel = projectFromBackup.projectLogLevel ?: ProjectLogLevelValues.NORMAL,
        totalTimeSpentMinutes = projectFromBackup.totalTimeSpentMinutes ?: 0,
        scoringStatus = projectFromBackup.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
        valueImportance = projectFromBackup.valueImportance,
        valueImpact = projectFromBackup.valueImpact,
        effort = projectFromBackup.effort,
        cost = projectFromBackup.cost,
        risk = projectFromBackup.risk,
        weightEffort = projectFromBackup.weightEffort,
        weightCost = projectFromBackup.weightCost,
        weightRisk = projectFromBackup.weightRisk,
        rawScore = projectFromBackup.rawScore,
        displayScore = projectFromBackup.displayScore,
    )

    val systemKey = normalizedIncoming.systemKey
    val existingSystemProject = systemKey?.let { existingSystemProjectsByKey[it] }
    
    if (existingSystemProject != null) {
        // Система проект: порівнюємо версії
        val incomingUpdated = normalizedIncoming.updatedAt ?: 0
        val existingUpdated = existingSystemProject.updatedAt ?: 0
        
        // Записуємо маппінг якщо ID різні
        if (normalizedIncoming.id != existingSystemProject.id) {
            projectIdMap[normalizedIncoming.id] = existingSystemProject.id
            Log.d(IMPORT_TAG, "  System '$systemKey': маппінг ${normalizedIncoming.id} -> ${existingSystemProject.id}")
        }
        
        // LWW (Last-Write-Wins) логіка
        if (incomingUpdated > existingUpdated) {
            Log.d(IMPORT_TAG, "  System '$systemKey': оновлюємо (incoming=$incomingUpdated > existing=$existingUpdated)")
            normalizedIncoming.copy(id = existingSystemProject.id)  // ← ВАЖЛИВО: зберігаємо існуючий ID!
        } else {
            Log.d(IMPORT_TAG, "  System '$systemKey': залишаємо локальну (existing=$existingUpdated >= incoming=$incomingUpdated)")
            existingSystemProject
        }
    } else {
        // Новий проект (системний або звичайний)
        normalizedIncoming
    }
}

// ============================================================================
// КРОК 4: Валідація та очищення parentId
// ============================================================================

Log.d(IMPORT_TAG, "=== КРОК 4: Валідація та очищення parentId ===")

val projectIdsSet = cleanedProjects.map { it.id }.toSet()
Log.d(IMPORT_TAG, "Всього проектів після нормалізації: ${cleanedProjects.size}")

// Будуємо маппу systemKey -> actualId для швидкого пошуку
val systemKeyToActualId = mutableMapOf<String, String>()
cleanedProjects.forEach { proj ->
    if (proj.systemKey != null) {
        systemKeyToActualId[proj.systemKey!!] = proj.id
    }
}

// Функція для правильної переіндексації батьків системних проектів
val remapParentId: (String?) -> String? = { parentId ->
    parentId?.let { pid ->
        when {
            // 1. Спочатку перевірити прямий маппінг ID
            pid in projectIdMap -> {
                val mappedId = projectIdMap[pid]!!
                // 2. Якщо помапнений проект є системним, отримати його актуальний ID
                val mappedProj = cleanedProjects.find { it.id == mappedId }
                if (mappedProj?.systemKey != null) {
                    systemKeyToActualId[mappedProj.systemKey!!] ?: mappedId
                } else {
                    mappedId
                }
            }
            // 3. Якщо ID є в наборі — залишити як є
            pid in projectIdsSet -> pid
            // 4. Інакше — null (батько не існує)
            else -> null
        }
    }
}

var projectsCleaned = 0
val cleanedProjectsWithParents = cleanedProjects.map { proj ->
    val mappedParent = remapParentId(proj.parentId)
    
    if (mappedParent == null && proj.parentId != null) {
        projectsCleaned++
        Log.w(IMPORT_TAG, "  Очищення: ${proj.id} (${proj.name}) батько ${proj.parentId} не існує")
        proj.copy(parentId = null)
    } else if (mappedParent != proj.parentId) {
        Log.d(IMPORT_TAG, "  Переіндексація: ${proj.id} батько ${proj.parentId} -> $mappedParent")
        proj.copy(parentId = mappedParent)
    } else {
        proj
    }
}

Log.d(IMPORT_TAG, "✅ Очищено проектів: $projectsCleaned, всього батьків: ${cleanedProjectsWithParents.count { it.parentId != null }}")

// ============================================================================
// КРОК 5: ВАЛІДАЦІЯ ПЕРЕД ВСТАВКОЮ (КРИТИЧНЕ)
// ============================================================================

Log.d(IMPORT_TAG, "=== КРОК 5: Валідація цілісності перед вставкою ===")

val finalProjectIds = cleanedProjectsWithParents.map { it.id }.toSet()

// 5A. Перевірити що немає orphan projects
val orphans = cleanedProjectsWithParents.filter { 
    it.parentId != null && it.parentId !in finalProjectIds 
}
if (orphans.isNotEmpty()) {
    val message = "ABORT: Found ${orphans.size} projects with invalid parents after remapping: " +
        orphans.take(5).joinToString { "${it.name}(${it.id})->${it.parentId}" }
    Log.e(IMPORT_TAG, message)
    return Result.failure(Exception(message))
}
Log.d(IMPORT_TAG, "✅ Батьки: всі валідні (${cleanedProjectsWithParents.count { it.parentId != null }} з дітьми)")

// 5B. Перевірити що системні проекти унікальні
val finalSystemProjects = cleanedProjectsWithParents.filter { it.systemKey != null }
val duplicateSystemKeys = finalSystemProjects.groupBy { it.systemKey }
    .filter { it.value.size > 1 }
    .keys
if (duplicateSystemKeys.isNotEmpty()) {
    val message = "ABORT: Found duplicate systemKeys after processing: $duplicateSystemKeys"
    Log.e(IMPORT_TAG, message)
    return Result.failure(Exception(message))
}
Log.d(IMPORT_TAG, "✅ Системні проекти: унікальні ($finalSystemProjects.size} системних ключів)")

// 5C. Перевірити що немає "мертвих" посилань в listItems
val finalListItems = backup.listItems
    .map { item ->
        val mappedProjectId = remapParentId(item.projectId)
        if (mappedProjectId == null) {
            Log.w(IMPORT_TAG, "  ListItem ${item.id}: батько не існує, буде видалений")
            null
        } else {
            item.copy(projectId = mappedProjectId)
        }
    }
    .filterNotNull()

Log.d(IMPORT_TAG, "✅ ListItems: очищено ${backup.listItems.size - finalListItems.size}, залишилось ${finalListItems.size}")

// ============================================================================
// КРОК 6: Аналогічна обробка для інших сутностей з projectId
// ============================================================================

Log.d(IMPORT_TAG, "=== КРОК 6: Переіндексація інших сутностей ===")

// NoteDocuments
val finalNoteDocuments = backup.documents.map {
    it.copy(projectId = remapParentId(it.projectId) ?: it.projectId)
}.filter { it.projectId in finalProjectIds }

// Checklists
val finalChecklists = backup.checklists.map {
    it.copy(projectId = remapParentId(it.projectId) ?: it.projectId)
}.filter { it.projectId in finalProjectIds }

// InboxRecords
val finalInboxRecords = backup.inboxRecords.map {
    it.copy(projectId = remapParentId(it.projectId) ?: it.projectId)
}.filter { it.projectId in finalProjectIds }

// ProjectLogs
val finalProjectLogs = backup.projectExecutionLogs.map {
    it.copy(projectId = remapParentId(it.projectId) ?: it.projectId)
}.filter { it.projectId in finalProjectIds }

// Attachments
val finalAttachments = backup.attachments.map { att ->
    att.copy(ownerProjectId = att.ownerProjectId?.let { remapParentId(it) })
}.filter { it.ownerProjectId == null || it.ownerProjectId in finalProjectIds }

// ProjectAttachmentCrossRefs
val finalCrossRefs = backup.projectAttachmentCrossRefs.map {
    it.copy(projectId = remapParentId(it.projectId) ?: it.projectId)
}.filter { it.projectId in finalProjectIds }

Log.d(IMPORT_TAG, """
✅ Переіндексація завершена:
  - ListItems: ${backup.listItems.size} -> ${finalListItems.size}
  - NoteDocuments: ${backup.documents.size} -> ${finalNoteDocuments.size}
  - Checklists: ${backup.checklists.size} -> ${finalChecklists.size}
  - InboxRecords: ${backup.inboxRecords.size} -> ${finalInboxRecords.size}
  - ProjectLogs: ${backup.projectExecutionLogs.size} -> ${finalProjectLogs.size}
  - Attachments: ${backup.attachments.size} -> ${finalAttachments.size}
  - CrossRefs: ${backup.projectAttachmentCrossRefs.size} -> ${finalCrossRefs.size}
""")

// ============================================================================
// КРОК 7: Вставка в БД (бази для трансакції)
// ============================================================================

// Замість старого cleanedProjectsWithParents використовуємо cleanedProjectsWithParents
// Замість старих backup.listItems використовуємо finalListItems
// Замість старих backup.documents використовуємо finalNoteDocuments
// тощо...

Log.d(IMPORT_TAG, "=== КРОК 7: Вставка в базу даних ===")
Log.d(IMPORT_TAG, "Читай комітування даних в основній транзакції")

// Продовження має використовувати finalCleanedProjects, finalListItems, тощо замість оригіналів
