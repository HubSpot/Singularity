import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_DEPLOY', (requestId, deployId) => ({url: `history/request/${requestId}/deploy/${deployId}`}));

export const SaveAction = buildJsonApiAction('SAVE_DEPLOY', 'POST', (deployData) => ({url: 'deploys', body: deployData}));
