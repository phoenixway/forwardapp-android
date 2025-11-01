рефакторинг кастомних списків і нотатків. каст. списки - по факту і є. я
намагався всюди де можна їх так перейменувати і зробити інший подібний
рефакторининг. а старі нотатки - дуже розвинуті. мали бути перейменовані
щось в стилі "legacynotes". я вже дійшов до рефакторингу сутностей коду.
більшість ui роботи вже має бути зроблена.
2024-11-28: прибрав застарілі NoteDao/NoteFts, перейменував залежності на LegacyNoteDao в SyncRepository та оновив WARP.md. `make check-compile` не запустився через обмежений мережевий доступ.
2024-11-28: перейменував екосистему custom list → NoteDocument (таблиці, маршрути, екрани), додав міграцію 60→61, створив адаптер legacy нотаток і оновив UI на нову модель.
2024-11-28: прибрав fallback-маршрути `custom_list_*` із `AppNavigation` та видалив застарілі `.old` екрани, аби позбутись дубльованих шляхів після переходу на NoteDocument.
2024-11-28: перейменував backup-моделі на `legacyNotes`, синхронізував SyncRepository/FullAppBackup з note documents і перевірив компіляцію (`./gradlew :app:compileDebugKotlin`).
2024-11-29: додав нову сутність Checklist (таблиці, DAO, Repository, міграцію 61→62), інтегрував у RecentItems/Sync/Backup та підключив у ProjectRepository. Реалізував Compose-екран `ChecklistScreen` з drag&drop, швидким додаванням рядків та очищенням виконаних пунктів, оновив навігацію, Attachments UI й Recent Items. `./gradlew :app:compileDebugKotlin` не вдалося запустити через обмеження доступу до Gradle lock-файлу в sandbox.
