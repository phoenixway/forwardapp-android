Рефакторинг custom lists → NoteDocument просувається. UI та навігацію вже
переніс, fallback-маршрути прибрав. Backup та SyncRepository оновлено
під `legacyNotes`/`noteDocuments`. Далі треба:
1. Підготувати юніт/інтеграційні тести для NoteDocument і міграції 60→61.
2. Перевірити десктопний WiFi-синк на предмет підтримки note documents та
   за потреби оновити createSyncReport/applyChanges.
