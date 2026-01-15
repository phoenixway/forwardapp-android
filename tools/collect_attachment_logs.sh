#!/bin/bash

# Скрипт для збору логів синхронізації вкладень
# Використання: ./collect_attachment_logs.sh <output_file>

OUTPUT_FILE="${1:-/tmp/attachment_sync_$(date +%Y%m%d_%H%M%S).log}"

echo "========================================"
echo "Attachment Sync Debug Log Collector"
echo "========================================"
echo "Output file: $OUTPUT_FILE"
echo ""

# Перевіримо, чи підключений пристрій
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No Android device connected!"
    exit 1
fi

echo "[1/5] Clearing logcat buffer..."
adb logcat -c

echo "[2/5] Starting log capture (30 seconds)..."
echo ">>> PERFORM YOUR SYNC OPERATIONS NOW <<<" 
echo ""

# Запустимо logcat в фоні
adb logcat -v threadtime > "$OUTPUT_FILE" &
LOGCAT_PID=$!

# Чекаємо 30 секунд
sleep 30

# Зупинимо logcat
kill $LOGCAT_PID 2>/dev/null
wait $LOGCAT_PID 2>/dev/null

echo "[3/5] Extracting attachment sync logs..."
grep -E "FWD_ATTACH|FWD_SYNC_TEST" "$OUTPUT_FILE" > "${OUTPUT_FILE%.log}_filtered.log" 2>/dev/null || echo "No FWD logs found"

echo "[4/5] Extracting database state..."
{
    echo "=== ATTACHMENT TABLE ==="
    adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
        "SELECT id, attachment_type, entity_id, owner_project_id, synced_at, version FROM attachment ORDER BY created_at DESC LIMIT 20;" 2>/dev/null || echo "Cannot query attachment table"
    
    echo ""
    echo "=== ATTACHMENT COUNTS ==="
    adb shell sqlite3 /data/data/com.romankozak.forwardappmobile/databases/forward_app.db \
        "SELECT 'Total attachments' as type, COUNT(*) as count FROM attachment UNION ALL 
         SELECT 'Unsynced', COUNT(*) FROM attachment WHERE synced_at IS NULL UNION ALL 
         SELECT 'Synced', COUNT(*) FROM attachment WHERE synced_at IS NOT NULL UNION ALL 
         SELECT 'Total cross-refs', COUNT(*) FROM project_attachment_cross_ref UNION ALL 
         SELECT 'Unsynced cross-refs', COUNT(*) FROM project_attachment_cross_ref WHERE synced_at IS NULL;" 2>/dev/null || echo "Cannot query counts"
} >> "${OUTPUT_FILE%.log}_db.log"

echo "[5/5] Generating summary..."
{
    echo "========================================"
    echo "ATTACHMENT SYNC DEBUG LOG SUMMARY"
    echo "========================================"
    echo "Timestamp: $(date)"
    echo ""
    
    echo "=== LOG FILES GENERATED ==="
    echo "Full log:       $OUTPUT_FILE"
    echo "Filtered log:   ${OUTPUT_FILE%.log}_filtered.log"
    echo "Database state: ${OUTPUT_FILE%.log}_db.log"
    echo ""
    
    echo "=== KEY METRICS ==="
    if [ -f "$OUTPUT_FILE" ]; then
        echo "Total lines: $(wc -l < "$OUTPUT_FILE")"
        echo "FWD_ATTACH logs: $(grep -c "FWD_ATTACH" "$OUTPUT_FILE" || echo 0)"
        echo "FWD_SYNC_TEST logs: $(grep -c "FWD_SYNC_TEST" "$OUTPUT_FILE" || echo 0)"
    fi
    echo ""
    
    echo "=== NEXT STEPS ==="
    echo "1. Review full log:       cat $OUTPUT_FILE"
    echo "2. Review filtered log:   cat ${OUTPUT_FILE%.log}_filtered.log"
    echo "3. Review DB state:       cat ${OUTPUT_FILE%.log}_db.log"
    echo ""
    echo "=== THINGS TO LOOK FOR ==="
    echo "✓ [createLinkAttachment] STEP1, STEP2, STEP3 - attachment creation"
    echo "✓ [getChangesSince] Attachments: - shows how many are being exported"
    echo "✓ [applyServerChanges] Processing attachments - shows incoming count"
    echo "✓ [EXPORT-UNSYNC] - unsynced attachments being sent"
    echo "✓ [MARK-SYNCED] - attachments marked as synced"
    echo ""
    echo "=== ERROR PATTERNS TO SEARCH ==="
    grep -i "DEFECT\|ERROR\|EXCEPTION" "$OUTPUT_FILE" | head -20 || echo "No obvious errors found"
    
} | tee "${OUTPUT_FILE%.log}_summary.txt"

echo ""
echo "========================================"
echo "Log collection complete!"
echo "Summary file: ${OUTPUT_FILE%.log}_summary.txt"
echo "========================================"
