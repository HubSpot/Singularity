import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_FILES',
  (taskId, path='') => ({url: `/sandbox/${taskId}/browse?path=${path}`}),
  (taskId, path='') => `${taskId}/${path}`);
