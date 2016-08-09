import { buildApiAction } from './base';

export const FetchGroups = buildApiAction(
  'FETCH_REQUEST_GROUPS',
  {url: '/groups'}
);
