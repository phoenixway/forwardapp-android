import os
import re

# Налаштування шляхів
BASE_PATH = "app/src/main/java/com/romankozak/forwardappmobile/core/data/models"
SNAPSHOT_BASE = "app/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/entities"

# Домени та їх цільові файли
DOMAINS = {
    "ai": "ai/AiSnapshots.kt",
    "day_management": "metrics/MetricSnapshots.kt",
    "tactical": "tactical/TacticalSnapshots.kt",
    "context": "context/CoreSnapshots.kt",
    "attachments": "attachments/AttachmentSnapshots.kt",
    "misc": "misc/MiscSnapshots.kt",
    "reminders": "reminders/ReminderSnapshots.kt"
}

def get_snapshot_path(entity_name, domain):
    filename = DOMAINS.get(domain, "misc/MiscSnapshots.kt")
    full_path = os.path.join(SNAPSHOT_BASE, filename)
    return full_path

def check_exists(snapshot_path, entity_name):
    if not os.path.exists(snapshot_path):
        return False
    with open(snapshot_path, 'r') as f:
        return f"{entity_name}Snapshot" in f.read()

def generate_snapshot_content(entity_name):
    # Базова логіка генерації: ми робимо Snapshot з тими ж полями, але спрощеними
    return f"data class {entity_name}Snapshot(\n    val id: String,\n    val updatedAt: Long,\n    val version: Int,\n    val isDeleted: Boolean\n    // TODO: Додати специфічні поля сутності\n)"

# Список сутностей для перевірки (базуючись на DatabaseContent)
entities = [
    ("Context", "context"), ("Goal", "context"), ("BacklogItem", "context"),
    ("TacticalMission", "tactical"), ("NoteDocument", "attachments"),
    ("Checklist", "attachments"), ("ChecklistItem", "attachments"),
    ("ActivityRecord", "day_management"), ("DailyMetric", "day_management"),
    ("ChatMessage", "ai"), ("Conversation", "ai"), ("AiInsight", "ai"),
    ("ContextRoleProfile", "context"), ("Reminder", "reminders")
]

print(f"{'Entity':<20} | {'Model File':<40} | {'Snapshot Status/Path'}")
print("-" * 100)

for entity, domain in entities:
    # Пошук файлу моделі (спрощено)
    model_file = "Unknown (Search manually)"
    for root, dirs, files in os.walk(BASE_PATH):
        for f in files:
            if entity in f:
                model_file = os.path.relpath(os.path.join(root, f))
                break
    
    snap_path = get_snapshot_path(entity, domain)
    exists = check_exists(snap_path, entity)
    
    status = f"EXISTS: {snap_path}" if exists else f"NEW FILE: {snap_path}"
    
    print(f"{entity:<20} | {model_file:<40} | {status}")
    
    if not exists:
        print(f"   >>> SUGGESTED CONTENT FOR {entity}Snapshot:")
        print(f"   {generate_snapshot_content(entity)}\n")
