# Історія змін схеми БД (Android)

## Версія 77 (MIGRATION_76_77)
- **Нова таблиця `backlog_orders`**  
  - Поля: `id` (PK, дорівнює id listItem), `list_id`, `item_id`, `item_order`, `order_version`, `updatedAt`, `syncedAt`, `isDeleted`.  
  - Індекси: `list_id` і унікальний `list_id + item_id`.  
  - FK: `list_id` → `projects.id` (CASCADE).
- **Призначення**: зробити порядок беклогу канонічним джерелом, відокремленим від `list_items`. Це знижує ризик дублювання, дозволяє LWW для порядку (`orderVersion/updatedAt`) і спрощує синк.
- **Seed у міграції**: значення переноситься з існуючих `list_items` (`order_version = COALESCE(version, updatedAt, 0)`).
- **Логіка синку**:
  - Експорт/дельта включає `backlogOrders`.
  - Імпорт: перед мерджем `listItems` застосовується порядок із `backlog_orders` (LWW), після чого виконуються дедупи `listItems` і `backlog_orders`.
- **Репозиторії**:
  - `BacklogOrderDao/Repository` для читання/спостереження та upsert порядків.
  - `ListItemRepository.updateListItemsOrder` пише і в `list_items`, і в `backlog_orders`.
  - `ProjectRepository` комбінує `listItems` з `backlogOrders` для відображення порядку у беклозі.

## Попередні зміни
- Докладні специфікації синку/бекупу — див. `docs/backup_schema_spec.md`, `docs/SYNC_FEATURE_SPEC.md`.
