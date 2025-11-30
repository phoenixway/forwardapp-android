  # Masterplan — Synapse Zero-Friction Sync (Android)

  1) Версіювання даних
  - Додати/узгодити поля updated_at, version, is_deleted, synced_at у всіх сутностях.
  - Тригери/репозиторії на будь-яку зміну: оновлюють updated_at та інкрементують version; видалення = is_deleted=1.

  2) Sync engine (Android)
  - SyncRepository: збір unsynced (updated_at > synced_at або synced_at=null), застосування server changes.
  - LWW: порівнюємо version, за рівності – updated_at; systemKey сутності зливаємо за ключем.
  - Soft-delete підтримка, synced_at оновлюється після успішного застосування.

  3) Canonical backup baseline
  - FullAppBackup v1 (існує) + чіткі статуси/помилки імпорту/експорту.
  - Selective import з повною підтримкою сутностей (включно scripts/attachments/recent entries).

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
