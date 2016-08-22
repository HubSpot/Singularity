import { buildApiAction } from './base';

export const FetchTaskS3Logs = buildApiAction(
  'FETCH_TASK_S3_LOGS',
  (taskId, catchStatusCodes) => ({
    url: `/logs/task/${taskId}`,
    catchStatusCodes
  })
);
