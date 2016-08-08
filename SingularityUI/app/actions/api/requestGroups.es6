import { buildApiAction } from './base';

export const FetchGroups = buildApiAction(
  'FETCH_GROUPS',
  {url: '/groups'}
);
