import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_DEPLOY', (requestId, deployId) => {return {url: `history/request/${requestId}/deploy/${deployId}`}});
