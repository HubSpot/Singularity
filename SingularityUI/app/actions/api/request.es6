import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_REQUEST', (requestId) => {return {url: `/requests/request/${ requestId }`}});
export const SaveAcrion = buildJsonApiAction('SAVE_REQUEST', 'POST', (requestData) => {return {body: requestData}});
