import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_HISTORY',
  (taskId) => {return {url: `/history/task/${taskId}`}},
  (taskId) => taskId);

export const KillAction = buildJsonApiAction('KILL_TASK', 'DELETE', (taskId) => {return {url: `/history/task/${taskId}`}});
