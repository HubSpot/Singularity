import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_TASK_RESOURCE_USAGE', (taskId) => ({url: `/tasks/task/${taskId}/statistics`}));
