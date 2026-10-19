# Quiet Notifications v1 (native Ambxst mod)

**Delivery:** installable mod package, offline preflight verified. **Not deployed** to an actual Hyprland session. Requires local ref validation and an isolated runtime check before enabling in production.

## User contract

* An unobtrusive 17px notification bell appears **immediately left of the right group** in the horizontal topbar.
* Zero unread notifications → bell entirely absent and no width reserved.
* Ordinary unread → simple `Icons.bell`, styled with the active theme (`Colors.outline`).
* Any unread `urgency=critical` → `Icons.bellRinging`, theme's `Colors.errorContainer` background and `Colors.overErrorContainer` foreground, with **no pulsing/blinking**.
* Click → an existing Ambxst `BarPopup` containing existing `NotificationDelegate` card components; notifications and their native actions remain intact.
* On closing the popup, **only the unread IDs captured on opening** are marked read. A message arriving while the popup is open remains unread, and can retain the bell after closing.
* Marking as read does not dismiss, delete, or purge a notification. The unread marker is written into Ambxst's existing notification-history JSON.
* Preexisting cached notifications, which have no `quietUnread` field, load as read (no flood of legacy messages on first boot).
* New notifications remain stored, but transient popups are disabled by default (`Notifications.silent: true`). The existing notch's silent/DND control can change this at runtime.

## Architecture

`ambxst.mod.json` declares:

1. An overlay adding `modules/bar/QuietNotificationButton.qml`.
2. A focused patch to `modules/bar/BarContent.qml` (widget placement) and `modules/services/Notifications.qml` (per-message read marker, priority, serialization, DND default).

No separate notification daemon, background polling, `hyprctl`, additional service process, or new DBus server.

The component is inserted only in the **horizontal** bar. Vertical bars are unchanged. The existing notch bell is not removed to preserve upstream behavior.

## Verification

Unzip the `.zip` so that `ambxst.mod.json` is directly inside the extracted folder, then:

```bash
cd ~/.local/src/ambxst
python3 ~/Downloads/amx-quiet-notifications-v1/tests/check.py \
  --repo "$PWD" \
  --ref 1e1a56493582f34e5e5593bc9d3c2f87246266fb
```

The checker performs:

- manifest and target checks;
- static behavior checks;
- **synthetic** patch apply using `git apply --check --whitespace=error-all`;
- read-only `git show` of files from the specified local ref and patch check against those exact local bytes.

A passing `HOST_BASE_PATCH` checks the selected Git ref, **not the final composition with every installed mod**. Only the manager's own compatibility composition can check interactions with other bar mods.

If host preflight passes, install **without enabling**:

```bash
ambxst mods install ~/Downloads/amx-quiet-notifications-v1
ambxst mods list
```

When the restore-safe isolated return path has been verified and the manager compatibility review has passed, enable the package and test in the isolated candidate shell, not the active session:

```bash
ambxst mods enable dev.hyperland.quiet-notifications
```

Enabling composes a generation, which is picked up at the **next** Ambxst start. **Do not restart** the production `ambxst.service` as part of this package's installation. The project already identified restart/session-restore risks.

Sample runtime acceptance notifications (run in the isolated candidate):

```bash
notify-send -u normal   'AMX quiet test' 'Ordinary message'
notify-send -u critical 'AMX quiet test' 'Critical message'
```

Check: no bell with empty unread; normal theme bell; critical theme accent; native popup opens; actions/dismissal work; closing popup marks only its snapshot read; a fresh message while open remains unread; persistence after a **restore-safe** shell return.

## Recovery

Use the Ambxst manager's built-in rollback if the candidate fails:

```bash
ambxst mods disable dev.hyperland.quiet-notifications
# If a newly activated generation fails:
ambxst mods rollback
```

These commands are documented, **not automatically executed**.

## Boundaries

- Source anchor: upstream Ambxst `1e1a56493582f34e5e5593bc9d3c2f87246266fb` (v1.3.x).
- No `qmlformat`, `qmllint`, or live Quickshell acceptance is possible inside the artifact-building container.
- Ambxst's existing history retention policy still applies; unread status does not override the policy.
- Behavior depends on the native notification-server urgency format and the existing `NotificationDelegate` QML API. Test `urgency=critical` on the actual build.
- Another mod may also change `Notifications.silent` and should be checked in the final composition.
