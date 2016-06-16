import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_TASK_FILES', (taskId, path) => `/sandbox/${taskId}/browse?path=${path || ''}`);
