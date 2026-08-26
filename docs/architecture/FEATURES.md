# Application Features

This document provides an overview of the main features and functionality of the ForwardApp mobile application.

## Core Features

### 1. Project and Goal Management

-   **Description:** Create, organize, and track personal and professional projects and goals. Projects can be hierarchical, with sub-projects and tasks.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/`
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/goals/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/projects/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/goals/`

### 2. Task and Checklist Management

-   **Description:** Create to-do lists, checklists, and tasks. Tasks can be assigned to projects, have due dates, and reminders.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/daymanagement/`
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/attachments/types/checklists/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/tasks/`

### 3. Note-Taking and Document Editing

-   **Description:** A powerful note-taking feature that supports rich text editing, embedding of images, and organization of notes into notebooks or linking to projects.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/notes/`
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/attachments/types/notedocuments/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/notes/`
    -   `app/src/main/assets/editor.html`

### 4. Time Tracking

-   **Description:** Track time spent on various activities and projects. This helps in understanding productivity and billing for freelance work.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/activitytracker/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/activitytracker/`

### 5. Reminders and Notifications

-   **Description:** Set reminders for tasks, events, and goals. The app sends notifications to ensure you stay on track.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/reminders/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/broadcastreceivers/ReminderBroadcastReceiver.kt`

### 6. Inbox for Quick Capture

-   **Description:** A dedicated inbox to quickly capture thoughts, ideas, and tasks. These can be later organized and assigned to projects.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/projects/views/inbox/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/inbox/`

### 7. AI-Powered Insights

-   **Description:** The app uses AI to provide insights into your productivity, suggest next actions, and help you achieve your goals more effectively.
-   **Packages & Files:**
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/aichat/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/insights/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/services/GenerationService.kt`

## Cross-Cutting Concerns

### - **Data Sync**

    -   **Description:** Syncs data across devices.
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/sync/SyncDataViewModel.kt`

### - **Search**

    -   **Description:** A global search functionality to find any item within the app.
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/search/`

### - **Settings**

    -   **Description:** A comprehensive settings screen to configure the app to your needs.
    -   `packages/shared/src/commonMain/kotlin/com/romankozak/forwardappmobile/shared/features/settings/`
    -   `app/src/main/java/com/romankozak/forwardappmobile/ui/settings/`
