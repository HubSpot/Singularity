import { buildApiAction } from './base';

export const FetchGroups = buildApiAction(
  'FETCH_REQUEST_GROUPS',
  (catchStatusCodes = null) => ({
    url: '/groups?useWebCache=true',
    catchStatusCodes
  })
);
