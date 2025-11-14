# ForwardApp Desktop (Electron + React)

## Підготовка

1. Згенеруй npm-пакет зі спільним KMP-шаром: `make shared-npm`.
2. Увімкни pnpm (разово): `corepack enable`.
3. Перейди в `apps/desktop` і виконай `pnpm install`.

## Режим розробки

```bash
pnpm dev
```

- `vite` стартує React/TypeScript renderer.
- Для Electron запускається `tsc --watch`, який компілює `electron/*.ts` у `dist-electron/electron`, та `nodemon`, що перезапускає `electron dist-electron/electron/main.js` при зміні коду. Скрипт `wait-on` гарантує, що перша збірка завершилася перед стартом Electron.

## Білд

```bash
pnpm build
```

Команда послідовно виконує `vite build` (renderer) і `tsc -p tsconfig.electron.json` (Electron, вихід у `dist-electron/electron/`). Потім можна запустити `electron dist-electron/electron/main.js` або інтегруватися з інструментом пакування на кшталт `electron-builder`.

> ℹ️ Проєкти зберігаються лише в пам’яті під час цієї фази. Після перезапуску застосунку вони будуть втрачені. Персистентність з SQLDelight/sql.js можна додати як окрему задачу.
