import { buildApiAction } from './base';

export const FetchTaskFiles = buildApiAction(
  'FETCH_TASK_FILES',
  (taskId, path = '', successResponseCodes = null) => ({
    url: `/sandbox/${taskId}/browse?path=${path}`,
    successResponseCodes
  }),
  (taskId, path = '') => `${taskId}/${path}`
);
