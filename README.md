# ForwardAppMobile

ForwardAppMobile — Android-додаток для управління проєктами, цілями та особистою ефективністю: беклог, щоденний план, нагадування, трекер активностей, стратегічний шар і експериментальні можливості (AI/файли/Wi‑Fi синк).

## Основні фічі
- Беклог проєкту з свайпами, вибором, переміщенням і швидкими діями (додавання до дня, трекінг, нагадування).
- Розширена навігація: головний екран з планувальними режимами, історією, пошуком, контекстами; окремі екрани дня, трекера, стратегічного менеджменту.
- Нагадування та трекінг активностей: ReminderPropertiesDialog, ActivityTracker, інтеграція з записами проєктів/цілей.
- Вкладення й документи: бібліотека вкладень (експериментальна), редактор документів/чеклістів, посилання на зовнішні ресурси.
- Wi‑Fi синк/імпорт (експериментально), експериментальні фічі з керуванням через налаштування.
- Тоглери фіч: `FeatureFlag`/`FeatureToggles` збережені у DataStore та керовані в налаштуваннях.

Детальний огляд фіч і ключових файлів: `docs/FEATURES2.md`.

## Архітектура (скорочено)
- **UI/Compose**: `ui/screens/*`, `ui/features/*`, спільні компоненти в `features/common`.
- **Домени/репозиторії**: `data/repository/*`, моделі/DAO в `data/database/*`.
- **Навігація**: графи у `routes/*`.
- **Фічетогли**: `config/FeatureFlag.kt`, `config/FeatureToggles.kt`, налаштування `ui/screens/settings/*`.
- **Інтеграції**: Wi‑Fi синк (`ui/screens/mainscreen/usecases/SyncUseCase.kt`, `WifiSyncServer.kt`), AI/чат маршрути (`routes/ChatRoute.kt`).

## Швидкий старт (локальна збірка)
```bash
# Встановлення залежностей
./gradlew tasks   # перевірити доступність Gradle wrapper

# Збірка
./gradlew :app:assembleDebug

# Тести (якщо потрібні)
./gradlew :app:testDebugUnitTest
```

## Керування фічами (feature toggles)
- Тоглери зберігаються у DataStore (`SettingsRepository.featureTogglesFlow`).
- Керування у UI: Settings → Experimental Features (Attachments library, Planning modes, Wi‑Fi sync, Strategic management, System project moves тощо).
- Програмно: `FeatureToggles.isEnabled(FeatureFlag.X)`; оновлення через `SettingsViewModel.updateFeatureToggle`.

## Корисні шляхи
- Головний екран і навігація: `ui/screens/mainscreen/*`, `routes/AppNavigation.kt`.
- Беклог/проєкт: `ui/screens/projectscreen/*`, `ui/features/backlog/*`.
- Нагадування: `ui/reminders/dialogs/ReminderPropertiesDialog.kt`, VM-хендлери в `ProjectScreenViewModel.kt`.
- Вкладення: `features/attachments/ui/library/*`.
- Синк: `ui/screens/mainscreen/usecases/SyncUseCase.kt`, `WifiSyncServer.kt`.

## Документація
- Архітектурні нотатки: `docs/ARCHITECTURE_NOTES.md`.
- Огляд фіч і ключових файлів: `docs/FEATURES2.md`.
- Компонент HoldMenu2: `docs/HoldMenu2-manual.md`.

