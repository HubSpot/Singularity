import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_REQUEST', (requestId) => {return {url: `/requests/request/${ requestId }`}});
export const SaveAction = buildJsonApiAction('SAVE_REQUEST', 'POST', (requestData) => {return {url: '/requests', body: requestData}});
