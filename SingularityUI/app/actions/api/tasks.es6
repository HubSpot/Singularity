import { buildApiAction, buildJsonApiAction } from './base';

export const FetchTask = buildApiAction(
  'FETCH_TASK',
  (taskId) => ({
    url: `/tasks/task/${taskId}`,
  })
);

export const KillTask = buildJsonApiAction(
  'KILL_TASK',
  'DELETE',
  (taskId, data) => ({
    url: `/tasks/task/${taskId}`,
    body: data
  })
);

export const FetchTaskCleanups = buildApiAction(
  'FETCH_TASK_CLEANUPS',
  {url: '/tasks/cleaning'}
);

export const FetchTaskStatistics = buildApiAction(
  'FETCH_TASK_STATISTICS',
  (taskId) => ({
    url: `/tasks/task/${taskId}/statistics`
  })
);

export const RunCommandOnTask = buildJsonApiAction(
  'RUN_COMMAND_ON_TASK',
  'POST',
  (taskId, commandName) => ({
    url: `/tasks/task/${taskId}/command`,
    body: {name: commandName}
  })
);
