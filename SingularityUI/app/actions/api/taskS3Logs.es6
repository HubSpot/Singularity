import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_TASK_S3_LOGS', (taskId) => `/logs/task/${taskId}`);
