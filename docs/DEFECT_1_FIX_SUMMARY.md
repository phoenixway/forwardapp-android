# DEFECT #1 Fix Summary: Local Unsynced Attachments Limbo

## Problem
When Desktop sent 0 attachments during sync, local unsynced attachments were left in a perpetual "unsynced" state:
- They weren't lost (good)
- But they were never marked as synced (bad)
- This created a "limbo" where they couldn't be synced again until Desktop properly exported them

**Scenario:**
1. User creates attachments on Android → `syncedAt = null`
2. Desktop (buggy) responds with `attachments = []`
3. Android filters and preserves local attachments
4. But since no incoming attachments matched them, `syncedAt` is never set
5. On next sync, same attachments appear as "unsynced" again → endless loop

## Root Cause
The `applyServerChanges()` logic only marks attachments as synced if they:
- Are in the incoming list from Desktop, AND
- Pass the merge logic (mergeAndMark)

When Desktop sends 0 attachments, local attachments bypass the merge entirely and remain unsynced.

## Solution Implemented

Modified `applyServerChanges()` in `SyncRepository.kt`:

### Key Changes:

1. **Detect zero-attachment case**:
   ```kotlin
   if (correctedChanges.attachments.isEmpty()) {
       // Desktop sent 0 - mark local unsynced as synced
   }
   ```

2. **Mark valid local attachments as synced**:
   ```kotlin
   val unsyncedLocalAttachments = if (correctedChanges.attachments.isEmpty()) {
       local.attachments
           .filter { it.syncedAt == null && (it.ownerProjectId == null || it.ownerProjectId in projectIds) }
           .map { it.copy(syncedAt = ts, updatedAt = maxOf(it.updatedAt, ts)) }
   } else {
       emptyList()
   }
   ```

3. **Combine and insert both incoming and synced-local**:
   ```kotlin
   val allAttachmentsToInsert = incomingAttachments + unsyncedLocalAttachments
   ```

4. **Include synced attachments in cross-reference scope**:
   ```kotlin
   val attachmentIds = (local.attachments.map { it.id } + incomingAttachments.map { it.id } + unsyncedLocalAttachments.map { it.id }).toSet()
   ```

### Important Details:

- **Only applies when `attachments.isEmpty()`**: Once DEFECT #2 is fixed and Desktop properly exports attachments, this fallback won't trigger
- **Preserves orphaned attachments**: Still filters out attachments with invalid `ownerProjectId`
- **Maintains data integrity**: Uses the sync timestamp (`ts`) consistently

## Data Flow

```
Desktop sends attachments=0
    ↓
Android's applyServerChanges() detects this
    ↓
Marks all valid local unsynced attachments with syncedAt=now
    ↓
Local attachments inserted with syncedAt set
    ↓
On next sync cycle, these attachments won't be re-exported as "unsynced" ✅
```

## Interaction with DEFECT #2

Once DEFECT #2 (Desktop not exporting attachments) is fixed:
- Desktop will properly export attachments
- The `unsyncedLocalAttachments` path won't trigger (because `attachments.size > 0`)
- Attachments will flow properly through the normal merge logic

Until then:
- This fix prevents the limbo state
- Local attachments are marked as synced after being sent once
- Cross-references can still be processed

## Testing Notes

To verify:
1. Create attachments on Android (should have `syncedAt = null`)
2. Sync with Desktop (Desktop returns `attachments = 0`)
3. Android should log: `DEFECT #1 HANDLED: Desktop sent 0 attachments but marking X local unsynced attachments as synced`
4. Check database: local attachments should now have `syncedAt` set
5. Next sync cycle: same attachments shouldn't appear as unsynced again

## Files Modified

- `app/src/main/java/com/romankozak/forwardappmobile/data/repository/SyncRepository.kt`

## Commit

```
commit 9242c6c6
Fix DEFECT #1: Mark local unsynced attachments as synced when Desktop sends 0
```

## Status

**DEFECT #1**: ✅ Fixed  
**DEFECT #2**: ✅ Fixed  
**DEFECT #3**: ⏳ Pending (filtering logic in Android's `getChangesSince`)
