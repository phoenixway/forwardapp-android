# Механізм вмикання/вимикання синхронізації

Status: CURRENT

Current Android synchronization availability is controlled by two independent
layers:

1. a compile-time capability gate in the `sync` module;
2. a persisted runtime feature toggle for Wi-Fi sync behavior and UI actions.

The two layers solve different problems and must not be conflated.

## 1. Compile-time sync capability

The `sync` module selects either the real implementation or no-op
implementations at build time.

Current ownership is in:

`sync/build.gradle.kts`

The build script reads the Gradle project property:

`SYNC_ENABLED`

Behavior:

- absent or `true` -> real sync implementation;
- `false` -> no-op sync implementation.

The default is enabled.

The real source set combines:

- `src/main/java`
- `src/syncOn/java`

The disabled source set combines:

- `src/main/java`
- `src/syncOff/java`

Current `syncOff` contains no-op/disabled implementations including
`NoOpImplementations.kt`, an alternate `AttachmentsRepository`, and
`SyncLocalService`.

This layer determines whether real synchronization capability is compiled into
the application. A runtime feature toggle cannot restore functionality that was
excluded here.

A Gradle properties entry, when used, must therefore be named:

`SYNC_ENABLED=false`

The older lowercase `syncEnabled` spelling is not the property read by the
current build script.

## 2. Runtime Wi-Fi sync feature toggle

Wi-Fi sync also has a runtime feature flag:

`FeatureFlag.WifiSync`

Its default value is derived from:

`BuildConfig.IS_EXPERIMENTAL_BUILD`

Therefore the `exp` / `prod` flavor still influences the initial/default state,
but it is not the complete runtime toggle mechanism.

Current runtime ownership includes:

- `FeatureToggles`;
- `SettingsRepository`;
- `SettingsViewModel`;
- Wi-Fi sync settings and workflow consumers.

`SettingsRepository` persists feature-toggle overrides in DataStore. A stored
override can therefore differ from the flavor-derived default.

`SettingsViewModel` reads the persisted feature-toggle map and exposes
`wifiSyncEnabled` to the settings UI.

The Settings UI allows the Wi-Fi sync feature to be enabled or disabled. When
disabled, dependent server controls are disabled and active Wi-Fi sync
workflows are gated.

Runtime consumers also check `FeatureFlag.WifiSync` before opening Wi-Fi sync
dialogs or performing Wi-Fi import/push actions.

## 3. Relationship between the layers

The effective model is:

    build capability
        SYNC_ENABLED
            |
            v
    syncOn or syncOff implementation
            |
            v
    runtime feature state
        FeatureFlag.WifiSync
            |
            v
    Settings / dialogs / import / push actions

`SYNC_ENABLED=false` removes the real implementation at build time.

When the real implementation is present, `FeatureFlag.WifiSync` controls
whether the user-facing/runtime Wi-Fi sync workflows are enabled.

`BuildConfig.IS_EXPERIMENTAL_BUILD` supplies the default runtime value, not an
absolute visibility or capability rule.

## 4. Maintenance rule

When changing synchronization availability, preserve the distinction between:

- build-time implementation selection;
- flavor-derived defaults;
- persisted runtime overrides;
- feature-gated UI/workflow actions.

Do not document `prod` as permanently disabling Wi-Fi sync unless production
code again enforces that as a hard rule.

Do not rename or replace `SYNC_ENABLED` in documentation without changing the
actual Gradle property consumed by `sync/build.gradle.kts`.
