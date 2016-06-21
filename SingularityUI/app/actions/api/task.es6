import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_HISTORY',
  (taskId) => {return {url: `/history/task/${taskId}`}},
  (taskId) => taskId);
