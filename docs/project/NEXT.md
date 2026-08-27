# Next

Investigate Desktop/Android automatic sync triggering.

The Inbox cross-client policy itself is live-validated. Keep the next
investigation at the transport/trigger layer unless repository evidence points
back to data semantics.

Reproduce the earlier case where Android changes became visible on Desktop only
after a manual Pull, then trace:

- what schedules or requests automatic Pull/Push;
- what state/change signal is expected to wake it;
- whether the request runs but is skipped, fails, or never starts;
- whether successful automatic sync persists and refreshes the same data path
  as manual sync.

Prefer fixing the missing trigger or lifecycle ownership rather than adding
Inbox-specific polling or refresh behavior.
