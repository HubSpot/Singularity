import { buildApiAction } from './base';

export const FetchTaskFiles = buildApiAction(
  'FETCH_TASK_FILES',
  (taskId, path = '', catchStatusCodes = null) => ({
    url: `/sandbox/${taskId}/browse?path=${path}`,
    catchStatusCodes
  }),
  (taskId, path = '') => `${taskId}/${path}`
);
