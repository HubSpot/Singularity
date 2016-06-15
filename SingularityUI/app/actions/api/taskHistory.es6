import buildApiAction from './base';

export const FetchForDeploy = buildApiAction('FETCH_TASK_HISTORY_FOR_DEPLOY', (requestId, deployId, count, page) =>
  `/history/request/${requestId}/tasks?requestId=${requestId}&deployId=${deployId}&count=${count}&page=${page}`);
