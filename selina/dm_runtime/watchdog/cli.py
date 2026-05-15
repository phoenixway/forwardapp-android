from __future__ import annotations

import argparse
import sys

from dm_runtime.config.settings import SettingsStore
from dm_runtime.notifications import Notifier
from dm_runtime.watchdog.daemon import WatchdogRunner


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="DM Runtime watchdog")
    parser.add_argument("--once", action="store_true", help="Run one check and exit")
    parser.add_argument("--notify-test", action="store_true", help="Send a test notification and exit")
    args = parser.parse_args(argv)

    settings = SettingsStore().load()
    if args.notify_test:
        result = Notifier(settings.notification_backend).notify(
            "DM notification test",
            "If you see this, the notification backend works.",
            urgency="normal",
            tag="test",
        )
        print(f"backend={result.backend} delivered={result.delivered} {result.message}")
        return

    runner = WatchdogRunner(settings=settings)
    if args.once:
        triggers = runner.check_once()
        print(f"checked: {len(triggers)} trigger(s)")
        for trigger in triggers:
            print(f"- {trigger.id}: {trigger.title}")
        return
    try:
        runner.run_forever()
    except KeyboardInterrupt:
        print("DM watchdog stopped.")
        sys.exit(130)


if __name__ == "__main__":
    main()
