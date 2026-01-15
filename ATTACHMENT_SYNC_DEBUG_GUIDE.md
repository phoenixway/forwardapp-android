# Attachment Sync Debug Guide

## Проблеми у синхронізації вкладень

### Прояви проблеми:
1. **Втрата вкладень при синхронізації**: 106 вкладень → 12 вкладень
2. **Нові вкладення не синхронізуються**: нотатка створена на Android не з'являється на desktop і пропадає
3. **Асиметрична синхронізація**: notes на desktop не видно на Android
4. **Desktop взагалі НЕ експортує вкладення**

---

## 3 Критичні дефекти у коді

### ДЕФЕКТ #1: Коли desktop експортує `attachments=0`
**Місце:** `SyncRepository.applyServerChanges()`, рядки 817-866

**Проблема:**
- Коли desktop не має вкладень (або експортує пусту колекцію), Android код **ігнорує локальні вкладення**
- Локальні unsynced вкладення залишаються в БД, але:
  - Вони не позначаються як `synced` (залишаються з `syncedAt=null`)
  - При наступній синхронізації вони знову експортуються
  - Але на desktop вони не потраплять (тому що desktop їх не експортує)

**Симптом:** 
```
applyServerChanges] Incoming attachments count: 0. If 0, this indicates desktop didn't export them.
```

**Поточне логування:**
```kotlin
Log.d(WIFI_SYNC_LOG_TAG, "[applyServerChanges] Processing attachments. Total incoming: ${correctedChanges.attachments.size}")
```

**Нове логування (ДОДАНО):**
- Логування стану **локальних** attachment ДО обробки incoming
- Кількість synced vs unsynced локально
- WARNING коли incoming==0 але local.unsynced>0

---

### ДЕФЕКТ #2: Desktop не експортує вкладення взагалі
**Місце:** `SyncRepository.createAttachmentsBackupJsonString()`, рядки 237-283

**Проблема:**
- Цей метод експортує вкладення, але:
  - Мав повну логірування (для debugging)
  - **НЕ включений в Wi-Fi `/export` endpoint** (потрібна перевірка)

**Поточна ситуація:**
- На Android: `createDeltaBackupJsonString()` → `getChangesSince()` вкоротяє attachments
- На desktop: **невідомо, як вкладення експортуються/імпортуються**

**Нове логування (ДОДАНО):**
```kotlin
if (changes.attachments.isEmpty() && allLocalAttachments.isNotEmpty()) {
    Log.w(WIFI_SYNC_LOG_TAG, "[createDeltaBackupJsonString] DEFECT #2 DETECTED: 
    Exporting 0 attachments but ${allLocalAttachments.size} exist locally.")
}
```

---

### ДЕФЕКТ #3: Нові вкладення на Android не експортуються на desktop
**Місце:** `SyncRepository.getChangesSince()`, рядки 1262-1283

**Проблема:**
- Коли вкладення **новоствореним** на Android (синхронізовано з desktop **НОВИМ**):
  - Мають `syncedAt = null` (не синхронізовані)
  - Мають `updatedAt = createdAt` (поточний час)
  - Повинні експортуватися як `unsync'd` attachments
  - Але логування не показує деталі

**Симптом:**
1. Тап на нотатку Android → додати вкладення (link) → нотатка не синхронізується
2. Нотатка локально залишається, але на desktop вона не з'являється

**Дефект в логіці:**
- `attachmentsUnsync = local.attachments.filter { it.syncedAt == null }` ✓
- Це правильно, але логування не показує ці unsynced attachments окремо

**Нове логування (ДОДАНО):**
```kotlin
if (attachmentsUnsync.isNotEmpty()) {
    Log.d(WIFI_SYNC_LOG_TAG, "[getChangesSince] Found ${attachmentsUnsync.size} unsynced attachments")
    attachmentsUnsync.take(5).forEach {
        Log.d(WIFI_SYNC_LOG_TAG, "  [EXPORT-UNSYNC] Attachment: id=${it.id}, 
        type=${it.attachmentType}, entity=${it.entityId}, version=${it.version}")
    }
}
```

---

## Повний Sync Flow для вкладень

```
CREATE ATTACHMENT (Android)
    ↓
[createLinkAttachment() in AttachmentRepository]
    - LinkItemEntity created: syncedAt=null
    - AttachmentEntity created: syncedAt=null
    - ProjectAttachmentCrossRef created: syncedAt=null
    ↓
EXPORT ATTACHMENTS
    ↓
[WifiSyncManager.performWifiPush() or performWifiImport()]
    ↓
[SyncRepository.getChangesSince(lastSyncTime)]
    - Collect all attachments where syncedAt=null OR updatedAt > lastSyncTime
    - Filter by projectIds (valid projects)
    ↓
[WifiSyncServer /export endpoint]
    - Sends AttachmentsBackup JSON
    ↓
DESKTOP RECEIVES & MERGES
    ↓
[WifiSyncServer /import endpoint]
    - POST data with attachment changes
    ↓
[SyncRepository.applyServerChanges(incoming)]
    - Merge with LWW (Last-Write-Wins) strategy
    - Call markSyncedNow() for synced items
    ↓
[SyncRepository.markSyncedNow(content)]
    - AttachmentEntity.copy(syncedAt = ts)
    - ProjectAttachmentCrossRef.copy(syncedAt = ts)
    - Update in DB
    ↓
DONE - Attachments now marked as synced
```

---

## Як читати логи для дебагу

### 1. Коли створюємо вкладення на Android:
Шукай логи з тегом **`FWD_ATTACH`**:
```
[createLinkAttachment] START: project=proj-123, link=https://...
[createLinkAttachment] STEP1: LinkItemEntity created: id=link-456, syncedAt=null (NEW)
[createLinkAttachment] STEP2: AttachmentEntity created: id=att-789, syncedAt=null (NEW)
[createLinkAttachment] STEP3: ProjectAttachmentCrossRef created: syncedAt=null (NEW)
```

### 2. Коли експортуємо (push на desktop):
Шукай логи з тегом **`FWD_SYNC_TEST`**:

**getChangesSince():**
```
[getChangesSince] since=1234567890 (yyyy-MM-dd HH:mm:ss)
[getChangesSince] Attachments: total=106, unsync'd=5, updated=0, result=5
[getChangesSince] DEFECT #3 INFO: Found 5 unsynced attachments that WILL be exported
  [EXPORT-UNSYNC] Attachment: id=att-789, type=LINK_ITEM, entity=link-456, ...
```

**createDeltaBackupJsonString():**
```
[createDeltaBackupJsonString] deltaSince=1234567890, attachments=5, crossRefs=5
```

### 3. Коли імпортуємо (pull від desktop):
**applyServerChanges():**
```
[applyServerChanges] Processing attachments. Total incoming: 0
[applyServerChanges] IMPORTANT: Incoming attachments count: 0. If 0, this indicates desktop didn't export them.
[applyServerChanges] Local attachments BEFORE: total=106, synced=101, unsynced=5
  Local unsynced: id=att-789, type=LINK_ITEM, ...
[applyServerChanges] DEFECT #1 DETECTED: Desktop sent 0 attachments but Android has 5 unsynced
```

### 4. Коли маркуємо як synced:
```
[markSyncedNow] Marking 5 attachments synced
  [MARK-SYNCED] Attachment: id=att-789, type=LINK_ITEM, syncedAt=1735000000
```

---

## План дебагу

### Крок 1: Записати логи при створенні вкладення
```bash
adb logcat | grep "FWD_ATTACH"
```

Очікувані логи:
- 3 create логи (LinkItem, Attachment, CrossRef)

### Крок 2: Запустити sync (push на desktop)
```bash
adb logcat | grep "FWD_SYNC_TEST"
```

Очікувані логи:
- `[getChangesSince]` з `unsync'd=X` (має бути > 0)
- `[EXPORT-UNSYNC]` з деталями вкладення

**ПРОБЛЕМА:** Якщо `unsync'd=0`, вкладення не експортуватиметься.

### Крок 3: Перевірити на desktop
- Чи з'явилося вкладення на desktop?
- Якщо НЕ, проблема в дефекті #2 (desktop не импортує)

### Крок 4: Імпортувати з desktop (pull)
```bash
adb logcat | grep "FWD_SYNC_TEST"
```

Очікувані логи:
- `[applyServerChanges]` з `Total incoming: ...`
- Якщо `incoming=0`, то дефект #2 підтверджується

---

## Можливі причини кожного дефекту

### ДЕФЕКТ #1 Root Causes:
1. **Невіддалена очистка синхронізованих вкладень** - видаляння синхронізованих вкладень при замість soft delete
2. **Неправильна логіка merge** у `mergeAndMark()` для attachments
3. **Version conflict resolution** - старіша версія перевизначає нову

### ДЕФЕКТ #2 Root Causes:
1. **Desktop розробка** - вкладення взагалі не реалізовані на desktop
2. **Невключена функція експорту** в `/export` endpoint
3. **Немає логіки імпорту вкладень** на desktop після отримання

### ДЕФЕКТ #3 Root Causes:
1. **Невідправлення unsynced attachments** у `getChangesSince()`
2. **Фільтрація attachments за ownerProjectId** - сирітські вкладення відфільтровуються
3. **Таймінг проблема** - last sync time встановлено неправильно

---

## Контрольні точки синхронізації

Зберігай ці команди для швидкого дебагу:

```bash
# 1. Скинути синхрон (видалити synced timestamps)
adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
  "UPDATE attachment SET synced_at = NULL; UPDATE project_attachment_cross_ref SET synced_at = NULL;"

# 2. Перевірити кількість вкладень
adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
  "SELECT COUNT(*) FROM attachment; SELECT COUNT(*) FROM project_attachment_cross_ref;"

# 3. Переглянути unsynced attachments
adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
  "SELECT id, attachment_type, entity_id, owner_project_id, synced_at, version FROM attachment WHERE synced_at IS NULL LIMIT 10;"

# 4. Отримати логи
adb logcat -b all | grep "FWD_SYNC_TEST\|FWD_ATTACH" > /tmp/sync_debug.log
```

---

## Наступні кроки

1. **Запустити логування** на Android устрої
2. **Створити тестовий сценарій**:
   - Створити 3-5 вкладень на Android
   - Записати логи
   - Синхронізувати
   - Записати логи імпорту
   - Перевірити desktop
3. **Аналізувати логи** відповідно до цього гайду
4. **Виявити точний дефект** у одному з трьох місць
5. **Виправити** дефект з мінімальною зміною логіки
6. **Переконатися**, що синхронізація працює в обох напрямках

---

## Додаткові ресурси

- `SyncRepository.kt` - основна логіка синхронізації
- `AttachmentRepository.kt` - логіка створення вкладень
- `WifiSyncServer.kt` - endpoints для Wi-Fi синхронізації
- `SyncBump.kt` - helper функції для маркування синхронізації
