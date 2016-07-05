import { buildApiAction } from './base';

export const FetchTaskHistory = buildApiAction(
  'FETCH_TASK_HISTORY',
  (taskId) => ({
    url: `/history/task/${taskId}`
  }),
  (taskId) => taskId
);

export const FetchActiveTasksForRequest = buildApiAction(
  'FETCH_ACTIVE_TASKS_FOR_REQUEST',
  (requestId) => ({
    url: `/history/request/${requestId}/tasks/active`
  }),
  (requestId) => requestId
);

export const FetchActiveTasksForDeploy = buildApiAction(
  'FETCH_ACTIVE_TASKS_FOR_DEPLOY',
  (requestId, deployId) => ({
    url: `/history/request/${requestId}/deploy/${deployId}/tasks/active`
  })
);

export const FetchTaskHistoryForDeploy = buildApiAction(
  'FETCH_TASK_HISTORY_FOR_DEPLOY',
  (requestId, deployId, count, page) => ({
    url: `/history/request/${requestId}/tasks?requestId=${requestId}&deployId=${deployId}&count=${count}&page=${page}`
  })
);

export const FetchDeployForRequest = buildApiAction(
  'FETCH_DEPLOY',
  (requestId, deployId) => ({
    url: `/history/request/${requestId}/deploy/${deployId}`
  })
);

export const FetchRequestRunHistory = buildApiAction(
  'FETCH_REQUEST_RUN_HISTORY',
  (requestId, runId) => ({
    url: `/history/request/${ requestId }/run/${runId}`
  })
);
