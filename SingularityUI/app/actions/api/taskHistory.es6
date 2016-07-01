import { buildApiAction } from './base';

export const FetchForDeploy = buildApiAction('FETCH_TASK_HISTORY_FOR_DEPLOY', (requestId, deployId, count, page) => {
  return {url: `/history/request/${requestId}/tasks?requestId=${requestId}&deployId=${deployId}&count=${count}&page=${page}`}
});

export const FetchAction = buildApiAction('FETCH_TASK_HISTORY',
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
  }
});
