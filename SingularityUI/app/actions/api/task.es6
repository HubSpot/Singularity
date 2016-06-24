import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_HISTORY',
  (taskId) => ({url: `/history/task/${taskId}`}),
  (taskId) => taskId);

export const KillAction = buildJsonApiAction('KILL_TASK', 'DELETE', (taskId, data) => ({url: `/tasks/task/${taskId}`, body: data}));
