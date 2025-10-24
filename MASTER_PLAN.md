# Master Plan

## Backlog

- [x] Дослідити, чому `GoalItem` та `StatusIconsRow` не рендеряться, незважаючи на правильні дані та `shouldShowStatusIcons = true`. Перевірити вищі рівні ієрархії компонентів.

Проблема: Іконка '??' (знак питання) не відображається на компоненті цілі, незважаючи на те, що `ContextUtils` правильно ідентифікує її, а `GoalItemViewModel` встановлює `shouldShowStatusIcons` на `true`. Логи показують, що `GoalItem` та `StatusIconsRow` не виконуються, навіть після спрощення `AnimatedVisibility` та `InteractiveListItem`. Це вказує на проблему з рекомпозицією або рендерингом компонентів вищого рівня.