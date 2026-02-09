# Зарезервовані ролі контекстів

## Що це
Зарезервовані ролі контекстів — це базові ролі (`basePresetCode`), які:
- мають фіксований набір capability;
- автоматично синхронізуються в `structure_presets`;
- не повинні видалятися користувачем.

## Поточний список зарезервованих ролей
Визначено в `app/src/main/java/com/romankozak/forwardappmobile/core/gate/ContextRoleRegistry.kt`.

- `project` -> `backlog`, `artifact`
- `direction` -> `direction`
- `aspect` -> `dashboard`
- `main-beacon` -> `direction`, `artifact`
- `management` -> `backlog`, `inbox`

## Де і як це працює
1. Джерело правди ролей і capability:
- `ContextRoleRegistry` (`reservedRoleDefinitions` + `isReservedBaseRole`)

2. Автоматичне створення/оновлення в БД:
- `ContextStructureRepository.ensureReservedBaseRolePresets()`
- Upsert відбувається перед читанням/застосуванням пресетів.

3. Заборона видалення в UI:
- `StructurePresetsScreen` вимикає кнопку delete для reserved ролей.
- `StructurePresetsViewModel.removePreset(...)` має додаткову перевірку і не видаляє reserved ролі.

## Як додати нову зарезервовану роль
1. Відкрити `ContextRoleRegistry.kt`.
2. Додати константу `ROLE_...`.
3. Додати елемент у `reservedRoleDefinitions`:
- `code`
- `label`
- `description`
- `capabilities` (через `CapabilityId("...")`)
4. Переконатися, що capability існують у системі.
5. Після запуску екранів пресетів/налаштувань або застосування пресету роль буде автоматично upsert у `structure_presets`.

## Як редагувати існуючу зарезервовану роль
1. Внести зміни в `reservedRoleDefinitions` (label/description/capabilities).
2. Не змінювати `code`, якщо потрібна зворотна сумісність.
3. При наступному `ensureReservedBaseRolePresets()` пресет оновиться в БД.

## Як заборонити/дозволити видалення
Зараз заборона жорстка для всіх кодів, які повертає `ContextRoleRegistry.isReservedBaseRole(...)`.

Щоб змінити поведінку:
- оновити умову в `isReservedBaseRole(...)`;
- синхронно оновити UI-логіку в `StructurePresetsScreen` і ViewModel-захист у `StructurePresetsViewModel.removePreset(...)`.

## Важливі примітки
- `basePresetCode` у `ContextConfiguration` використовує саме код ролі (наприклад, `project`).
- Якщо роль додана як reserved, користувацьке видалення блокується.
- Capability з reserved ролей впливають на відображення вкладок/можливостей контексту через `ContextRoleRegistry.getCapabilitiesForRole(...)`.
