# Attachment Sync Defect Analysis

## Your Specific Situation

### Symptoms you reported:
1. **Android library**: 106 attachments → 12 after sync (94 lost = 88.7% loss rate)
2. **New note on Android**: Created locally, doesn't sync to desktop, **disappears from Android**
3. **New note on desktop**: Exists locally on desktop, doesn't appear on Android
4. **Desktop export**: Never exports attachments at all

---

## Root Cause Analysis

### The 106 → 12 scenario

**Most likely scenario:**

You're hitting a combination of all 3 defects:

```
STEP 1: Initial state
├─ Android: 106 attachments (many synced, some unsynced)
└─ Desktop: ??? attachments (unknown state)

STEP 2: Desktop to Android sync (pull)
├─ Desktop exports: 0 attachments (DEFECT #2 - desktop doesn't export)
├─ Android receives: 0 attachments incoming
├─ Android keeps: All 106 local attachments (unchanged)
└─ Android syncs: Nothing (correct behavior given incoming=0)

STEP 3: Android to Desktop sync (push)
├─ Android collects: ~12 attachments from getChangesSince() (DEFECT #3)
│  └─ Reason: getChangesSince() filters by various criteria:
│     • ownerProjectId validation
│     • Project existence validation
│     • deltaSince timestamp filtering
│     └─ Result: Only ~12 pass all filters
├─ Android exports: 12 attachments
└─ Android marks: Those 12 as synced (syncedAt = now)

STEP 4: Result
├─ Android now shows: 12 attachments (were marked synced, not exported others)
└─ Desktop shows: 0 attachments (doesn't import)

THE PROBLEM:
The other 94 attachments (106-12=94) either:
a) Got filtered out (invalid ownerProjectId) - LIKELY
b) Got filtered out (old timestamp) - POSSIBLE  
c) Are marked as synced but shouldn't be - POSSIBLE
```

---

### The "Note disappears" scenario

**What happens:**

```
STEP 1: Create note attachment on Android
├─ User adds link to note in project "ABC"
├─ New AttachmentEntity created: syncedAt=null, version=1
├─ ProjectAttachmentCrossRef created: syncedAt=null, version=1
└─ Logged: [createLinkAttachment] DONE: attachment is NEW and unsync'd

STEP 2: Attempt to sync to desktop
├─ Android collects unsynced changes with getChangesSince()
├─ NOTE: This is NOT triggered automatically!
│  └─ User must tap "Sync" button or auto-sync runs
└─ IF sync is triggered:
   ├─ getChangesSince() finds this new attachment (syncedAt=null)
   ├─ Attachment gets exported to desktop
   └─ Android marks it syncedAt = now

STEP 3: Desktop doesn't import
├─ Desktop Wi-Fi server receives export
├─ BUT: Desktop never processes attachments (DEFECT #2)
└─ Attachment is lost on desktop

STEP 4: Next sync cycle (desktop to Android)
├─ Desktop exports: 0 attachments (DEFECT #2)
├─ Android receives: 0 incoming
├─ BUT: Android already has this attachment locally AND marked it synced
├─ New attachment is neither:
│  • Exported again (was already synced)
│  • Visible on desktop (never imported)
└─ USER PERCEPTION: "Note disappeared"
```

---

## Which Defect is YOUR main problem?

### Most Likely: **DEFECT #2 + Defect #3**

**Evidence:**
- "Desktop doesn't export attachments at all" = DEFECT #2
- "106 → 12 after sync" = DEFECT #3 (filtering loses 88% of attachments)
- "New note... disappears" = DEFECT #2 (desktop never imports) + DEFECT #1 (gets lost)

### Secondary: **DEFECT #1**

When you push attachments from Android and desktop responds with 0 attachments, Android keeps local ones. But they're already marked as synced, so they're not re-exported.

---

## Data Flow to Verify

### Question 1: How many attachments does Android ACTUALLY export?

```kotlin
// In getChangesSince(), count the result
val attachmentsUnsync = local.attachments.filter { it.syncedAt == null }
val attachmentsUpdated = local.attachments.filter { (it.updatedTs() ?: 0L) > since && it.syncedAt != null }
val attachmentsResult = attachmentsUnsync + attachmentsUpdated

// HOW MANY PASS THE FILTER?
val filtered = correctedChanges.attachments.filter { it.ownerProjectId == null || it.ownerProjectId in projectIds }

// If attachmentsResult=106 but filtered=12, then filtering is the problem
```

**Log to check:** `[getChangesSince] Attachments: total=106, unsync'd=X, updated=Y, result=Z`

If `Z < 106`, some attachments are not being exported.

### Question 2: Does Android receive any attachments from desktop?

```
Look for: [applyServerChanges] Processing attachments. Total incoming: ???
```

If this is always 0, desktop is not exporting at all.

### Question 3: Which attachments get filtered out?

Check the detailed DEFECT #3 logs:
```
[EXPORT-UNSYNC] Attachment: id=..., type=..., owner=...
```

Compare with:
```
adb shell sqlite3 /data/data/.../forward_app.db "SELECT COUNT(*) FROM attachment"
```

If counts don't match, they were filtered.

---

## Immediate Actions to Take

### 1. Enable DEBUG BUILD & Collect Logs

```bash
# Build with our new logging
./gradlew assembleExpDebug

# Install on device
adb install -r app/build/outputs/apk/expDebug/app-expDebug.apk

# Clear app data (start fresh)
adb shell pm clear com.romankozak.forwardappmobile

# Start log collection script
tools/collect_attachment_logs.sh /tmp/debug1.log
```

### 2. Test Scenario: Single Attachment

```
1. Open app
2. Create new note in project (e.g., "Test123")
3. Add single web link
4. **Record the attachment ID from logcat**
5. Sync (push to desktop)
6. Check if appears on desktop
7. Sync back (pull from desktop)
8. Check Android logs
```

### 3. Analyze the Logs

Use the filtering commands from `ATTACHMENT_SYNC_DEBUG_GUIDE.md`:

```bash
grep "FWD_ATTACH" /tmp/debug1.log  # Creation logs
grep "DEFECT" /tmp/debug1.log       # Problem detection logs
grep "EXPORT" /tmp/debug1.log       # Export logs
grep "applyServerChanges" /tmp/debug1.log  # Import logs
```

### 4. Check Database State

```bash
# After the test scenario
sqlite3 forward_app.db < tools/attachment_sync_queries.sql > /tmp/db_state.txt

# Key metric: Count of unsynced vs synced
SELECT COUNT(*) FROM attachment WHERE synced_at IS NULL;
SELECT COUNT(*) FROM attachment WHERE synced_at IS NOT NULL;
```

---

## Expected Log Patterns

### Correct Flow (What we want):

```
[createLinkAttachment] START: project=proj-XYZ, link=https://example.com
[createLinkAttachment] STEP1: LinkItemEntity created: id=link-ABC, syncedAt=null (NEW)
[createLinkAttachment] STEP2: AttachmentEntity created: id=att-DEF, syncedAt=null (NEW)
[createLinkAttachment] STEP3: ProjectAttachmentCrossRef created: syncedAt=null (NEW)

[getChangesSince] Attachments: total=107, unsync'd=1, updated=0, result=1
[getChangesSince] DEFECT #3 INFO: Found 1 unsynced attachments that WILL be exported
  [EXPORT-UNSYNC] Attachment: id=att-DEF, type=LINK_ITEM, entity=link-ABC, version=1

[createDeltaBackupJsonString] attachments=1, crossRefs=1

[applyServerChanges] Processing attachments. Total incoming: 1
[applyServerChanges] Attachment to insert: id=att-DEF, syncedAt=TIMESTAMP, version=1

[markSyncedNow] Marking 1 attachments synced
  [MARK-SYNCED] Attachment: id=att-DEF, syncedAt=TIMESTAMP
```

### Problematic Flow (What we're seeing):

```
[getChangesSince] Attachments: total=106, unsync'd=5, updated=0, result=5
  NOTICE: 101 synced attachments NOT exported (will never sync to desktop)

[applyServerChanges] Processing attachments. Total incoming: 0
  WARNING: Desktop sent 0 but Android has 5 unsynced. These will NOT be lost...
  (But they also won't sync!)
```

---

## Fix Priority

### Priority 1: DEFECT #2 (Desktop doesn't export)
**Impact:** 🔴 CRITICAL - Breaks bi-directional sync
**Evidence:** "Desktop doesn't export attachments at all"
**Action:** Check desktop code for attachment export logic

### Priority 2: DEFECT #3 (Filtering loses 88% of attachments)
**Impact:** 🔴 CRITICAL - Causes "106 → 12" loss
**Possible causes:**
- Orphaned attachments (ownerProjectId not in current projects)
- Old attachments with deleted projects
- Cross-ref filtering logic
**Action:** Use SQL queries to identify which attachments are being filtered and why

### Priority 3: DEFECT #1 (Empty desktop export)
**Impact:** 🟡 MEDIUM - Causes confusion but doesn't lose data
**Action:** After fixing #2 & #3, re-test this scenario

---

## Testing Matrix

After fixes, test all scenarios:

```
              | Android→Desktop | Desktop→Android |
              |   (Push)        |    (Pull)       |
──────────────┼─────────────────┼─────────────────┤
New Attach    |     ✓ WORKS     |     ✓ WORKS     |
Modify Attach |     ✓ WORKS     |     ✓ WORKS     |
Delete Attach |     ✓ WORKS     |     ✓ WORKS     |
Orphan Attach |     ✓ PRESERVED |     ✓ IMPORTED  |
──────────────┴─────────────────┴─────────────────┘
```

Each scenario should maintain attachment counts and not lose data.

---

## Log Inspection Checklist

Use this when analyzing your logs:

- [ ] `[createLinkAttachment]` shows 3 steps?
- [ ] `[getChangesSince]` shows unsynced > 0?
- [ ] `[EXPORT-UNSYNC]` logs show your attachment?
- [ ] `[applyServerChanges]` shows incoming > 0?
- [ ] `[MARK-SYNCED]` marks the attachment?
- [ ] Database query shows syncedAt was updated?
- [ ] No `DEFECT` warnings in logs?
- [ ] No `ERROR` or `Exception` in logs?

If any answer is "No", that's where the problem is.
