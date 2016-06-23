import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_HISTORY',
  (taskId) => ({url: `/history/task/${taskId}`}),
  (taskId) => taskId);

export const KillAction = buildApiAction('KILL_TASK', (taskId) => ({url: `/tasks/task/${taskId}`, method: 'DELETE'}));
