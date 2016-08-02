import { buildApiAction } from './base';

export const FetchTaskS3Logs = buildApiAction(
  'FETCH_TASK_S3_LOGS',
  (taskId) => ({
    url: `/logs/task/${taskId}`
  })
);
