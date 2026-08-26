## Files

Status: HISTORICAL

This document preserves an earlier migration campaign for moving synchronized
Room entities toward Snapshot DTO transport.

The campaign is no longer an active implementation plan. Current production
sync already uses SnapshotBundle-based flows, and several entity assumptions in
this document predate later canonicalization work, including recurrence-v2.

Retain the file as migration history only. Do not resume its incomplete stages
without re-establishing current requirements from production code and canonical
project state.

core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/SnapshotBundle.kt
sync/src/main/java/com/romankozak/forwardappmobile/sync/SyncMapper.kt
core-data-models/src/main/java/com/romankozak/forwardappmobile/core/data/models/sync/snapshot/SnapshotMapper.kt
app/src/main/java/com/romankozak/forwardappmobile/features/ai/chat/Mappers.kt

## Entities
​1. Домен "Ядро та Контексти" (Core & Structure)
​Це каркас вашої системи. Без них інші дані втратять прив'язку.
​Context (Project): Основна одиниця (Проекти/Сфери).
​Goal: Цілі, прив'язані до контекстів.
​BacklogItem: Елементи беклогу.
​BacklogOrder: Порядок сортування в списках.
​ContextArtifact: Важливі результати/висновки по проектах.
​ContextLog: Журнал подій всередині контексту.
​InboxRecord: Вхідні думки/записи, які ще не розібрані.
​2. Домен "Тактика та Місії" (Tactical)
​Ваш "оперативний" рівень.
​TacticalMission: Конкретні місії з дедлайнами.
​TacticalMissionAttachmentCrossRef: Зв'язки місій із чек-лістами чи документами.
​3. Домен "Вкладення та Нотатки" (Knowledge Base)
​Ваша база знань.
​NoteDocument: Сучасні документи (Markdown/Rich Text).
​Checklist: Списки справ.
​ChecklistItem: Пункти всередині списків.
​LegacyNote: Старі текстові нотатки (для сумісності).
​Script: Ваші кастомні скрипти автоматизації.
​Attachment: Метадані файлів.
​ContextAttachmentCrossRef: Зв'язок вкладень із проектами.
​4. Домен "AI та Інсайти" (Artificial Intelligence)
​Все, що стосується вашої роботи з Angelica-AI.
​Conversation: Сесії діалогів.
​ChatMessage: Кожне повідомлення в чаті.
​ConversationFolder: Папки для організації чатів.
​AiInsight: Поради та висновки, згенеровані ШІ.
​AiEvent: Технічні події ШІ.
​5. Домен "Управління часом та Метрики" (Day Management & RPG)
​Ігрові механіки та планування.
​DayPlan: План на конкретний день.
​DayTask: Завдання на день.
​DailyMetric: Ваші показники (енергія, стрес, XP за день).
​ActivityRecord: Хронологія активності (саме тут живуть XP та Anti-XP).
​RecurringTask: Завдання, що повторюються (рутини).
​Reminder: Сповіщення.
​6. Домен "Система та Налаштування" (System & Presets)
​Як додаток адаптується під вас.
​ContextRoleProfile: Пресети інтерфейсу (Ролі).
​ContextRoleProfileItem: Елементи всередині пресетів.
​ContextConfiguration: Налаштування конкретного проекту.
​ContextStructureItem: Структурні налаштування.
​LifeSystemState: Стан вашої "системи життя" (ентропія, навантаження).
​SystemApp: Зв'язки з зовнішніми додатками.
​LinkItem / RelatedLink: Зовнішні посилання.
​RecentItem: Останні переглянуті проекти.

## Stages

### 1.Stage 1: Provide snapshots data classes based on enteties data model.
Done.
### 2. Stage: provide mappers