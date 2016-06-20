import buildApiAction from './base';

export const FetchForDeployAction = buildApiAction('FETCH_FOR_DEPLOY', (requestId, deployId) => {return {url: `/history/request/${requestId}/deploy/${deployId}/tasks/active`}});
