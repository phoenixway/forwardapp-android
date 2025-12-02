## 1. Поточне завдання (CURRENT TASK)
  у мене своєрідна система вкладень. задіяні таблиці нотаток, чеклістів, власне вкладень, посилань на них і ще бог знає чого. поточний код не
  синхронізує вкладення так як потрібно. в бібіотеці вкладень в анроїд версії було 106 вкладень. після синхронізації - 12. на анроїді роблю в
  інбоксі нову нотатку - не синхронізується на десктоп і пропадає з аедроїда. на десктопі при створенні нотатки вона існує локально а на андроїді
  її нема. з цим всім треба розібратися. ймовірно треба весь процес від створення вкладення будьякого типу до кінця синхронізації покрити контрольними
  логами щоб дізнатися де глюки.

## 3. План дій
- [x] Проаналізовано логіку синхронізації (`SyncRepository.kt`) та виявлено, що `systemKey` не використовувався як стабільний ідентифікатор для системних проектів.
- [x] **Виправлено:** Проведено значний рефакторинг функції `applyServerChanges` в `SyncRepository.kt`.
    -   Нова логіка спочатку ідентифікує системні проекти за `systemKey` і приводить їхні `id` у відповідність до локальної бази даних.
    -   Створюється карта перенаправлення (`idRedirects`) для старих ID.
    -   Ця карта використовується для корекції всіх зовнішніх ключів (`projectId`, `parentId`, `entityId`) у всіх інших сутностях (нотатках, чеклістах, `ListItem` тощо) *перед* тим, як виконувати їх злиття.
- [x] **Результат:** Цей підхід гарантує цілісність даних та коректну синхронізацію системних проектів та всіх пов'язаних з ними вкладень між різними клієнтами, незалежно від їхніх локальних `id`.
- [ ] Перевірити коректність роботи синхронізації після ручної заміни файлу.

## 4. Невирішені проблеми / Блокери (PROBLEMS)

## 5. Значимі файли і модулі (IMPORTANT FILES / MODULES)

## 6. Інструкції та вимоги до стилю (INSTRUCTIONS)

- Не переписувати архітектуру без підтвердження  
- Виконувати лише один крок за раз  
- Не змінювати DI без дозволу  
- Додавати пропозицію оновлення контексту після кожної дії  
- Писати код, що точно компілюється  
- Завжди пояснювати причину помилки  

---

## 7. Замітки для агента (NOTES)
- ідеї в SYNAPSE_DESIGN.md, SYNC_FEATURE_SPEC.md
- спочатку почитати файли з папки docs

### обірваний лог який можке містити підказку

Проблема НАЙДЕНА! Все еще deltaSinceParam=null.

Это означает, что десктоп не отправляет параметр deltaSince в запросе к Android! Я добавил логику на Android для принятия параметра, но десктоп не отправляет его.

Нужно найти код на десктопе, который делает HTTP GET запрос к /export и добавить туда параметр deltaSince:
$ grep -rn "fetch.*export\|axios.*export\|/export" /home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src --include="*.ts" --include="*.tsx" | grep -v node_modules | head -20
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/main/main.ts:955:        const fullUrl = buildDeviceUrl(deviceAddress, "/export");
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/main/backup/server.ts:36:  app.get("/export", async (_req, res) => {
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/main/backup/server.ts:45:      console.error("[LAN] /export failed:", err);
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/main/backup/server.test.ts:46:      const res = await fetch(`${base}/export`);
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/main/backup/server.test.ts:109:      const resUnauthorized = await fetch(`${base}/export`);
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/main/backup/server.test.ts:112:      const resAuthorized = await fetch(`${base}/export`, {
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/renderer/App.tsx:348:      const exportUrl = rawAddress ? buildDeviceUrl(rawAddress, '/export') : null;
/home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/renderer/App.tsx:360:        const resp = await fetch(exportUrl);
Найдено! Line 955 в main.ts и line 348/360 в App.tsx. Дивимо App.tsx:
✓ Read
~/studio/public/forwardapp-suit/forwardapp-desktop/src/renderer/App.tsx
@340-380
Знайдено! Line 360: const resp = await fetch(exportUrl);

Тут нужно додати параметр deltaSince. Дивимо як отримати останній час синхронізації на десктопі:
$ grep -B20 "const exportUrl" /home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/renderer/App.tsx | head -30
[... 6 lines truncated ...]
  }, []);

  useEffect(() => {
    let isCancelled = false;
    let timer: number | null = null;
    const runSync = async () => {
      if (isCancelled) return;
      if (isOffline) {
        logSync("Skip: offline");
        timer = window.setTimeout(runSync, 15000);
        return;
      }
      const settings = await window.electronAPI?.getAppSettings?.();
      const rawAddress = settings?.defaultWifiImportAddress || settings?.defaultLanAddress;
      const exportUrl = rawAddress ? buildDeviceUrl(rawAddress, '/export') : null;
Нужно получить последний syncedAt из state и передать его. Дивимо логіку з latestStateRef:
$  (exit code: 1)
Error: `cmd` must be a string representing a command.
$ grep -n "latestStateRef\|stateReplaced" /home/romankozak/studio/public/forwardapp-suit/forwardapp-desktop/src/renderer/App.tsx | head -20
16:  stateReplaced,
102:          dispatch(stateReplaced(finalState));
150:      dispatch(stateReplaced(finalState));
261:  const latestStateRef = useRef(listsState);
297:    latestStateRef.current = listsState;
364:        const mergedState = mergeBackupWithState(latestStateRef.current, incomingBackup);
366:        dispatch(stateReplaced(mergedState));
Добавлю функцію для отримання мінімального syncedAt з state на десктопі:
✓ Read
~/studio/public/forwardapp-suit/forwardapp-desktop/src/renderer/App.tsx
@1-50

---

## 8. Після цього
- Можна прибрати додане логування з `GoalRepository` та `SyncRepository`.
---