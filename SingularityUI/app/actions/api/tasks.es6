import { buildApiAction } from './base';

export const FetchForDeployAction = buildApiAction('FETCH_FOR_DEPLOY', (requestId, deployId) => {return {url: `/history/request/${requestId}/deploy/${deployId}/tasks/active`}});

export const FetchAction = buildApiAction('FETCH_TASKS', (state) => {
  const stateToFetch = state !== 'decommissioning' ? state : 'active';

  let propertyString = '?property=';
  switch(stateToFetch) {
    case 'active':
      propertyString += ['offer.hostname', 'taskId', 'mesosTask.resources', 'rackId', 'taskRequest.request.requestType'].join('&property=');
      break;
    case 'scheduled':
      propertyString += ['offer.hostname', 'taskId', 'mesosTask.resources', 'rackId', 'taskRequest.request.requestType', 'pendingTask'].join('&property=');
      break;
    default:
      propertyString = '';
  }

  return {url: `tasks/${stateToFetch}${propertyString}`};
});
