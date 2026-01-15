# Мережевий обмін файлами між десктопом і Android

## Бекенд (десктоп)
- HTTP API для файлів: `PUT/GET/DELETE /attachments/:id`
- Сховище файлів: `~/.forwardapp/attachments/remote` (можна змінити `FORWARDAPP_ATTACHMENT_DIR`)
- Запуск без UI: `make lan-server` або `npm run lan:server`
  - Порт: `FORWARDAPP_PORT` або `PORT` (дефолт 8080)
  - Токен: `FORWARDAPP_AUTH_TOKEN` або `X_AUTH_TOKEN` (опційно, заголовок `X-Auth-Token`)
- Wi‑Fi backup API: `GET /export`, `POST /import` (без змін)

## Клієнт (Android)
- Локальний каталог файлів: `filesDir/attachments/<id>`
  - Debug-шлях: `/data/data/com.romankozak.forwardappmobile.debug/files/attachments`
- Черга дій у SharedPreferences: `AttachmentSyncAction` (upload/download/delete-remote/delete-local)
  - Додається після Wi‑Fi імпорту (download для file-вкладень із бекапу)
  - Додається після Wi‑Fi експорту (upload для локальних file-вкладень)
- Виконання черги: HTTP до десктопу `/attachments/:id`
  - Базовий URL: адреса Wi‑Fi синку + порт `wifi_sync_port`
  - Токен: `wifi_auth_token` у налаштуваннях (якщо десктоп вимагає)
- Якщо `attachmentType` != "file" (нотатки, чеклісти тощо) — черга порожня, синк як раніше через JSON.

## Flow синку (файли)
1. Android `GET /export` → парсить бекап → додає `download` для file-вкладень → виконує чергу.
2. Android `POST /import` (unsynced) → додає `upload` для локальних file-вкладень → виконує чергу.
3. JSON-дані (нотатки/чеклісти/документи) йдуть у `/export`/`/import`, без `/attachments`.

## Налаштування користувача
- Запустіть десктопний сервер (доступний з телефону в одній Wi‑Fi мережі; порт відкритий).
- Якщо є токен — вкажіть на десктопі (`FORWARDAPP_AUTH_TOKEN`) і в Android (`wifi_auth_token`).
- Для бінарних файлів:
  - Android: покладіть файл у `filesDir/attachments/<id>` з тим самим `attachment.id`.
  - Десктоп (за потреби локального джерела): `~/.forwardapp/attachments/local/<id>`.

## Підсумок
- Нотатки/чеклісти/документи залишаються у JSON-бекапі.
- Бінарний контент (`attachmentType == "file"`) ходить окремо через `/attachments/:id` із чергою дій.
- Офлайн робота не ламається: локальні файли доступні, мережа потрібна лише для перенесення між пристроями.
