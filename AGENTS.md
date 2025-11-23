# ForwardApp DevTools – Agent System Manifest

Цей файл описує архітектуру всіх агентів, які беруть участь у твоєму
робочому процесі:

- coding agent (Gemini / Codex)
- voice agent (voice.py)
- orchestrator (work.sh)
- template engine
- context memory system (Context.md / Masterplan.md / Progress.md)
- dictionary expansions

---

# 1. Структура forwardapp-devtools

forwardapp-devtools/
context/
Context.md
Context-example.md
Masterplan.md
Progress.md
dictionary/
default.env
templates/
start.md
step.md
fix.md
tools/
work.sh
voice.py


---

# 2. Ролі агентів

## Coding Agent (Gemini / Codex)
- отримує промпти із work.sh (через tmux)
- читає CONTEXT.md та MASTERPLAN.md
- виконує рівно одну дію за раз
- пропонує оновлення CONTEXT.md
- формує внески в Progress.md
- пише код, пояснює помилки, виправляє їх

Детальна логіка — у `GEMINI.md`.

---

## Orchestrator (work.sh)
- читає шаблони з `templates/*`
- читає словники з `dictionary/*.env`
- підставляє {{context}}, {{plan}}, {{progress}}
- підтримує багаторядкові аргументи
- формує готовий промпт для агента
- відправляє його в tmux-сесію `agent`

Команди:
- `work start`
- `work step`
- `work fix`
- `work build`
- і будь-які інші шаблони

---

## Context Memory System
Оперативна пам'ять агента — це:

- **Context.md** — основний робочий стан (CURRENT TASK, PLAN, PROBLEMS…)
- **Masterplan.md** — стратегічні задачі
- **Progress.md** — історія виконання

Ці файли НЕ редагуються агентом напряму.  
Всі зміни проходять через:
- `=== PROPOSED CONTEXT UPDATE ===`
- і потім застосовуються work.sh або вручну.

---

## Voice Agent (voice.py)
Функції:
- Hotword: “Форвард, слухай”
- перетворення голос → команд
- підтримка команд:
  - почати роботу  
  - наступний крок  
  - онови контекст  
  - виправ помилку  
  - зачитай контекст  
  - додай до плану / проблем / ходу роботи  
  - запам’ятай наступний текст як інструкцію

Використовує Whisper.cpp + простий NLP router.

---

# 3. Взаємодія агентів

voice → voice.py → work.sh → tmux → coding agent → результат → Context.md


Шаблони визначають формат промптів.  
Словники визначають розширення даних.  
Context визначає поточну оперативну пам’ять.

---

# 4. Правила взаємодії всередині системи

- coding agent НІКОЛИ сам не змінює файли  
- coding agent завжди пропонує зміни блоками
- context-файли — єдине джерело правди
- всі агенти працюють українською
- кожна дія → один крок
- coding agent після дії пропонує `make debug-cycle`
- voice agent може додавати записи в Context.md напряму

---

# 5. Git Guidelines

Після значимих змін:
- `git add ...`
- `git cz`

Контекстні файли краще зберігати в окремому приватному репозиторії.

---

Цей файл описує всю агентну екосистему ForwardApp.

