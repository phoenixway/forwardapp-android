# Прогрес роботи над синхронізацією вкладень

## 02.12.2024

### Діагностика проблеми
- **Знайдено**: при синхронізації з сервера вкладення = 0, але локально вкладення створюються з `syncedAt=null` ✅
- **Дивлюся на експорт**: при `getChangesSince(deltaSince)` вкладення не експортуються вообще

### Виявлені 3 критичні баги:

#### 1. **Неправильна фільтрація при експорті (getChangesSince)**
**Проблема**: Фільтр `(updatedAt > since)` не враховує несинхронізовані вкладення
- Коли вкладення створюється локально з `syncedAt=null`, воно має `updatedAt = createdAt`
- При наступній синхронізації (deltaSince > createdAt), це вкладення не експортується!
**Виправлено**: Змінена логіка на `syncedAt == null OR (updatedAt > since)`

#### 2. **Неоновлення updatedAt при імпорті (applyServerChanges)**
**Проблема**: При імпорті вкладень встановлюється тільки `syncedAt`, але не `updatedAt`
- Тому при наступному експорті фільтр не бачить це як оновлене
**Виправлено**: Додано `updatedAt = maxOf(at.updatedAt, synced)` для AttachmentEntity та ProjectAttachmentCrossRef

#### 3. **Перенаправлення ID для системних проектів**
**Вже виправлено раніше**: ID системних проектів приводяться у відповідність перед синхронізацією

### Додане логування
1. **SyncRepository.kt**:
   - `getChangesSince()`: показує кількість unsync'd, updated, та final result для attachments/crossRefs
   - `createDeltaBackupJsonString()`: логує скільки attachment/crossRef вироблювалось до експорту
   - `applyServerChanges()`: логує перенаправлення ownerProjectId для вкладень

2. **AttachmentRepository.kt**:
   - `ensureAttachmentForEntity()`: логує створення та знаходження
   - `createLinkAttachment()`: логує всі операції

### Статус
✅ Код скомпільований успішно
✅ Усі 3 баги виправлені логічно
⏳ Очікування тестування на пристрої

### Зміни у коді
- `applyServerChanges()` line 1484: додано `updatedAt` при merge для attachments
- `applyServerChanges()` line 1500: додано `updatedAt` при merge для crossRefs  
- `getChangesSince()` line 970-980: змінена логіка фільтрації на `syncedAt == null OR (updatedAt > since)`
