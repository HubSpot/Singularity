import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_FILES', (taskId, path) => {return {url: `/sandbox/${taskId}/browse?path=${path || ''}`}});
