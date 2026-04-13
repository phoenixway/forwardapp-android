# ForwardApp Module Rules

## `shared-contracts`

May contain:

- ids
- enums
- DTO
- transport payloads
- cross-platform primitive result/error shapes
- import/export descriptors

Must not contain:

- business logic
- reducers
- use cases
- platform APIs
- Compose UI

## `shared-domain`

May contain:

- pure business rules
- parsers
- normalization
- validation
- mappers
- merge/search/filter/sort rules

Must not contain:

- Android APIs
- desktop filesystem/process glue
- Room
- ViewModel
- Compose UI

## `shared-application`

May contain:

- intents/actions/events
- state machines
- reducers/stores
- repository interfaces
- feature orchestration
- effect models

Must not contain:

- Android ViewModel
- Room/DAO implementations
- desktop shell code
- file adapters
- Compose screen rendering

## `android-app`

May contain:

- Android UI shell
- Android navigation
- lifecycle/platform UX wiring
- platform effect adapters

Must not contain:

- duplicated domain rules
- duplicated feature state machines that should be shared
- direct persistence logic inside screens

## `desktop-app`

May contain:

- desktop shell
- workbench/window composition
- desktop navigation
- keyboard/command UX
- platform effect adapters

Must not contain:

- Android dependencies
- duplicated domain rules
- duplicated shared stores/reducers
- direct storage logic in UI

## `android-data` and `desktop-data`

May contain:

- platform persistence adapters
- storage wiring
- sync/file/db glue

Must not contain:

- screen-specific UI logic
- shared normalization/parsing rules
- duplicated business rules
