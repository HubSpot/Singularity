import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_REQUEST', (requestId) => ({url: `/requests/request/${ requestId }`}));
export const SaveAction = buildJsonApiAction('SAVE_REQUEST', 'POST', (requestData) => ({url: '/requests', body: requestData}));
