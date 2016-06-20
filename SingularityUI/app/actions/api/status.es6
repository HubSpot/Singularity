import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_STATUS', {url: '/state'});
