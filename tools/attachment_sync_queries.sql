-- Attachment Sync Debug Queries
-- Run these via: adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db < attachment_sync_queries.sql

-- ==========================================
-- 1. OVERVIEW: Attachment Counts
-- ==========================================
.echo on
.print "=== ATTACHMENT OVERVIEW ==="
SELECT 'Total attachments' as metric, COUNT(*) as value FROM attachment
UNION ALL
SELECT 'Unsynced attachments', COUNT(*) FROM attachment WHERE synced_at IS NULL
UNION ALL
SELECT 'Synced attachments', COUNT(*) FROM attachment WHERE synced_at IS NOT NULL
UNION ALL
SELECT 'Deleted attachments', COUNT(*) FROM attachment WHERE is_deleted = 1
UNION ALL
SELECT 'Total cross-refs', COUNT(*) FROM project_attachment_cross_ref
UNION ALL
SELECT 'Unsynced cross-refs', COUNT(*) FROM project_attachment_cross_ref WHERE synced_at IS NULL
UNION ALL
SELECT 'Deleted cross-refs', COUNT(*) FROM project_attachment_cross_ref WHERE is_deleted = 1;

-- ==========================================
-- 2. UNSYNCED ATTACHMENTS (Should be exported)
-- ==========================================
.print ""
.print "=== UNSYNCED ATTACHMENTS (To be exported) ==="
SELECT 
    id,
    attachment_type,
    entity_id,
    owner_project_id,
    version,
    created_at,
    updated_at,
    synced_at,
    is_deleted
FROM attachment
WHERE synced_at IS NULL
ORDER BY created_at DESC
LIMIT 20;

-- ==========================================
-- 3. ATTACHMENT DETAILS BY STATUS
-- ==========================================
.print ""
.print "=== ATTACHMENTS BY SYNC STATUS ==="
SELECT
    CASE WHEN synced_at IS NULL THEN 'UNSYNCED' ELSE 'SYNCED' END as status,
    attachment_type,
    COUNT(*) as count,
    MAX(created_at) as newest
FROM attachment
GROUP BY status, attachment_type
ORDER BY status DESC, attachment_type;

-- ==========================================
-- 4. CROSS-REFERENCES FOR UNSYNCED ATTACHMENTS
-- ==========================================
.print ""
.print "=== CROSS-REFS FOR UNSYNCED ATTACHMENTS ==="
SELECT 
    pac.project_id,
    pac.attachment_id,
    pac.attachment_order,
    pac.version,
    pac.synced_at,
    a.attachment_type,
    a.entity_id
FROM project_attachment_cross_ref pac
LEFT JOIN attachment a ON pac.attachment_id = a.id
WHERE pac.synced_at IS NULL
ORDER BY pac.created_at DESC
LIMIT 20;

-- ==========================================
-- 5. ORPHAN ATTACHMENTS (No cross-ref)
-- ==========================================
.print ""
.print "=== ORPHAN ATTACHMENTS (No project links) ==="
SELECT 
    a.id,
    a.attachment_type,
    a.entity_id,
    a.owner_project_id,
    COUNT(pac.project_id) as project_links,
    a.synced_at
FROM attachment a
LEFT JOIN project_attachment_cross_ref pac ON a.id = pac.attachment_id AND pac.is_deleted = 0
GROUP BY a.id
HAVING COUNT(pac.project_id) = 0
ORDER BY a.created_at DESC;

-- ==========================================
-- 6. LINK ITEMS (Referenced by attachments)
-- ==========================================
.print ""
.print "=== LINK ITEMS (From unsynced attachments) ==="
SELECT 
    li.id,
    li.link_data,
    li.synced_at,
    COUNT(a.id) as attachment_count
FROM link_item li
LEFT JOIN attachment a ON a.entity_id = li.id AND a.attachment_type = 'LINK_ITEM' AND a.is_deleted = 0
GROUP BY li.id
HAVING li.synced_at IS NULL OR COUNT(a.id) > 0
ORDER BY li.created_at DESC
LIMIT 20;

-- ==========================================
-- 7. VERSION CONFLICTS
-- ==========================================
.print ""
.print "=== POTENTIAL VERSION CONFLICTS ==="
SELECT 
    a.id,
    a.attachment_type,
    a.version,
    a.synced_at,
    pac.version as xref_version,
    pac.synced_at as xref_synced_at
FROM attachment a
LEFT JOIN project_attachment_cross_ref pac ON a.id = pac.attachment_id AND pac.is_deleted = 0
WHERE a.version > 1 OR pac.version > 1
ORDER BY a.updated_at DESC
LIMIT 10;

-- ==========================================
-- 8. SYNC TIMELINE
-- ==========================================
.print ""
.print "=== SYNC TIMELINE (Last 10 changes) ==="
SELECT 
    'attachment' as entity_type,
    id,
    updated_at,
    synced_at,
    CASE WHEN synced_at IS NULL THEN 'UNSYNCED' WHEN is_deleted = 1 THEN 'DELETED' ELSE 'SYNCED' END as status,
    version
FROM attachment
WHERE synced_at IS NOT NULL OR synced_at IS NULL
ORDER BY updated_at DESC
LIMIT 10;

-- ==========================================
-- 9. ATTACHMENT SIZE ANALYSIS
-- ==========================================
.print ""
.print "=== ATTACHMENT DATA SIZE ==="
SELECT 
    COUNT(*) as attachment_count,
    ROUND(SUM(LENGTH(id)) / 1024.0, 2) as ids_kb,
    ROUND(SUM(LENGTH(entity_id)) / 1024.0, 2) as entity_ids_kb,
    ROUND(SUM(LENGTH(owner_project_id)) / 1024.0, 2) as owner_ids_kb
FROM attachment;

-- ==========================================
-- 10. RESET SYNC FLAGS (Use carefully!)
-- ==========================================
.print ""
.print "=== RESET SYNC FLAGS (Commented out) ==="
.print "-- To reset all synced timestamps (forcing re-export):"
.print "-- UPDATE attachment SET synced_at = NULL;"
.print "-- UPDATE project_attachment_cross_ref SET synced_at = NULL;"
.print ""
.print "-- To verify synced_at was cleared:"
.print "-- SELECT COUNT(*) FROM attachment WHERE synced_at IS NOT NULL;"
