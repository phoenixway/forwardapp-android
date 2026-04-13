# ForwardApp Shared Architecture Blueprint

## Status

Орієнтир для desktop/shared migration станом на 2026-04-13.
Цей документ описує цільову архітектуру, а не поточний повністю завершений стан.

## Goal

Розвивати ForwardApp як:

- окремий `android-app` shell
- окремий `desktop-app` shell
- спільне архітектурне ядро з явними модульними межами

Desktop не є копією Android app.
Shared ядро не формується випадковими extraction-ами без цільового module ownership.

## Target Module Map

- `android-app`
  - Android UI shell
  - Android navigation
  - Android lifecycle / platform integrations
  - Android DI adapters
- `desktop-app`
  - desktop entrypoint
  - workbench/window shell
  - desktop navigation
  - desktop feature composition
  - desktop UX adapters
- `shared-contracts`
  - ids
  - enums
  - DTO
  - transport payloads
  - cross-platform import/export descriptors
- `shared-domain`
  - pure business rules
  - parsers
  - validation
  - normalization
  - merge/search/filter/sort rules
  - import/export normalization
- `shared-application`
  - feature orchestration
  - stores/reducers/state machines
  - repository interfaces
  - effect models
- `android-data`
  - Android persistence adapters
  - Room / ContentResolver glue
  - Android sync/storage implementations
- `desktop-data`
  - desktop file/json/sqlite adapters
  - desktop persistence glue
  - desktop sync/storage implementations

## Current Real State

- `shared-contracts`, `shared-domain`, `shared-application`, `desktop-data`, `desktop-app` already exist.
- `android-app` still contains a large amount of feature orchestration and state handling that should gradually move to shared layers.
- `sync` currently acts as an intermediate shared-ish bridge for import/export flows. It is not yet the final target ownership for all application orchestration.

## Dependency Rules

- Shared modules must not import `android.*`.
- Shared modules must not import `androidx.lifecycle.*`.
- Shared modules must not import `androidx.room.*`.
- `desktop-*` modules must not depend on Android modules.
- UI shells must not own pure business rules.
- Platform data modules must not own shared normalization/parsing rules if those rules are platform-agnostic.

## Migration Principles

1. Blueprint first, implementation second.
2. Vertical slice is allowed only when target ownership is already known.
3. Do not move Compose screens or platform glue into shared.
4. Prefer extracting pure logic, descriptors, state models, reducers and orchestration.
5. Keep Android and desktop independently buildable after each substantial step.

## Current Architectural Focus

Поточний головний фокус: snapshot/import/recovery path як перший спільний workflow між Android і Desktop.

Why this path first:

- already required by desktop recovery
- rich in pure normalization logic
- high duplication risk if left platform-local
- creates a foundation for desktop CLI/GUI recovery/import workflows

## Immediate Next Target

Після стабілізації snapshot/import flow наступним цільовим extraction є shared application-level store/use case для import preview and selection state, щоб Android ViewModel і desktop shell не дублювали orchestration. 
