# Посібник з ручного тестування синхронізації (Android ⇄ Desktop)

## 1. Дампи синхронізації

- **Android → /tmp**  
  - `make get-android-dumps`  
    - Тягне `/data/user/0/com.romankozak.forwardappmobile.debug/files/sync-dumps` у `/tmp/android-sync-dumps` (і сирий tar у `/tmp/android-sync-dumps.tar`).
  - Очистити дампи на пристрої:  
    `adb exec-out run-as com.romankozak.forwardappmobile.debug sh -c 'rm -f /data/user/0/com.romankozak.forwardappmobile.debug/files/sync-dumps/*'`

- **Desktop → /tmp**  
  - LAN авто-дамп лежить у `/tmp/forwardapp-backup-dumps/wifi-import---auto.json`.
  - Ручні експорт/імпорт Web/LAN: файли також у `/tmp/forwardapp-backup-dumps/` (prefixed `export---…`, `import---…`).

- **Швидка перевірка дампів**  
  - Розмір списків: `jq '.database.listItems | length' wifi-import---auto.json`  
  - Дублі по (projectId, entityId, itemType):  
    ```bash
    python - <<'PY'
    import json, collections
    data=json.load(open("wifi-import---auto.json"))
    li=data["database"]["listItems"]
    c=collections.Counter((x["projectId"],x["entityId"],x["itemType"]) for x in li)
    print("dups", sum(1 for v in c.values() if v>1))
    PY
    ```

## 2. Примірний чекліст ручного тесту
1) **Початковий стан**  
   - Очистити дампи на пристрої, переконатися, що синк вимкнено або вказано валідний IP.  
   - На десктопі переконатися, що локальна база не дублює беклог (якщо треба — імпорт чистих дампів).
2) **Сортування на Android**  
   - Відкрити беклог, перетягнути кілька goals/підпроєктів.  
   - Переконатися, що порядок лишився після перезавантаження екрана/додатка.  
3) **Сортування на Desktop**  
   - Перетягнути ті самі елементи. Перевірити, що порядок зберігся після перезавантаження десктопу.  
4) **Синхронізація**  
   - Запустити Wi‑Fi sync у будь-який бік.  
   - Переконатися, що порядок збігається на обох платформах і не з’явились дублікати.  
   - Перевірити дамп `wifi-import---auto.json` на дублікати як вище.
5) **Видалення/підпроєкти**  
   - Пересунути підпроєкт (SUBLIST) і goal. Підтвердити, що синк не відкотить порядок.

## 3. Автотести
- Контрактні тести синку: `./gradlew :app:syncContractTest` (alias `testProdDebugUnitTest` з включеними sync-тестами).
- Швидка збірка: `./gradlew :app:assembleDebug` або `make debug` / `make debug-cycle`.

## 4. Нотатки по порядку беклогу
- Порядок зберігається в окремій таблиці `backlog_orders` (див. `docs/DB_MIGRATION_HISTORY.md`), щоб уникати конфліктів і дублювань.  
- Під час синку вхідні `listItems` коригуються порядком із `backlog_orders` (LWW за `orderVersion/updatedAt`), після чого виконується дедуплікація.

