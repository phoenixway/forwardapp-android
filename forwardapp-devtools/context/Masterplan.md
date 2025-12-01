  # Masterplan — Synapse Zero-Friction Sync (Android)

   - LWW ядро:
      - getUnsyncedChanges: збирає записи з syncedAt=null|старше або isDeleted=true.
      - applyServerChanges: merge за version → updatedAt (де є), виставляє syncedAt.
      - Soft delete: isDeleted=true, не видаляємо фізично.
  - Wi‑Fi push/pull:
      - Push: pushUnsyncedToWifi (є) викликається з UI, адресу з settings/вводу; після успіху ставимо syncedAt.
      - Pull: fetchBackupFromWifi → applyServerChanges (LWW) замість навігації на sync screen, опційно лог/статус для
        користувача.
  - UI Synapse:
      - Статус/force sync (settings + індикатор), лог останньої сесії; кнопка Wi‑Fi push/pull.
  - Background sync: WorkManager із Wi‑Fi/charging constraints, debounce, retries.
  - Тести:
      - Unit: getUnsyncedChanges/applyServerChanges (конфлікти version/updatedAt, isDeleted, syncedAt).
      - Integration: export/import round-trip v2, selective import з sync metadata.
      - Smoke Wi‑Fi push/pull (можливо інструментальні або локальні fake server).
  - Документи: оновити README/backup_schema (вже v2), додати опис LWW/Sync flow.
  
  4) Backend stub (Ktor або Wi‑Fi)
  - REST pull/push diff, auth токен, збереження lastSyncTimestamp на клієнті.
  - Мінімальна серверна логіка LWW для дзеркала даних.

  5) UI Synapse
  - Екран налаштувань: статус (Synced/Syncing/Error/Offline), остання синхра, кількість pending, кнопка Force Sync, лог
  останньої сесії.
  - Індикатор на головному екрані (статус/анімація під час синхри).

  6) Background sync
  - WorkManager з wifi/charging constraints, debounce, retries, нотифікація про фейл.

  7) Тести/матриця
  - Автотести LWW (конфлікти, soft delete), інтеграційні sync round-trip (pull/push).
  - Тести selective import + attachments.
  - Ручний чекліст: systemKey сутності, attachments/scripts/recent entries, full/partial імпорт.
