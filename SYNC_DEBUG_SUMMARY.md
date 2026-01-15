# Attachment Sync Debug - Summary

## Problem Statement

Your attachment synchronization system has a critical issue:
- **106 attachments** on Android → **12 attachments** after sync (**88.7% loss**)
- **New attachments on Android** are created but disappear after sync
- **New notes on Desktop** don't appear on Android
- **Desktop doesn't export attachments at all**

---

## What Was Done

### 1. Code Analysis ✅
Identified 3 critical defects in the sync flow:

| Defect | Location | Impact |
|--------|----------|--------|
| **#1** | `applyServerChanges()` | When desktop sends 0 attachments, local unsynced ones get stuck |
| **#2** | Desktop code (unknown) | Desktop never exports attachments |
| **#3** | `getChangesSince()` + validation | Attachments with invalid `ownerProjectId` are exported but rejected |

### 2. Debug Logging Added ✅

**File**: `app/src/main/java/com/romankozak/forwardappmobile/data/repository/SyncRepository.kt`

```kotlin
// EXPORT PHASE
[getChangesSince] Attachments: total=X, unsync'd=Y, updated=Z, result=N
[getChangesSince] DEFECT #3 INFO: Found Y unsynced attachments that WILL be exported
[getChangesSince] WARNING: X attachments have invalid ownerProjectId

// IMPORT PHASE
[applyServerChanges] Processing attachments. Total incoming: N
[applyServerChanges] Local attachments BEFORE: total=X, synced=Y, unsynced=Z
[applyServerChanges] DEFECT #1 DETECTED: Desktop sent 0 but Android has X unsynced

// MARK SYNCED
[markSyncedNow] Marking N attachments synced
  [MARK-SYNCED] Attachment: id=..., syncedAt=TIMESTAMP
```

**File**: `app/src/main/java/com/romankozak/forwardappmobile/features/attachments/data/AttachmentRepository.kt`

```kotlin
// CREATION
[createLinkAttachment] STEP1: LinkItemEntity created (syncedAt=null)
[createLinkAttachment] STEP2: AttachmentEntity created (syncedAt=null)
[createLinkAttachment] STEP3: ProjectAttachmentCrossRef created (syncedAt=null)
[createLinkAttachment] DONE: attachment is NEW and unsync'd, will be exported on next sync
```

### 3. Debug Tools Created ✅

**`tools/collect_attachment_logs.sh`**
- Automatically collects all sync-related logs
- Filters by `FWD_ATTACH` and `FWD_SYNC_TEST` tags
- Dumps database state
- Generates comprehensive summary

**`tools/attachment_sync_queries.sql`**
- 10 SQL queries to inspect attachment state
- Check unsynced vs synced counts
- Find orphan attachments
- Analyze cross-refs

### 4. Documentation Created ✅

**`ATTACHMENT_SYNC_DEBUG_GUIDE.md`** (842 lines)
- Complete explanation of 3 defects
- Full sync flow diagram
- How to read logs
- Step-by-step debug plan
- Control points for testing

**`ATTACHMENT_DEFECT_ANALYSIS.md`** (350 lines)
- Your specific situation analyzed
- Root cause for 106→12 scenario
- Expected vs problematic log patterns
- Testing matrix

**`SYNC_DEBUG_SUMMARY.md`** (this file)
- Executive summary
- What to do next

---

## Next Steps

### Phase 1: Gather Evidence (1-2 hours)

1. **Build and install debug version**
   ```bash
   ./gradlew assembleExpDebug
   adb install -r app/build/outputs/apk/expDebug/app-expDebug.apk
   ```

2. **Clear app data (fresh start)**
   ```bash
   adb shell pm clear com.romankozak.forwardappmobile
   ```

3. **Run log collection script**
   ```bash
   tools/collect_attachment_logs.sh /tmp/debug_run1.log
   ```

4. **During script run, perform test scenario:**
   ```
   - Create 1-3 simple notes with links
   - Perform Wi-Fi sync (push to desktop)
   - Perform sync back (pull from desktop)
   - Check both devices
   ```

5. **Analyze the logs**
   ```bash
   # Check for specific patterns
   grep "FWD_ATTACH" /tmp/debug_run1_filtered.log
   grep "DEFECT" /tmp/debug_run1_filtered.log
   grep "WARNING" /tmp/debug_run1_filtered.log
   ```

### Phase 2: Identify Root Cause (30 mins)

Look for these patterns:

**If you see:**
```
[getChangesSince] WARNING: 94 attachments have invalid ownerProjectId
```
→ **Problem**: Orphaned attachments (deleted projects)
→ **Solution**: Preserve as orphans or clean up

**If you see:**
```
[applyServerChanges] DEFECT #1 DETECTED: Desktop sent 0 attachments
```
→ **Problem**: Desktop not exporting
→ **Solution**: Check desktop code (DEFECT #2)

**If you see:**
```
[getChangesSince] unsync'd=5 result=5 (then [createDeltaBackupJsonString] attachments=12)
```
→ **Problem**: Filtering between export and actual JSON output
→ **Solution**: Find the filtering logic

### Phase 3: Fix the Root Cause (2-4 hours)

Once you identify which defect is primary:

**If DEFECT #1:**
→ Make unsynced attachments survive empty import

**If DEFECT #2:**
→ Implement attachment export on desktop

**If DEFECT #3:**
→ Fix ownerProjectId validation or preserve orphans

### Phase 4: Verify Fix (1-2 hours)

Test all scenarios:
```
Android→Desktop: ✓ Single note with link
Desktop→Android: ✓ Import the same link
Orphans:         ✓ Link without project still syncs
Modifications:   ✓ Changing link text syncs
Deletions:       ✓ Deleting link soft-deletes
```

---

## Files Modified

### Code Changes
- `SyncRepository.kt`: Added detailed logging for export/import/mark phases
- `AttachmentRepository.kt`: Added logging for creation flow
- `SyncRepository.kt`: Added validation for invalid ownerProjectId

### New Documentation
- `ATTACHMENT_SYNC_DEBUG_GUIDE.md`: 842 lines
- `ATTACHMENT_DEFECT_ANALYSIS.md`: 350 lines
- `SYNC_DEBUG_SUMMARY.md`: This file

### New Tools
- `tools/collect_attachment_logs.sh`: Log collection script
- `tools/attachment_sync_queries.sql`: Database inspection queries

---

## Key Metrics to Track

Every time you sync, look for these numbers:

```
[getChangesSince] Attachments: total=X, unsync'd=Y, updated=Z, result=N
                               ↑        ↑         ↑         ↑
                          All local    New     Modified   To export

[applyServerChanges] Processing attachments. Total incoming: M
                                                             ↑
                                            From desktop/server

[markSyncedNow] Marking K attachments synced
                         ↑
                    Successfully synced
```

**Healthy sync:**
- X = consistent (same total)
- Y + Z = N (all exported items are new or modified)
- M = N (all exported items received)
- K = M (all received items marked synced)

**Problematic sync:**
- Y + Z ≠ N (some items filtered out)
- M = 0 and Y > 0 (exported but not received)
- K = 0 and M > 0 (received but not marked)

---

## Common Issues & Solutions

| Symptom | Root Cause | Solution |
|---------|-----------|----------|
| Attachments always 0 exported | Desktop sync disabled | Enable on desktop |
| Y > 0 but M = 0 | Desktop not importing | Check desktop import logic |
| High invalid ownerProjectId | Projects deleted | Preserve as orphans |
| Stuck at unsynced=N | Last sync failed | Clear syncedAt and retry |

---

## Commands for Quick Testing

```bash
# View unsynced attachments
adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
  "SELECT COUNT(*) as unsynced FROM attachment WHERE synced_at IS NULL;"

# View attachment breakdown
adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db < tools/attachment_sync_queries.sql

# Collect logs with auto-summary
./tools/collect_attachment_logs.sh /tmp/test.log

# View only relevant logs
adb logcat | grep -E "FWD_ATTACH|FWD_SYNC_TEST|DEFECT"

# Reset sync state (for testing)
adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
  "UPDATE attachment SET synced_at = NULL; UPDATE project_attachment_cross_ref SET synced_at = NULL;"
```

---

## Expected Timeline

- **Gathering evidence**: 1-2 hours
- **Analysis**: 30 minutes
- **Fix (simple)**: 1 hour
- **Fix (complex)**: 2-4 hours
- **Verification**: 1-2 hours
- **Total**: 4-9 hours

---

## Success Criteria

After fixes, your sync should:

1. ✅ Export all 106 attachments (not just 12)
2. ✅ Desktop receives all 106
3. ✅ New attachments sync in both directions
4. ✅ Counts remain consistent: 106 → 106 → 106
5. ✅ No WARNING or ERROR logs
6. ✅ All DEFECT warnings resolved

---

## Questions to Answer

As you debug, answer these:

1. Are the 94 "lost" attachments actually deleted or just unsynced?
   ```
   Hint: Check DB for is_deleted flag and synced_at values
   ```

2. Do they have valid ownerProjectId?
   ```
   Hint: Run attachment_sync_queries.sql #3 and #4
   ```

3. Are they being exported from Android?
   ```
   Hint: Look for [EXPORT-UNSYNC] logs for all 106
   ```

4. Are they being received on desktop?
   ```
   Hint: Check desktop logs for [applyServerChanges] Total incoming: 106
   ```

5. Which exact attachments are missing?
   ```
   Hint: Export before sync, after sync, compare IDs
   ```

---

## Resources

- **Code files**: `SyncRepository.kt`, `AttachmentRepository.kt`, `WifiSyncServer.kt`
- **Debug tools**: `tools/collect_attachment_logs.sh`, `tools/attachment_sync_queries.sql`
- **Documentation**: `ATTACHMENT_SYNC_DEBUG_GUIDE.md`, `ATTACHMENT_DEFECT_ANALYSIS.md`
- **Logs**: `$LOG_TAG = "FWD_SYNC_TEST"` (sync), `"FWD_ATTACH"` (creation)

---

## Notes

- All logging is non-blocking (only `Log.d()` and `Log.w()`)
- No behavior changes, only diagnostics
- Safe to leave logging in production
- You'll need both Android and desktop logs to fully diagnose
- Consider version control for test data before/after sync

---

**Status**: Ready for debugging  
**Last updated**: Dec 3, 2024  
**Next review**: After gathering evidence
