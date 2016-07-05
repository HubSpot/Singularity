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

export const FetchTaskSearchParams = buildApiAction(
  'FETCH_TASK_HISTORY',
  ({requestId, deployId = null, host = null, lastTaskStatus = null, startedAfter = null, startedBefore = null, orderDirection = null, count, page}) => {
    let params = [];
    if (deployId) params.push(`&deployId=${deployId}`);
    if (host) params.push(`&host=${host}`);
    if (lastTaskStatus) params.push(`&lastTaskStatus=${lastTaskStatus}`);
    if (startedAfter) params.push(`&startedAfter=${startedAfter}`);
    if (startedBefore) params.push(`&startedBefore=${startedBefore}`);
    if (orderDirection) params.push(`&orderDirection=${orderDirection}`);
    return {
      url: `/history/request/${requestId}/tasks?count=${count}&page=${page}${params.join('')}`
    };
});
