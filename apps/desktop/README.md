# ForwardApp Desktop (Electron + React)

## Підготовка

1. Згенеруй npm-пакет зі спільним KMP-шаром: `make shared-npm`.
2. Перейди в `apps/desktop` і виконай `npm install`.

## Режим розробки

```bash
npm run dev
```

- `vite` стартує React/TypeScript renderer.
- `tsx` виконує Electron (TS) entrypoints (`electron/main.ts`, `electron/preload.ts`) без окремого кроку білду.

## Білд

```bash
npm run build
```

Команда послідовно виконує:

1. `vite build` → збирає renderer у `apps/desktop/dist`.
2. `tsc -p tsconfig.electron.json` → компілює `electron/*.ts` у `apps/desktop/dist-electron`.

Для подальшого пакування (app installer) усе ще потрібен інструмент на кшталт `electron-builder`, але на цьому етапі ми фокусуємося на UI та інтеграції з KMP data-layer.

> ℹ️ Проєкти зберігаються лише в пам’яті під час цієї фази. Після перезапуску застосунку вони будуть втрачені. Персистентність з SQLDelight/sql.js можна додати як окрему задачу.
