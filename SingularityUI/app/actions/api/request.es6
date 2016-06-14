import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_REQUEST', (requestId) => `/requests/request/${ requestId }`);
