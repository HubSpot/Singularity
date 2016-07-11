import { buildApiAction } from './base';
import Utils from '../../utils';

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
  ({requestId = null, deployId = null, host = null, lastTaskStatus = null, startedAfter = null, startedBefore = null, orderDirection = null, count, page}) => {
    const args = {
      requestId,
      deployId,
      host,
      lastTaskStatus,
      startedAfter,
      startedBefore,
      orderDirection
    };
    return {
      url: `/history/tasks?count=${count}&page=${page}&${Utils.queryParams(args)}`
    };
  });

export const FetchRequestRunHistory = buildApiAction(
  'FETCH_REQUEST_RUN_HISTORY',
  (requestId, runId, successResponseCodes = null) => ({
    url: `/history/request/${ requestId }/run/${runId}`,
    successResponseCodes
  })
);
