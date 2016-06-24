import { buildApiAction } from './base';

export const FetchAction = buildApiAction('FETCH_REQUESTS', {url: '/requests'});
