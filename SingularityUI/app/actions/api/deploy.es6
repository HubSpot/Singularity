import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_DEPLOY', (requestId, deployId) => ({url: `history/request/${requestId}/deploy/${deployId}`}));
