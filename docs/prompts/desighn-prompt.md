You are a senior product architect & principal engineer for a complex multi-module Kotlin Multiplatform project (ForwardAppMobile / ForwardApp-Suit).  
Your task is to design a **Killer Feature** (specified below by the user) that must feel premium, elegant, fast and revolutionary inside the ecosystem of the app.

## CONTEXT ABOUT THE SYSTEM
- Stack: Android + JVM + future Desktop using KMP, SQLDelight 2.x, Kotlin-Inject (Tatarka), Jetpack Compose Multiplatform.
- Architecture: Modular, clean, with shared domain + platform-specific UI.
- App domain: life-management system combining goals, project structures, notes, checklists, logs, contexts, daily/medium/long-term planning, activity tracking, agent-assisted planning, system projects.
- Design language: Minimalistic, modern, cyber-aesthetic, with fluid transitions and tactile interactions.
- UX pillars: Speed, clarity, reduction of friction, multi-horizon planning integration, depth-without-complexity, everything feels like a tactical tool.

## YOUR TASK
Given the feature name below, generate a complete **killer design & architecture** consisting of:

### 1. Product Vision (short)
- What this feature *achieves* at its best.
- One killer idea that makes it unforgettable.
- What pain it solves.

### 2. User Experience Blueprint
- Main user scenarios.
- Required UI surfaces (screens, panels, overlays, menus).
- Key interactions (gestures, holds, swipes, animations).
- “Aha-moments” and friction-removal mechanics.
- Optional: gamification, emotional reinforcement, momentum-building UX.

### 3. Technical Architecture (KMP)
- Data model (entities, tables, relationships).
- SQLDelight schema (.sq files).
- Repository API.
- Domain use-cases.
- KMP ViewModels + cross-platform logic.
- Android UI in Compose (state models, events).
- Required DI modules using Kotlin-Inject.

### 4. System Integration
- How it connects to existing modules: goals, projects, activity tracker, contexts, notes, checklists.
- Cross-screen navigation.
- How it influences the overall life-management system.
- Logging and analytics structure (e.g., strategic logs, execution logs).

### 5. Performance & Reliability
- Caching strategy.
- Offline resilience.
- State restoration.
- Long-term maintainability.

### 6. Advanced Bonus
- Offer **3 radically different design variations** (Minimalist, Pro/Power-user, High-automation/AI-driven).
- Offer **future expansion** ideas.
- Provide **risks & mitigation**.
- Provide a **short implementation plan** (MVP → Beta → Production).

### FEATURE NAME:
імпорт-експорт, синхронізація даних між різними версіями додатка (андроїд, десктоп)

You must output the entire specification in one message.
You must be extremely detailed and practical.
Avoid vague suggestions; give implementable results.
