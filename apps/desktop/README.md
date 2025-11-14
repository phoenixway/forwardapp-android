# ForwardApp Desktop (Electron + React)

## Підготовка

1. Згенеруй npm-пакет зі спільним KMP-шаром:  
   `make shared-npm`
2. Перейди в цю директорію і встанови залежності:  
   `cd apps/desktop && npm install`

## Режим розробки

```bash
npm run dev
```

- `vite` стартує renderer (React).
- Electron автоматично відкриє вікно та під’єднається до dev-сервера.

## Білд

```bash
npm run build
```

Клієнтський бандл з’явиться у `apps/desktop/dist`. Для продакшену потрібно буде прописати власний процес пакування Electron (наприклад, через electron-builder) — на цій віхі фокус був на UI та інтеграції зі спільним KMP data-layer.
