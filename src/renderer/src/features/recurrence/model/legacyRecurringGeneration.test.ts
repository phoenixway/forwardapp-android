import { describe, expect, it } from 'vitest';
import {
  createTask,
  ensureDayBoard,
  newBackup,
  type AndroidRecurringTask
} from '../../../sync';

function dailySeries(id: string, startDate: number): AndroidRecurringTask {
  return {
    id,
    title: 'Recurring task',
    description: null,
    goalId: null,
    linkedProjectIds: [],
    linkedAttachmentIds: [],
    duration: null,
    priority: 'MEDIUM',
    points: 0,
    recurrenceRule: {
      frequency: 'DAILY',
      interval: 1
    },
    startDate,
    endDate: null,
    createdAt: startDate,
    updatedAt: startDate,
    syncedAt: null,
    isDeleted: false,
    version: 1
  };
}

describe('legacy recurring generation', () => {
  it('does not regenerate a recurring occurrence when a tombstone already occupies that day', () => {
    const backup = newBackup();
    const date = new Date(2026, 7, 17, 12, 0, 0, 0);
    const initialBoard = ensureDayBoard(backup, date);
    const series = dailySeries('series-1', initialBoard.plan.date);

    backup.database.recurringTasks = [series];
    backup.database.dayTasks = [
      {
        ...createTask(initialBoard.plan.id, series.title, 0),
        id: `recurring-task-instance-${initialBoard.plan.id}-${series.id}`,
        recurringTaskId: series.id,
        isDeleted: true,
        syncedAt: null,
        version: 2
      }
    ];

    const board = ensureDayBoard(backup, date);
    const occurrenceRows = (backup.database.dayTasks ?? []).filter(
      (task) =>
        task.dayPlanId === initialBoard.plan.id &&
        task.recurringTaskId === series.id
    );

    expect(occurrenceRows).toHaveLength(1);
    expect(occurrenceRows[0].isDeleted).toBe(true);
    expect(board.tasks.filter((task) => task.recurringTaskId === series.id)).toHaveLength(0);
  });

  it('materializes one live occurrence when the recurring day has no occurrence row', () => {
    const backup = newBackup();
    const date = new Date(2026, 7, 17, 12, 0, 0, 0);
    const initialBoard = ensureDayBoard(backup, date);
    const series = dailySeries('series-1', initialBoard.plan.date);

    backup.database.recurringTasks = [series];

    const board = ensureDayBoard(backup, date);
    const occurrenceRows = (backup.database.dayTasks ?? []).filter(
      (task) =>
        task.dayPlanId === initialBoard.plan.id &&
        task.recurringTaskId === series.id
    );

    expect(occurrenceRows).toHaveLength(1);
    expect(occurrenceRows[0].isDeleted).toBe(false);
    expect(board.tasks.filter((task) => task.recurringTaskId === series.id)).toHaveLength(1);
  });
});
