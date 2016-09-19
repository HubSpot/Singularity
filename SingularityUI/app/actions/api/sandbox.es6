import { buildApiAction } from './base';

export const FetchTaskFiles = buildApiAction(
  'FETCH_TASK_FILES',
  (taskId, path = undefined, catchStatusCodes = null) => {
    let url;
    if (!_.isUndefined(path)) {
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
