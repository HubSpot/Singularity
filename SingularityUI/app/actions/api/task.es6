import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_HISTORY',
  (taskId) => {return {url: `/history/task/${taskId}`}},
  (taskId) => taskId);

export const KillAction = buildApiAction('KILL_TASK', (taskId) => {return {url: `/tasks/task/${taskId}`, method: 'DELETE'}});
