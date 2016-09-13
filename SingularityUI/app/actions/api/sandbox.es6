import { buildApiAction } from './base';

export const FetchTaskFiles = buildApiAction(
  'FETCH_TASK_FILES',
  (taskId, path = '', catchStatusCodes = null) => {
    let url;
    if (path) {
      url = `/sandbox/${taskId}/browse?path=${path}`
    } else {
      url = `/sandbox/${taskId}/browse`
    }
    return {
      url: url,
      catchStatusCodes
    }
  },
  (taskId, path = '') => `${taskId}/${path}`
);
