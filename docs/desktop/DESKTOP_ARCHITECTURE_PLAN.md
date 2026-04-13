# ForwardApp Desktop Architecture Plan

## Goal

Побудувати desktop-версію без копіювання Android `app` у новий модуль.
Desktop має розвиватися як окремий продукт із shared domain/core і platform adapters.

Detailed governing documents for this plan now live in:

- `docs/desktop/00-shared-architecture-blueprint.md`
- `docs/desktop/01-campaign-board.md`
- `docs/desktop/02-decision-log.md`
- `docs/desktop/03-session-ledger.md`
- `docs/desktop/04-module-rules.md`

## Principles

1. `desktop-app` не залежить від Android API.
2. UI не читає напряму repositories чи storage.
3. Shared logic переноситься в окремі KMP/shared модулі тільки після очищення від Android/Room.
4. Кожен desktop feature має свій state holder, use case boundary і adapter boundary.
5. Жодних god-objects у desktop shell.
6. Shared extraction is campaign-driven, not opportunistic.

## Target Architecture

### Layers

- `desktop-app`
  - desktop entrypoint
  - window shell
  - desktop navigation
  - desktop feature composition
- `shared-contracts` (next)
  - ids
  - DTO
  - enums
  - transport payloads
- `shared-domain`
  - use cases
  - parsers
  - mappers
  - text / markdown / search / clipboard rules
- `shared-application`
  - feature orchestration
  - stores/reducers/state machines
- `android-app`
  - Android UI
  - Android Room/Hilt/Services
  - Android platform adapters
- `desktop-data`
  - file/db adapters
  - desktop sync adapters
  - persistence wiring

## Migration Order

1. Desktop shell and navigation.
2. Desktop design system and layout primitives.
3. Shared contracts module.
4. Shared domain module.
5. Read-only desktop context explorer.
6. Desktop command palette.
7. Editable backlog/inbox flows.
8. Desktop persistence and sync.
9. Shared application stores for cross-platform features.

## First Vertical Slice

### Scope

- open application window
- desktop navigation
- workbench area
- context explorer placeholder
- dashboard placeholder
- settings placeholder

### Non-goals

- Room reuse
- Android ViewModels reuse
- Hilt reuse
- Android services / notifications / reminders

## Anti-Spaghetti Rules

1. No feature may directly depend on another feature's internal state class.
2. No UI file may exceed one responsibility boundary.
3. Shared modules may not import `android.*` or `androidx.room.*`.
4. Desktop adapters must be behind interfaces.
5. New desktop functionality ships only as vertical slices.
6. Vertical slices are valid only when target module ownership is explicit.
